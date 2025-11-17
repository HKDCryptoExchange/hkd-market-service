package com.hkd.market.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hkd.market.domain.entity.Kline;
import com.hkd.market.domain.repository.KlineRepository;
import com.hkd.market.infrastructure.persistence.mapper.KlineMapper;
import com.hkd.market.infrastructure.persistence.po.KlinePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * K线Repository实现
 *
 * 职责：
 * 1. 实现domain层的KlineRepository接口
 * 2. 将domain实体转换为持久化对象
 * 3. 调用MyBatis Plus进行数据库操作
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KlineRepositoryImpl implements KlineRepository {

    private final KlineMapper klineMapper;

    @Override
    public Kline save(Kline kline) {
        KlinePO po = toKlinePO(kline);

        if (po.getId() == null) {
            // 新增
            klineMapper.insert(po);
            log.debug("Inserted new kline: id={}, symbol={}, interval={}, openTime={}",
                    po.getId(), po.getSymbol(), po.getInterval(), po.getOpenTime());
        } else {
            // 更新
            po.setUpdatedAt(LocalDateTime.now());
            klineMapper.updateById(po);
            log.debug("Updated kline: id={}, symbol={}, interval={}, openTime={}",
                    po.getId(), po.getSymbol(), po.getInterval(), po.getOpenTime());
        }

        return toKline(po);
    }

    @Override
    public Optional<Kline> findBySymbolAndIntervalAndOpenTime(String symbol, String interval, Long openTime) {
        LambdaQueryWrapper<KlinePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KlinePO::getSymbol, symbol)
                .eq(KlinePO::getInterval, interval)
                .eq(KlinePO::getOpenTime, openTime);

        KlinePO po = klineMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toKline);
    }

    @Override
    public List<Kline> findByTimeRange(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        List<KlinePO> poList = klineMapper.findByTimeRange(symbol, interval, startTime, endTime, limit);
        return poList.stream()
                .map(this::toKline)
                .collect(Collectors.toList());
    }

    @Override
    public List<Kline> findLatestKlines(String symbol, String interval, Integer limit) {
        List<KlinePO> poList = klineMapper.findLatestKlines(symbol, interval, limit);
        return poList.stream()
                .map(this::toKline)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Kline> findLatestKline(String symbol, String interval) {
        KlinePO po = klineMapper.findLatestKline(symbol, interval);
        return Optional.ofNullable(po).map(this::toKline);
    }

    @Override
    public Long countBySymbolAndInterval(String symbol, String interval) {
        return klineMapper.countBySymbolAndInterval(symbol, interval);
    }

    /**
     * 将domain实体转换为持久化对象
     *
     * @param kline domain实体
     * @return 持久化对象
     */
    private KlinePO toKlinePO(Kline kline) {
        if (kline == null) {
            return null;
        }

        return KlinePO.builder()
                .id(kline.getId())
                .symbol(kline.getSymbol())
                .interval(kline.getInterval())
                .openTime(kline.getOpenTime())
                .closeTime(kline.getCloseTime())
                .open(kline.getOpen())
                .high(kline.getHigh())
                .low(kline.getLow())
                .close(kline.getClose())
                .volume(kline.getVolume())
                .amount(kline.getAmount())
                .tradeCount(kline.getTradeCount())
                .completed(kline.getCompleted())
                .createdAt(kline.getCreatedAt())
                .updatedAt(kline.getUpdatedAt())
                .build();
    }

    /**
     * 将持久化对象转换为domain实体
     *
     * @param po 持久化对象
     * @return domain实体
     */
    private Kline toKline(KlinePO po) {
        if (po == null) {
            return null;
        }

        return Kline.builder()
                .id(po.getId())
                .symbol(po.getSymbol())
                .interval(po.getInterval())
                .openTime(po.getOpenTime())
                .closeTime(po.getCloseTime())
                .open(po.getOpen())
                .high(po.getHigh())
                .low(po.getLow())
                .close(po.getClose())
                .volume(po.getVolume())
                .amount(po.getAmount())
                .tradeCount(po.getTradeCount())
                .completed(po.getCompleted())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
