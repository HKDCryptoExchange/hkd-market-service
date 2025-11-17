package com.hkd.market.websocket.subscription;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 订阅管理器
 *
 * 管理客户端订阅关系：
 * - 频道订阅 (kline, depth, ticker, trade)
 * - 连接管理
 * - 订阅查询
 */
@Slf4j
@Component
public class SubscriptionManager {

    /**
     * 订阅关系存储
     * Key: 订阅键 (例如: kline:BTC-USDT:1m)
     * Value: 订阅该频道的所有Channel
     */
    private final Map<String, Set<Channel>> subscriptions = new ConcurrentHashMap<>();

    /**
     * 连接订阅关系存储 (用于快速查找某个连接订阅了哪些频道)
     * Key: Channel
     * Value: 该连接订阅的所有订阅键
     */
    private final Map<Channel, Set<String>> channelSubscriptions = new ConcurrentHashMap<>();

    /**
     * 订阅
     *
     * @param channel 连接Channel
     * @param channel 频道 (kline/depth/ticker/trade)
     * @param symbol 交易对
     * @param interval K线周期 (可选，仅kline频道需要)
     */
    public void subscribe(Channel channel, String channelName, String symbol, String interval) {
        String subscriptionKey = buildSubscriptionKey(channelName, symbol, interval);

        // 添加到订阅关系
        subscriptions.computeIfAbsent(subscriptionKey, k -> new CopyOnWriteArraySet<>()).add(channel);

        // 添加到连接订阅关系
        channelSubscriptions.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(subscriptionKey);

        log.debug("Channel {} subscribed to {}", channel.id().asShortText(), subscriptionKey);
    }

    /**
     * 取消订阅
     *
     * @param channel 连接Channel
     * @param channelName 频道
     * @param symbol 交易对
     * @param interval K线周期 (可选)
     */
    public void unsubscribe(Channel channel, String channelName, String symbol, String interval) {
        String subscriptionKey = buildSubscriptionKey(channelName, symbol, interval);

        // 从订阅关系中移除
        Set<Channel> channels = subscriptions.get(subscriptionKey);
        if (channels != null) {
            channels.remove(channel);
            if (channels.isEmpty()) {
                subscriptions.remove(subscriptionKey);
            }
        }

        // 从连接订阅关系中移除
        Set<String> keys = channelSubscriptions.get(channel);
        if (keys != null) {
            keys.remove(subscriptionKey);
            if (keys.isEmpty()) {
                channelSubscriptions.remove(channel);
            }
        }

        log.debug("Channel {} unsubscribed from {}", channel.id().asShortText(), subscriptionKey);
    }

    /**
     * 取消连接的所有订阅
     *
     * @param channel 连接Channel
     */
    public void unsubscribeAll(Channel channel) {
        Set<String> keys = channelSubscriptions.remove(channel);
        if (keys != null) {
            for (String key : keys) {
                Set<Channel> channels = subscriptions.get(key);
                if (channels != null) {
                    channels.remove(channel);
                    if (channels.isEmpty()) {
                        subscriptions.remove(key);
                    }
                }
            }
        }

        log.debug("Channel {} unsubscribed from all channels", channel.id().asShortText());
    }

    /**
     * 获取订阅某个频道的所有连接
     *
     * @param channelName 频道
     * @param symbol 交易对
     * @param interval K线周期 (可选)
     * @return 订阅该频道的所有Channel
     */
    public Set<Channel> getSubscribers(String channelName, String symbol, String interval) {
        String subscriptionKey = buildSubscriptionKey(channelName, symbol, interval);
        return subscriptions.getOrDefault(subscriptionKey, Set.of());
    }

    /**
     * 获取连接订阅的所有频道
     *
     * @param channel 连接Channel
     * @return 该连接订阅的所有订阅键
     */
    public Set<String> getChannelSubscriptions(Channel channel) {
        return channelSubscriptions.getOrDefault(channel, Set.of());
    }

    /**
     * 获取总订阅数
     */
    public int getTotalSubscriptions() {
        return subscriptions.size();
    }

    /**
     * 获取总连接数
     */
    public int getTotalConnections() {
        return channelSubscriptions.size();
    }

    /**
     * 构建订阅键
     *
     * @param channelName 频道
     * @param symbol 交易对
     * @param interval K线周期 (可选)
     * @return 订阅键 (例如: kline:BTC-USDT:1m)
     */
    private String buildSubscriptionKey(String channelName, String symbol, String interval) {
        if (interval != null && !interval.isEmpty()) {
            return channelName + ":" + symbol + ":" + interval;
        }
        return channelName + ":" + symbol;
    }
}
