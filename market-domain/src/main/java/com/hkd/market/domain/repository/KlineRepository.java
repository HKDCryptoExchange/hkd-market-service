package com.hkd.market.domain.repository;

import com.hkd.market.domain.entity.Kline;

import java.util.List;
import java.util.Optional;

/**
 * K线Repository接口
 *
 * 提供K线数据的增删改查功能
 * 具体实现在 market-infrastructure 模块
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
public interface KlineRepository {

    /**
     * 保存或更新K线
     *
     * @param kline K线数据
     * @return 保存后的K线
     */
    Kline save(Kline kline);

    /**
     * 查询指定交易对、周期、开盘时间的K线
     *
     * @param symbol 交易对 (例如: BTC-USDT)
     * @param interval K线周期
     * @param openTime 开盘时间 (时间戳秒)
     * @return K线数据
     */
    Optional<Kline> findBySymbolAndIntervalAndOpenTime(String symbol, String interval, Long openTime);

    /**
     * 查询指定时间范围的K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param startTime 开始时间 (时间戳秒)
     * @param endTime 结束时间 (时间戳秒)
     * @param limit 最大返回数量
     * @return K线列表
     */
    List<Kline> findByTimeRange(String symbol, String interval, Long startTime, Long endTime, Integer limit);

    /**
     * 查询最新的N条K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param limit 返回数量
     * @return K线列表（按时间倒序）
     */
    List<Kline> findLatestKlines(String symbol, String interval, Integer limit);

    /**
     * 查询最新的一条K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @return 最新K线
     */
    Optional<Kline> findLatestKline(String symbol, String interval);

    /**
     * 统计K线数量
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @return 数量
     */
    Long countBySymbolAndInterval(String symbol, String interval);
}
