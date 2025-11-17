package com.hkd.market.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hkd.market.api.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 市场成交记录实体
 *
 * 存储在PostgreSQL中，按月分表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("market_trades")
public class MarketTrade {

    /**
     * 主键ID (SnowflakeId)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 交易ID (从matching-engine获取)
     */
    private Long tradeId;

    /**
     * 交易对 (例如: BTC-USDT)
     */
    private String symbol;

    /**
     * 成交价格
     */
    private BigDecimal price;

    /**
     * 成交数量
     */
    private BigDecimal quantity;

    /**
     * 成交额
     */
    private BigDecimal amount;

    /**
     * Taker方向 (买/卖)
     */
    private OrderSide takerSide;

    /**
     * 买方订单ID
     */
    private Long buyOrderId;

    /**
     * 卖方订单ID
     */
    private Long sellOrderId;

    /**
     * 成交时间 (时间戳秒)
     */
    private Long tradeTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
