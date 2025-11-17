package com.hkd.market.application.service;

import com.hkd.market.api.enums.KlineInterval;
import com.hkd.market.domain.entity.Kline;
import com.hkd.market.domain.repository.KlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KlineAggregationService 单元测试
 *
 * 测试重点：
 * 1. 时间对齐算法（alignToInterval）- 所有9种周期
 * 2. OHLCV更新逻辑
 * 3. Redis缓存机制
 * 4. 数据库持久化
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("K线聚合服务测试")
class KlineAggregationServiceTest {

    @Mock
    private KlineRepository klineRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private KlineAggregationService klineAggregationService;

    @BeforeEach
    void setUp() {
        // Mock RedisTemplate.opsForValue()
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("测试1分钟K线时间对齐")
    void testAlignToInterval_1Minute() {
        // 2024-11-17 10:32:45 -> 2024-11-17 10:32:00
        Instant timestamp = Instant.parse("2024-11-17T10:32:45Z");
        long expectedOpenTime = Instant.parse("2024-11-17T10:32:00Z").getEpochSecond();

        // 准备Mock
        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行聚合
        klineAggregationService.aggregateTrade(
                "BTC-USDT",
                new BigDecimal("50000"),
                new BigDecimal("0.1"),
                timestamp
        );

        // 验证保存的K线的openTime
        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        // 找到1m周期的K线
        Optional<Kline> min1Kline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1m"))
                .findFirst();

        assertTrue(min1Kline.isPresent());
        assertEquals(expectedOpenTime, min1Kline.get().getOpenTime());
    }

    @Test
    @DisplayName("测试5分钟K线时间对齐")
    void testAlignToInterval_5Minutes() {
        // 2024-11-17 10:32:45 -> 2024-11-17 10:30:00
        Instant timestamp = Instant.parse("2024-11-17T10:32:45Z");
        long expectedOpenTime = Instant.parse("2024-11-17T10:30:00Z").getEpochSecond();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(
                "BTC-USDT",
                new BigDecimal("50000"),
                new BigDecimal("0.1"),
                timestamp
        );

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        Optional<Kline> min5Kline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("5m"))
                .findFirst();

        assertTrue(min5Kline.isPresent());
        assertEquals(expectedOpenTime, min5Kline.get().getOpenTime());
    }

    @Test
    @DisplayName("测试1小时K线时间对齐")
    void testAlignToInterval_1Hour() {
        // 2024-11-17 10:32:45 -> 2024-11-17 10:00:00
        Instant timestamp = Instant.parse("2024-11-17T10:32:45Z");
        long expectedOpenTime = Instant.parse("2024-11-17T10:00:00Z").getEpochSecond();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(
                "BTC-USDT",
                new BigDecimal("50000"),
                new BigDecimal("0.1"),
                timestamp
        );

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        Optional<Kline> hour1Kline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1h"))
                .findFirst();

        assertTrue(hour1Kline.isPresent());
        assertEquals(expectedOpenTime, hour1Kline.get().getOpenTime());
    }

    @Test
    @DisplayName("测试1天K线时间对齐")
    void testAlignToInterval_1Day() {
        // 2024-11-17 10:32:45 -> 2024-11-17 00:00:00
        Instant timestamp = Instant.parse("2024-11-17T10:32:45Z");
        long expectedOpenTime = Instant.parse("2024-11-17T00:00:00Z").getEpochSecond();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(
                "BTC-USDT",
                new BigDecimal("50000"),
                new BigDecimal("0.1"),
                timestamp
        );

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        Optional<Kline> day1Kline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1d"))
                .findFirst();

        assertTrue(day1Kline.isPresent());
        assertEquals(expectedOpenTime, day1Kline.get().getOpenTime());
    }

    @Test
    @DisplayName("测试新K线创建 - OHLCV初始值")
    void testCreateNewKline() {
        String symbol = "BTC-USDT";
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal quantity = new BigDecimal("0.1");
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(symbol, price, quantity, timestamp);

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        // 验证1m周期的K线OHLCV
        Optional<Kline> min1Kline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1m"))
                .findFirst();

        assertTrue(min1Kline.isPresent());
        Kline kline = min1Kline.get();
        assertEquals(price, kline.getOpen());
        assertEquals(price, kline.getHigh());
        assertEquals(price, kline.getLow());
        assertEquals(price, kline.getClose());
        assertEquals(quantity, kline.getVolume());
        assertEquals(price.multiply(quantity), kline.getAmount());
        assertEquals(1, kline.getTradeCount());
        assertFalse(kline.getCompleted());
    }

    @Test
    @DisplayName("测试OHLCV更新逻辑 - 更新最高价")
    void testUpdateKlineOHLCV_HighPrice() {
        String symbol = "BTC-USDT";
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");

        // 第一笔成交：创建K线
        Kline existingKline = Kline.builder()
                .id(1L)
                .symbol(symbol)
                .interval("1m")
                .openTime(timestamp.getEpochSecond())
                .closeTime(timestamp.getEpochSecond() + 60)
                .open(new BigDecimal("50000"))
                .high(new BigDecimal("50000"))
                .low(new BigDecimal("50000"))
                .close(new BigDecimal("50000"))
                .volume(new BigDecimal("0.1"))
                .amount(new BigDecimal("5000"))
                .tradeCount(1)
                .completed(false)
                .build();

        when(valueOperations.get(anyString())).thenReturn(existingKline);
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 第二笔成交：价格更高
        BigDecimal higherPrice = new BigDecimal("51000");
        BigDecimal quantity = new BigDecimal("0.2");

        klineAggregationService.aggregateTrade(symbol, higherPrice, quantity, timestamp.plusSeconds(30));

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        // 验证更新后的K线
        Optional<Kline> updatedKline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1m"))
                .findFirst();

        assertTrue(updatedKline.isPresent());
        assertEquals(new BigDecimal("50000"), updatedKline.get().getOpen()); // 开盘价不变
        assertEquals(higherPrice, updatedKline.get().getHigh()); // 最高价更新
        assertEquals(new BigDecimal("50000"), updatedKline.get().getLow()); // 最低价不变
        assertEquals(higherPrice, updatedKline.get().getClose()); // 收盘价更新为最新价
    }

    @Test
    @DisplayName("测试OHLCV更新逻辑 - 更新最低价")
    void testUpdateKlineOHLCV_LowPrice() {
        String symbol = "BTC-USDT";
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");

        Kline existingKline = Kline.builder()
                .id(1L)
                .symbol(symbol)
                .interval("1m")
                .openTime(timestamp.getEpochSecond())
                .closeTime(timestamp.getEpochSecond() + 60)
                .open(new BigDecimal("50000"))
                .high(new BigDecimal("50000"))
                .low(new BigDecimal("50000"))
                .close(new BigDecimal("50000"))
                .volume(new BigDecimal("0.1"))
                .amount(new BigDecimal("5000"))
                .tradeCount(1)
                .completed(false)
                .build();

        when(valueOperations.get(anyString())).thenReturn(existingKline);
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 价格更低
        BigDecimal lowerPrice = new BigDecimal("49000");
        BigDecimal quantity = new BigDecimal("0.2");

        klineAggregationService.aggregateTrade(symbol, lowerPrice, quantity, timestamp.plusSeconds(30));

        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, atLeastOnce()).save(klineCaptor.capture());

        Optional<Kline> updatedKline = klineCaptor.getAllValues().stream()
                .filter(k -> k.getInterval().equals("1m"))
                .findFirst();

        assertTrue(updatedKline.isPresent());
        assertEquals(new BigDecimal("50000"), updatedKline.get().getOpen()); // 开盘价不变
        assertEquals(new BigDecimal("50000"), updatedKline.get().getHigh()); // 最高价不变
        assertEquals(lowerPrice, updatedKline.get().getLow()); // 最低价更新
        assertEquals(lowerPrice, updatedKline.get().getClose()); // 收盘价更新为最新价
    }

    @Test
    @DisplayName("测试Redis缓存机制")
    void testRedisCaching() {
        String symbol = "BTC-USDT";
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");
        BigDecimal price = new BigDecimal("50000");
        BigDecimal quantity = new BigDecimal("0.1");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(symbol, price, quantity, timestamp);

        // 验证Redis缓存被调用 - 每个周期一次
        verify(valueOperations, times(9)).set(
                anyString(),
                any(Kline.class),
                anyLong(),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("测试数据库查询 - Redis未命中时")
    void testDatabaseFallback() {
        String symbol = "BTC-USDT";
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");
        BigDecimal price = new BigDecimal("50000");
        BigDecimal quantity = new BigDecimal("0.1");

        // Redis未命中
        when(valueOperations.get(anyString())).thenReturn(null);

        // 数据库有数据
        Kline existingKline = Kline.builder()
                .id(1L)
                .symbol(symbol)
                .interval("1m")
                .openTime(timestamp.getEpochSecond())
                .closeTime(timestamp.getEpochSecond() + 60)
                .open(price)
                .high(price)
                .low(price)
                .close(price)
                .volume(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .tradeCount(0)
                .completed(false)
                .build();

        when(klineRepository.findBySymbolAndIntervalAndOpenTime(eq(symbol), eq("1m"), anyLong()))
                .thenReturn(Optional.of(existingKline));
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(symbol, price, quantity, timestamp);

        // 验证数据库查询被调用
        verify(klineRepository, atLeastOnce()).findBySymbolAndIntervalAndOpenTime(eq(symbol), eq("1m"), anyLong());
    }

    @Test
    @DisplayName("测试聚合到所有9种周期")
    void testAggregateToAllIntervals() {
        String symbol = "BTC-USDT";
        Instant timestamp = Instant.parse("2024-11-17T10:30:00Z");
        BigDecimal price = new BigDecimal("50000");
        BigDecimal quantity = new BigDecimal("0.1");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(klineRepository.findBySymbolAndIntervalAndOpenTime(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.aggregateTrade(symbol, price, quantity, timestamp);

        // 验证保存了9条K线（每个周期一条）
        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository, times(9)).save(klineCaptor.capture());

        // 验证所有周期都被创建
        var intervals = klineCaptor.getAllValues().stream()
                .map(Kline::getInterval)
                .toList();

        assertEquals(9, intervals.size());
        assertTrue(intervals.contains("1m"));
        assertTrue(intervals.contains("5m"));
        assertTrue(intervals.contains("15m"));
        assertTrue(intervals.contains("30m"));
        assertTrue(intervals.contains("1h"));
        assertTrue(intervals.contains("4h"));
        assertTrue(intervals.contains("1d"));
        assertTrue(intervals.contains("1w"));
        assertTrue(intervals.contains("1M"));
    }

    @Test
    @DisplayName("测试标记K线为已完成")
    void testMarkKlineAsCompleted() {
        String symbol = "BTC-USDT";
        String interval = "1m";
        long openTime = Instant.parse("2024-11-17T10:30:00Z").getEpochSecond();

        Kline kline = Kline.builder()
                .id(1L)
                .symbol(symbol)
                .interval(interval)
                .openTime(openTime)
                .closeTime(openTime + 60)
                .open(new BigDecimal("50000"))
                .high(new BigDecimal("50000"))
                .low(new BigDecimal("50000"))
                .close(new BigDecimal("50000"))
                .volume(new BigDecimal("0.1"))
                .amount(new BigDecimal("5000"))
                .tradeCount(1)
                .completed(false)
                .build();

        when(klineRepository.findBySymbolAndIntervalAndOpenTime(symbol, interval, openTime))
                .thenReturn(Optional.of(kline));
        when(klineRepository.save(any(Kline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        klineAggregationService.markKlineAsCompleted(symbol, interval, openTime);

        // 验证K线被标记为已完成
        ArgumentCaptor<Kline> klineCaptor = ArgumentCaptor.forClass(Kline.class);
        verify(klineRepository).save(klineCaptor.capture());
        assertTrue(klineCaptor.getValue().getCompleted());

        // 验证Redis缓存被更新
        verify(valueOperations).set(
                anyString(),
                any(Kline.class),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }
}
