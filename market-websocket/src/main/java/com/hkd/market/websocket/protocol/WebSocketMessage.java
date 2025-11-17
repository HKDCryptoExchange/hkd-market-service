package com.hkd.market.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hkd.market.api.enums.WebSocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息协议
 *
 * 客户端和服务端之间交互的统一消息格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 频道 (kline/depth/ticker/trade)
     */
    private String channel;

    /**
     * 交易对 (例如: BTC-USDT)
     */
    private String symbol;

    /**
     * K线周期 (仅kline频道需要)
     */
    private String interval;

    /**
     * 消息数据
     */
    private Object data;

    /**
     * 时间戳 (毫秒)
     */
    private Long timestamp;

    /**
     * 错误代码 (仅ERROR类型)
     */
    private String code;

    /**
     * 错误消息 (仅ERROR类型)
     */
    private String message;

    /**
     * 创建PONG消息
     */
    public static WebSocketMessage pong() {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.PONG.getCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建SUBSCRIBED消息
     */
    public static WebSocketMessage subscribed(String channel, String symbol, String interval) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.SUBSCRIBED.getCode())
                .channel(channel)
                .symbol(symbol)
                .interval(interval)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建UNSUBSCRIBED消息
     */
    public static WebSocketMessage unsubscribed(String channel, String symbol, String interval) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.UNSUBSCRIBED.getCode())
                .channel(channel)
                .symbol(symbol)
                .interval(interval)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建ERROR消息
     */
    public static WebSocketMessage error(String code, String message) {
        return WebSocketMessage.builder()
                .type(WebSocketMessageType.ERROR.getCode())
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建数据推送消息
     */
    public static WebSocketMessage push(WebSocketMessageType type, String symbol, Object data) {
        return WebSocketMessage.builder()
                .type(type.getCode())
                .symbol(symbol)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
