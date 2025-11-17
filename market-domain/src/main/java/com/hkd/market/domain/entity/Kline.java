package com.hkd.market.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线实体
 *
 * 存储在TimescaleDB中，按open_time分区
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("klines")
public class Kline {

    /**
     * 主键ID (SnowflakeId)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 交易对 (例如: BTC-USDT)
     */
    private String symbol;

    /**
     * K线周期 (1m/5m/15m/30m/1h/4h/1d/1w/1M)
     */
    private String interval;

    /**
     * K线开始时间 (时间戳秒)
     */
    private Long openTime;

    /**
     * K线结束时间 (时间戳秒)
     */
    private Long closeTime;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 成交量
     */
    private BigDecimal volume;

    /**
     * 成交额
     */
    private BigDecimal amount;

    /**
     * 成交笔数
     */
    private Integer tradeCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否已完成 (false=进行中, true=已完成)
     */
    private Boolean completed;
}
