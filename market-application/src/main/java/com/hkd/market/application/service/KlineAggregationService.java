package com.hkd.market.application.service;

import com.hkd.market.api.enums.KlineInterval;
import com.hkd.market.domain.entity.Kline;
import com.hkd.market.domain.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * K线聚合服务
 *
 * 核心功能：
 * 1. 消费成交数据并聚合到各个周期的K线
 * 2. 实时更新K线OHLCV数据
 * 3. Redis缓存热数据
 * 4. PostgreSQL持久化
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KlineAggregationService {

    private final KlineRepository klineRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 聚合成交数据到各个周期的K线
     *
     * @param symbol 交易对 (例如: BTC/USDT)
     * @param price 成交价格
     * @param quantity 成交数量
     * @param tradeTime 成交时间
     */
    @Transactional
    public void aggregateTrade(String symbol, BigDecimal price, BigDecimal quantity, Instant tradeTime) {
        log.debug("Aggregating trade: symbol={}, price={}, quantity={}, time={}",
                symbol, price, quantity, tradeTime);

        // 聚合到所有9种周期
        for (KlineInterval interval : KlineInterval.values()) {
            try {
                aggregateToInterval(symbol, interval, price, quantity, tradeTime);
            } catch (Exception e) {
                log.error("Failed to aggregate trade to interval {}: symbol={}, price={}, quantity={}",
                        interval, symbol, price, quantity, e);
                // 不抛出异常，继续处理其他周期
            }
        }

        log.info("Trade aggregated successfully: symbol={}, price={}, quantity={}",
                symbol, price, quantity);
    }

    /**
     * 聚合到指定周期的K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param price 成交价格
     * @param quantity 成交数量
     * @param tradeTime 成交时间
     */
    private void aggregateToInterval(
            String symbol,
            KlineInterval interval,
            BigDecimal price,
            BigDecimal quantity,
            Instant tradeTime
    ) {
        // 1. 计算K线的开始时间（对齐到周期边界）
        long openTime = alignToInterval(tradeTime, interval);

        // 2. 尝试从Redis获取当前K线（热数据）
        String cacheKey = buildCacheKey(symbol, interval.getCode(), openTime);
        Kline currentKline = (Kline) redisTemplate.opsForValue().get(cacheKey);

        // 3. 如果Redis没有，从数据库查询
        if (currentKline == null) {
            Optional<Kline> klineOpt = klineRepository.findBySymbolAndIntervalAndOpenTime(
                    symbol, interval.getCode(), openTime
            );

            if (klineOpt.isPresent()) {
                currentKline = klineOpt.get();
                log.debug("Loaded kline from database: {}", currentKline);
            } else {
                // 4. 数据库也没有，创建新K线
                currentKline = createNewKline(symbol, interval, openTime, price, quantity, tradeTime);
                log.info("Created new kline: symbol={}, interval={}, openTime={}",
                        symbol, interval.getCode(), Instant.ofEpochSecond(openTime));
            }
        }

        // 5. 更新K线OHLCV数据
        updateKlineOHLCV(currentKline, price, quantity);

        // 6. 保存到数据库
        Kline savedKline = klineRepository.save(currentKline);

        // 7. 更新Redis缓存（TTL = 2 * 周期时间）
        long ttlSeconds = interval.getSeconds() * 2;
        redisTemplate.opsForValue().set(cacheKey, savedKline, ttlSeconds, TimeUnit.SECONDS);

        log.debug("Kline updated and cached: symbol={}, interval={}, openTime={}, close={}",
                symbol, interval.getCode(), Instant.ofEpochSecond(openTime), savedKline.getClose());
    }

    /**
     * 创建新K线
     *
     * 注意：新K线的OHLCV初始值都设置为零或初始价格
     * 随后会通过updateKlineOHLCV统一更新
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param openTime 开盘时间（时间戳秒）
     * @param price 第一笔成交价格
     * @param quantity 第一笔成交数量（未使用，保留参数兼容性）
     * @param tradeTime 成交时间（未使用，保留参数兼容性）
     * @return 新K线
     */
    private Kline createNewKline(
            String symbol,
            KlineInterval interval,
            long openTime,
            BigDecimal price,
            BigDecimal quantity,
            Instant tradeTime
    ) {
        long closeTime = openTime + interval.getSeconds();

        return Kline.builder()
                .symbol(symbol)
                .interval(interval.getCode())
                .openTime(openTime)
                .closeTime(closeTime)
                .open(price)          // 开盘价 = 第一笔成交价
                .high(price)          // 最高价 = 第一笔成交价（会被updateKlineOHLCV更新）
                .low(price)           // 最低价 = 第一笔成交价（会被updateKlineOHLCV更新）
                .close(price)         // 收盘价 = 第一笔成交价（会被updateKlineOHLCV更新）
                .volume(BigDecimal.ZERO)     // 成交量初始为0，由updateKlineOHLCV累加
                .amount(BigDecimal.ZERO)     // 成交额初始为0，由updateKlineOHLCV累加
                .tradeCount(0)               // 成交笔数初始为0，由updateKlineOHLCV累加
                .completed(false)            // 未完成
                .build();
    }

    /**
     * 更新K线OHLCV数据
     *
     * OHLCV = Open/High/Low/Close/Volume
     *
     * @param kline 当前K线
     * @param price 新成交价格
     * @param quantity 新成交数量
     */
    private void updateKlineOHLCV(Kline kline, BigDecimal price, BigDecimal quantity) {
        // 更新最高价
        if (price.compareTo(kline.getHigh()) > 0) {
            kline.setHigh(price);
        }

        // 更新最低价
        if (price.compareTo(kline.getLow()) < 0) {
            kline.setLow(price);
        }

        // 更新收盘价（最新价格）
        kline.setClose(price);

        // 累加成交量
        kline.setVolume(kline.getVolume().add(quantity));

        // 累加成交额
        BigDecimal tradeAmount = price.multiply(quantity);
        kline.setAmount(kline.getAmount().add(tradeAmount));

        // 累加成交笔数
        kline.setTradeCount(kline.getTradeCount() + 1);
    }

    /**
     * 对齐时间到K线周期边界
     *
     * 核心算法：将任意时间戳对齐到所属K线周期的起始时间
     *
     * 例如：
     * - 2024-11-17 10:32:45 对齐到 1分钟周期 = 2024-11-17 10:32:00
     * - 2024-11-17 10:32:45 对齐到 5分钟周期 = 2024-11-17 10:30:00
     * - 2024-11-17 10:32:45 对齐到 1小时周期 = 2024-11-17 10:00:00
     *
     * @param timestamp 原始时间
     * @param interval K线周期
     * @return 对齐后的时间戳（秒）
     */
    private long alignToInterval(Instant timestamp, KlineInterval interval) {
        long epochSecond = timestamp.getEpochSecond();

        switch (interval) {
            case MIN_1:
                // 1分钟：截断到分钟
                return epochSecond / 60 * 60;

            case MIN_5:
                // 5分钟：向下取整到5分钟边界
                return epochSecond / 300 * 300;

            case MIN_15:
                // 15分钟：向下取整到15分钟边界
                return epochSecond / 900 * 900;

            case MIN_30:
                // 30分钟：向下取整到30分钟边界
                return epochSecond / 1800 * 1800;

            case HOUR_1:
                // 1小时：截断到小时
                return epochSecond / 3600 * 3600;

            case HOUR_4:
                // 4小时：向下取整到4小时边界（从0点开始：0,4,8,12,16,20）
                return epochSecond / 14400 * 14400;

            case DAY_1:
                // 1天：截断到UTC 00:00:00
                return timestamp.truncatedTo(ChronoUnit.DAYS).getEpochSecond();

            case WEEK_1:
                // 1周：对齐到周一 00:00:00 UTC
                return timestamp.atZone(ZoneOffset.UTC)
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .truncatedTo(ChronoUnit.DAYS)
                        .toInstant()
                        .getEpochSecond();

            case MONTH_1:
                // 1月：对齐到月初 00:00:00 UTC
                return timestamp.atZone(ZoneOffset.UTC)
                        .with(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
                        .truncatedTo(ChronoUnit.DAYS)
                        .toInstant()
                        .getEpochSecond();

            default:
                log.warn("Unknown interval: {}, using 1 minute as default", interval);
                return epochSecond / 60 * 60;
        }
    }

    /**
     * 构建Redis缓存Key
     *
     * 格式: market:kline:{symbol}:{interval}:{openTime}
     * 例如: market:kline:BTC-USDT:1m:1700224800
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param openTime 开盘时间（时间戳秒）
     * @return Redis Key
     */
    private String buildCacheKey(String symbol, String interval, long openTime) {
        return String.format("market:kline:%s:%s:%d", symbol, interval, openTime);
    }

    /**
     * 标记K线为已完成
     *
     * 当K线周期结束后，标记为completed=true
     * 已完成的K线不再更新
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param openTime 开盘时间
     */
    public void markKlineAsCompleted(String symbol, String interval, long openTime) {
        Optional<Kline> klineOpt = klineRepository.findBySymbolAndIntervalAndOpenTime(
                symbol, interval, openTime
        );

        klineOpt.ifPresent(kline -> {
            kline.setCompleted(true);
            klineRepository.save(kline);

            // 更新Redis缓存
            String cacheKey = buildCacheKey(symbol, interval, openTime);
            redisTemplate.opsForValue().set(cacheKey, kline, 3600, TimeUnit.SECONDS);

            log.info("Kline marked as completed: symbol={}, interval={}, openTime={}",
                    symbol, interval, Instant.ofEpochSecond(openTime));
        });
    }
}
