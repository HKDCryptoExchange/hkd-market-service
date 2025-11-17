package com.hkd.market.websocket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkd.market.websocket.handler.WebSocketServerHandler;
import com.hkd.market.websocket.subscription.SubscriptionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket服务器初始化器
 *
 * 配置Netty Pipeline：
 * 1. HTTP编解码
 * 2. HTTP聚合
 * 3. WebSocket协议处理
 * 4. 心跳检测
 * 5. 业务逻辑处理
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String websocketPath;
    private final int maxFrameSize;
    private final long heartbeatInterval;
    private final SubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP编解码器
        pipeline.addLast("http-codec", new HttpServerCodec());

        // HTTP聚合器 (将多个HTTP消息聚合为一个FullHttpRequest)
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));

        // HTTP分块写处理器
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());

        // WebSocket压缩扩展
        pipeline.addLast("websocket-compression", new WebSocketServerCompressionHandler());

        // WebSocket协议处理器
        pipeline.addLast("websocket-protocol", new WebSocketServerProtocolHandler(
                websocketPath,     // WebSocket路径
                null,              // 子协议
                true,              // 允许扩展
                maxFrameSize,      // 最大帧大小
                false,             // 允许mask mismatch
                true,              // 检查UTF-8
                true               // 丢弃聚合的HTTP请求
        ));

        // 心跳检测 (读空闲超时)
        // readerIdleTime: 如果在指定时间内没有读到数据，触发IdleStateEvent
        long readerIdleTime = heartbeatInterval * 2; // 2倍心跳间隔
        pipeline.addLast("idle-state", new IdleStateHandler(
                readerIdleTime, 0, 0, TimeUnit.MILLISECONDS
        ));

        // 业务逻辑处理器
        pipeline.addLast("websocket-handler", new WebSocketServerHandler(
                subscriptionManager,
                objectMapper
        ));

        log.debug("WebSocket pipeline initialized for channel: {}", ch.id().asShortText());
    }
}
