package com.hkd.market.api.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 成交事件 - 严格按照 Kafka Schema 定义
 *
 * Topic: trading.matching-engine.trades.v1
 * Schema: .claude/contracts/kafka-schemas/trade-executed.json
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {

    /**
     * 事件唯一ID（用于幂等性检查）
     */
    @JsonProperty("event_id")
    private String eventId;

    /**
     * 事件类型 - 必须是 "TRADE_EXECUTED"
     */
    @JsonProperty("event_type")
    private String eventType;

    /**
     * 事件版本 - 必须是 "v1"
     */
    @JsonProperty("event_version")
    private String eventVersion;

    /**
     * 事件时间戳
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * 来源服务 - 必须是 "matching-engine"
     */
    @JsonProperty("source_service")
    private String sourceService;

    /**
     * 关联ID（用于链路追踪）
     */
    @JsonProperty("correlation_id")
    private String correlationId;

    /**
     * 事件载荷（业务数据）
     */
    @JsonProperty("payload")
    private TradePayload payload;

    /**
     * 成交载荷
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradePayload {

        /**
         * 成交ID
         */
        @JsonProperty("trade_id")
        private String tradeId;

        /**
         * 交易对，格式: BTC/USDT
         */
        @JsonProperty("symbol")
        private String symbol;

        /**
         * 成交价格（字符串，避免精度丢失）
         */
        @JsonProperty("price")
        private String price;

        /**
         * 成交数量（字符串，避免精度丢失）
         */
        @JsonProperty("quantity")
        private String quantity;

        /**
         * 买方订单ID
         */
        @JsonProperty("buyer_order_id")
        private String buyerOrderId;

        /**
         * 卖方订单ID
         */
        @JsonProperty("seller_order_id")
        private String sellerOrderId;

        /**
         * 买方用户ID
         */
        @JsonProperty("buyer_user_id")
        private String buyerUserId;

        /**
         * 卖方用户ID
         */
        @JsonProperty("seller_user_id")
        private String sellerUserId;

        /**
         * 买方手续费（基础币种）
         */
        @JsonProperty("buyer_fee")
        private String buyerFee;

        /**
         * 卖方手续费（报价币种）
         */
        @JsonProperty("seller_fee")
        private String sellerFee;

        /**
         * Maker 方向（BUY 或 SELL）
         */
        @JsonProperty("maker_side")
        private String makerSide;

        /**
         * 成交时间（毫秒精度）
         */
        @JsonProperty("timestamp")
        private Instant timestamp;
    }
}
