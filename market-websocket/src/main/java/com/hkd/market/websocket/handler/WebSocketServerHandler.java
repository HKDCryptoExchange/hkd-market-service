package com.hkd.market.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkd.market.api.enums.WebSocketMessageType;
import com.hkd.market.websocket.protocol.WebSocketMessage;
import com.hkd.market.websocket.subscription.SubscriptionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket业务处理器
 *
 * 处理客户端消息：
 * - PING/PONG心跳
 * - SUBSCRIBE订阅
 * - UNSUBSCRIBE取消订阅
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final SubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("New WebSocket connection established: {}", ctx.channel().id().asShortText());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket connection closed: {}", ctx.channel().id().asShortText());

        // 清理订阅关系
        subscriptionManager.unsubscribeAll(ctx.channel());

        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 只处理文本消息
        if (!(frame instanceof TextWebSocketFrame textFrame)) {
            log.warn("Unsupported frame type: {}", frame.getClass().getName());
            return;
        }

        String text = textFrame.text();
        log.debug("Received message from {}: {}", ctx.channel().id().asShortText(), text);

        try {
            // 解析消息
            WebSocketMessage message = objectMapper.readValue(text, WebSocketMessage.class);

            // 处理消息
            handleMessage(ctx, message);
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", text, e);
            sendError(ctx, "INVALID_MESSAGE", "Invalid message format");
        }
    }

    /**
     * 处理消息
     */
    private void handleMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            WebSocketMessageType type = WebSocketMessageType.fromCode(message.getType());

            switch (type) {
                case PING -> handlePing(ctx);
                case SUBSCRIBE -> handleSubscribe(ctx, message);
                case UNSUBSCRIBE -> handleUnsubscribe(ctx, message);
                default -> sendError(ctx, "UNSUPPORTED_MESSAGE_TYPE", "Unsupported message type: " + message.getType());
            }
        } catch (IllegalArgumentException e) {
            sendError(ctx, "INVALID_MESSAGE_TYPE", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            sendError(ctx, "INTERNAL_ERROR", "Internal server error");
        }
    }

    /**
     * 处理PING消息
     */
    private void handlePing(ChannelHandlerContext ctx) {
        sendMessage(ctx, WebSocketMessage.pong());
    }

    /**
     * 处理SUBSCRIBE消息
     */
    private void handleSubscribe(ChannelHandlerContext ctx, WebSocketMessage message) {
        String channel = message.getChannel();
        String symbol = message.getSymbol();
        String interval = message.getInterval();

        // 验证参数
        if (channel == null || symbol == null) {
            sendError(ctx, "INVALID_PARAMS", "Missing required parameters: channel and symbol");
            return;
        }

        // 订阅
        subscriptionManager.subscribe(ctx.channel(), channel, symbol, interval);

        // 发送订阅成功响应
        sendMessage(ctx, WebSocketMessage.subscribed(channel, symbol, interval));

        log.info("Channel {} subscribed to {}:{}{}",
                ctx.channel().id().asShortText(),
                channel,
                symbol,
                interval != null ? ":" + interval : "");
    }

    /**
     * 处理UNSUBSCRIBE消息
     */
    private void handleUnsubscribe(ChannelHandlerContext ctx, WebSocketMessage message) {
        String channel = message.getChannel();
        String symbol = message.getSymbol();
        String interval = message.getInterval();

        // 验证参数
        if (channel == null || symbol == null) {
            sendError(ctx, "INVALID_PARAMS", "Missing required parameters: channel and symbol");
            return;
        }

        // 取消订阅
        subscriptionManager.unsubscribe(ctx.channel(), channel, symbol, interval);

        // 发送取消订阅成功响应
        sendMessage(ctx, WebSocketMessage.unsubscribed(channel, symbol, interval));

        log.info("Channel {} unsubscribed from {}:{}{}",
                ctx.channel().id().asShortText(),
                channel,
                symbol,
                interval != null ? ":" + interval : "");
    }

    /**
     * 发送消息
     */
    private void sendMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(ChannelHandlerContext ctx, String code, String message) {
        sendMessage(ctx, WebSocketMessage.error(code, message));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                log.warn("WebSocket connection idle timeout: {}", ctx.channel().id().asShortText());
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket handler exception: {}", ctx.channel().id().asShortText(), cause);
        ctx.close();
    }
}
