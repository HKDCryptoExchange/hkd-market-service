package com.hkd.market.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线持久化对象
 *
 * 映射到 klines 表
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("klines")
public class KlinePO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 交易对（例如: BTC-USDT）
     */
    @TableField("symbol")
    private String symbol;

    /**
     * K线周期（例如: 1m, 5m, 1h, 1d）
     */
    @TableField("interval")
    private String interval;

    /**
     * 开盘时间（时间戳秒）
     */
    @TableField("open_time")
    private Long openTime;

    /**
     * 收盘时间（时间戳秒）
     */
    @TableField("close_time")
    private Long closeTime;

    /**
     * 开盘价
     */
    @TableField("open")
    private BigDecimal open;

    /**
     * 最高价
     */
    @TableField("high")
    private BigDecimal high;

    /**
     * 最低价
     */
    @TableField("low")
    private BigDecimal low;

    /**
     * 收盘价
     */
    @TableField("close")
    private BigDecimal close;

    /**
     * 成交量（基础币种）
     */
    @TableField("volume")
    private BigDecimal volume;

    /**
     * 成交额（报价币种）
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 成交笔数
     */
    @TableField("trade_count")
    private Integer tradeCount;

    /**
     * 是否已完成
     */
    @TableField("completed")
    private Boolean completed;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
