package com.hkd.market.websocket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkd.market.websocket.subscription.SubscriptionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Netty WebSocket服务器
 *
 * 功能：
 * - 启动WebSocket服务器
 * - 管理事件循环组
 * - 优雅关闭
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hkd.market.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketServer {

    private final SubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;

    @Value("${hkd.market.websocket.port:8010}")
    private int port;

    @Value("${hkd.market.websocket.path:/ws/market}")
    private String path;

    @Value("${hkd.market.websocket.max-frame-size:65536}")
    private int maxFrameSize;

    @Value("${hkd.market.websocket.heartbeat-interval:30000}")
    private long heartbeatInterval;

    @Value("${hkd.market.websocket.boss-threads:1}")
    private int bossThreads;

    @Value("${hkd.market.websocket.worker-threads:8}")
    private int workerThreads;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 启动WebSocket服务器
     */
    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                log.info("Starting WebSocket server on port {} with path {}", port, path);

                // 创建事件循环组
                bossGroup = new NioEventLoopGroup(bossThreads);
                workerGroup = new NioEventLoopGroup(workerThreads);

                // 配置服务器
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new WebSocketServerInitializer(
                                path,
                                maxFrameSize,
                                heartbeatInterval,
                                subscriptionManager,
                                objectMapper
                        ));

                // 绑定端口并启动服务器
                ChannelFuture future = bootstrap.bind(port).sync();
                serverChannel = future.channel();

                log.info("========================================");
                log.info("   WebSocket Server Started Successfully");
                log.info("   Port: {}", port);
                log.info("   Path: {}", path);
                log.info("   Max Frame Size: {} bytes", maxFrameSize);
                log.info("   Heartbeat Interval: {} ms", heartbeatInterval);
                log.info("   Boss Threads: {}", bossThreads);
                log.info("   Worker Threads: {}", workerThreads);
                log.info("   WebSocket URL: ws://localhost:{}{}", port, path);
                log.info("========================================");

                // 等待服务器关闭
                serverChannel.closeFuture().sync();

            } catch (InterruptedException e) {
                log.error("WebSocket server interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to start WebSocket server", e);
            } finally {
                shutdown();
            }
        }, "WebSocket-Server").start();
    }

    /**
     * 优雅关闭服务器
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket server...");

        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        log.info("WebSocket server shut down successfully");
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return subscriptionManager.getTotalConnections();
    }

    /**
     * 获取当前订阅数
     */
    public int getSubscriptionCount() {
        return subscriptionManager.getTotalSubscriptions();
    }
}
