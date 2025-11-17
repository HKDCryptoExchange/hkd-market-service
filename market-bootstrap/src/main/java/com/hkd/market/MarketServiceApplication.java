package com.hkd.market;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HKD Market Service - Main Application Entry Point
 *
 * 功能:
 * - K线数据生成与存储 (9种周期: 1m/5m/15m/30m/1h/4h/1d/1w/1M)
 * - WebSocket实时推送 (Netty, 支持10万+并发)
 * - 深度数据服务
 * - 24h统计计算
 * - 成交记录服务
 *
 * 技术栈:
 * - Java 21 + Spring Boot 3.2
 * - Netty 4.1 (WebSocket Server)
 * - TimescaleDB (K线数据存储)
 * - Redis (缓存)
 * - Kafka (消费成交记录)
 *
 * 端口: 8010
 *
 * @author HKD Development Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MarketServiceApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(MarketServiceApplication.class, args);
            log.info("========================================");
            log.info("   HKD Market Service Started Successfully");
            log.info("   Port: 8010");
            log.info("   WebSocket: ws://localhost:8010/ws/market");
            log.info("========================================");
        } catch (Exception e) {
            log.error("Failed to start Market Service", e);
            System.exit(1);
        }
    }
}
