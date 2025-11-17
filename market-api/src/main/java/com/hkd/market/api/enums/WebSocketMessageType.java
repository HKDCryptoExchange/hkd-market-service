package com.hkd.market.api.enums;

import lombok.Getter;

/**
 * WebSocket消息类型枚举
 *
 * 用于标识客户端和服务端之间的消息类型
 */
@Getter
public enum WebSocketMessageType {

    // ========== 客户端消息 ==========

    /**
     * 订阅消息
     * 格式: {"type":"SUBSCRIBE","channel":"kline","symbol":"BTC-USDT","interval":"1m"}
     */
    SUBSCRIBE("subscribe"),

    /**
     * 取消订阅
     * 格式: {"type":"UNSUBSCRIBE","channel":"kline","symbol":"BTC-USDT","interval":"1m"}
     */
    UNSUBSCRIBE("unsubscribe"),

    /**
     * Ping心跳
     * 格式: {"type":"PING"}
     */
    PING("ping"),

    // ========== 服务端消息 ==========

    /**
     * Pong心跳响应
     * 格式: {"type":"PONG","timestamp":1234567890}
     */
    PONG("pong"),

    /**
     * K线数据推送
     * 格式: {"type":"KLINE","symbol":"BTC-USDT","interval":"1m","data":{...}}
     */
    KLINE("kline"),

    /**
     * 深度数据推送
     * 格式: {"type":"DEPTH","symbol":"BTC-USDT","data":{"bids":[...],"asks":[...]}}
     */
    DEPTH("depth"),

    /**
     * Ticker数据推送
     * 格式: {"type":"TICKER","symbol":"BTC-USDT","data":{...}}
     */
    TICKER("ticker"),

    /**
     * 成交记录推送
     * 格式: {"type":"TRADE","symbol":"BTC-USDT","data":{...}}
     */
    TRADE("trade"),

    /**
     * 24h统计推送
     * 格式: {"type":"STATS_24H","symbol":"BTC-USDT","data":{...}}
     */
    STATS_24H("stats_24h"),

    /**
     * 订阅成功响应
     * 格式: {"type":"SUBSCRIBED","channel":"kline","symbol":"BTC-USDT"}
     */
    SUBSCRIBED("subscribed"),

    /**
     * 取消订阅成功响应
     * 格式: {"type":"UNSUBSCRIBED","channel":"kline","symbol":"BTC-USDT"}
     */
    UNSUBSCRIBED("unsubscribed"),

    /**
     * 错误消息
     * 格式: {"type":"ERROR","code":"INVALID_SYMBOL","message":"Invalid symbol"}
     */
    ERROR("error");

    /**
     * 消息类型代码
     */
    private final String code;

    WebSocketMessageType(String code) {
        this.code = code;
    }

    /**
     * 根据code获取枚举值
     *
     * @param code 消息类型代码
     * @return WebSocketMessageType枚举
     * @throws IllegalArgumentException 如果code不存在
     */
    public static WebSocketMessageType fromCode(String code) {
        for (WebSocketMessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WebSocket message type: " + code);
    }
}
