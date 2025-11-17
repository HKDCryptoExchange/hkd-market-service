package com.hkd.market.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkd.market.infrastructure.persistence.po.KlinePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * K线 Mapper
 *
 * MyBatis Plus 映射接口
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Mapper
public interface KlineMapper extends BaseMapper<KlinePO> {

    /**
     * 查询指定时间范围的K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param startTime 开始时间（时间戳秒）
     * @param endTime 结束时间（时间戳秒）
     * @param limit 最大返回数量
     * @return K线列表
     */
    @Select("SELECT * FROM klines " +
            "WHERE symbol = #{symbol} " +
            "AND interval = #{interval} " +
            "AND open_time >= #{startTime} " +
            "AND open_time < #{endTime} " +
            "ORDER BY open_time ASC " +
            "LIMIT #{limit}")
    List<KlinePO> findByTimeRange(
            @Param("symbol") String symbol,
            @Param("interval") String interval,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("limit") Integer limit
    );

    /**
     * 查询最新的N条K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @param limit 返回数量
     * @return K线列表（按时间倒序）
     */
    @Select("SELECT * FROM klines " +
            "WHERE symbol = #{symbol} " +
            "AND interval = #{interval} " +
            "ORDER BY open_time DESC " +
            "LIMIT #{limit}")
    List<KlinePO> findLatestKlines(
            @Param("symbol") String symbol,
            @Param("interval") String interval,
            @Param("limit") Integer limit
    );

    /**
     * 查询最新的一条K线
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @return 最新K线
     */
    @Select("SELECT * FROM klines " +
            "WHERE symbol = #{symbol} " +
            "AND interval = #{interval} " +
            "ORDER BY open_time DESC " +
            "LIMIT 1")
    KlinePO findLatestKline(
            @Param("symbol") String symbol,
            @Param("interval") String interval
    );

    /**
     * 统计K线数量
     *
     * @param symbol 交易对
     * @param interval K线周期
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM klines " +
            "WHERE symbol = #{symbol} " +
            "AND interval = #{interval}")
    Long countBySymbolAndInterval(
            @Param("symbol") String symbol,
            @Param("interval") String interval
    );
}
