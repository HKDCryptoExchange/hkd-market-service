# HKD Market Service

HKD交易所行情数据服务 - 实时K线数据生成、WebSocket推送、深度数据、24h统计

## 功能概述

### 核心功能

1. **K线数据生成与存储**
   - 支持9种周期：1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w, 1M
   - 从Kafka消费成交记录，实时聚合K线数据
   - 存储到TimescaleDB (Hypertable，按时间分区)
   - Redis缓存最近100根K线

2. **WebSocket实时推送**
   - 基于Netty 4.1，支持10万+并发连接
   - 订阅/取消订阅机制
   - 心跳检测 (30秒间隔)
   - 推送K线、深度、Ticker、成交记录

3. **深度数据服务**
   - 从matching-engine获取实时订单簿
   - 支持10/20/50档深度
   - Redis缓存 (1秒TTL)
   - 100ms间隔推送

4. **24h统计计算**
   - 滑动窗口计算 (价格变化、涨跌幅、成交量等)
   - Redis Sorted Set存储24h成交数据
   - 每分钟更新统计

5. **成交记录服务**
   - Kafka消费成交记录
   - Redis缓存最近1000笔
   - PostgreSQL持久化 (按月分表)
   - WebSocket实时推送

## 技术栈

- **Java 21** + **Spring Boot 3.2**
- **Netty 4.1** - WebSocket服务器
- **TimescaleDB** - K线数据存储 (PostgreSQL + Timescale扩展)
- **Redis 7.2** - 缓存
- **Kafka 3.6** - 消息队列
- **MyBatis Plus 3.5** - ORM
- **Flyway 10.2** - 数据库迁移

## 项目结构

```
hkd-market-service/
├── market-api/              # API定义 (DTO, Request, Response, Enums)
├── market-domain/           # 领域模型 (Entity, Repository接口, 领域服务)
├── market-application/      # 应用服务 (业务逻辑)
├── market-infrastructure/   # 基础设施 (数据库, 缓存, Kafka消费者)
├── market-websocket/        # WebSocket服务 (Netty服务器, 订阅管理)
└── market-bootstrap/        # 启动模块 (配置, Controller)
```

## 快速开始

### 前置条件

- Java 21
- Maven 3.8+
- PostgreSQL 16 + TimescaleDB扩展
- Redis 7.2
- Kafka 3.6

### 安装依赖

```bash
# 安装hkd-common (如果还未安装)
cd ../hkd-common
mvn clean install

# 构建market-service
cd ../hkd-market-service
mvn clean package
```

### 配置数据库

```sql
-- 创建数据库
CREATE DATABASE hkd_market;

-- 启用TimescaleDB扩展
\c hkd_market
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Flyway会自动创建表结构
-- 需要手动执行以下SQL启用Hypertable:
SELECT create_hypertable('klines', 'open_time', chunk_time_interval => 86400);
SELECT add_retention_policy('klines', INTERVAL '5 years');
```

### 配置环境变量

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=hkd_market
export DB_USERNAME=hkd_admin
export DB_PASSWORD=hkd_dev_password_2024

export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=hkd_redis_2024

export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

export MATCHING_ENGINE_WS_URL=ws://localhost:8005/ws/matching
```

### 启动服务

```bash
# 使用dev配置启动
java -jar market-bootstrap/target/market-bootstrap-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# 或使用Maven启动
mvn spring-boot:run -pl market-bootstrap
```

服务将在以下端口启动：
- **HTTP REST API**: `http://localhost:8010`
- **WebSocket**: `ws://localhost:8010/ws/market`
- **Actuator**: `http://localhost:8010/actuator`

## API文档

### REST API

#### 健康检查

```bash
GET /api/v1/health
```

响应示例：
```json
{
  "status": "UP",
  "service": "market-service",
  "version": "1.0.0",
  "timestamp": "2025-11-17T12:00:00"
}
```

#### WebSocket连接统计

```bash
GET /api/v1/websocket/stats
```

响应示例：
```json
{
  "connections": 1024,
  "subscriptions": 2048,
  "timestamp": "2025-11-17T12:00:00"
}
```

### WebSocket协议

#### 连接

```
ws://localhost:8010/ws/market
```

#### 订阅K线

```json
{
  "type": "subscribe",
  "channel": "kline",
  "symbol": "BTC-USDT",
  "interval": "1m"
}
```

#### 订阅深度

```json
{
  "type": "subscribe",
  "channel": "depth",
  "symbol": "BTC-USDT"
}
```

#### 订阅Ticker

```json
{
  "type": "subscribe",
  "channel": "ticker",
  "symbol": "BTC-USDT"
}
```

#### 订阅成交记录

```json
{
  "type": "subscribe",
  "channel": "trade",
  "symbol": "BTC-USDT"
}
```

#### 取消订阅

```json
{
  "type": "unsubscribe",
  "channel": "kline",
  "symbol": "BTC-USDT",
  "interval": "1m"
}
```

#### 心跳

```json
{
  "type": "ping"
}
```

服务器响应：
```json
{
  "type": "pong",
  "timestamp": 1234567890
}
```

## 开发指南

### 添加新的K线周期

1. 在 `KlineInterval` 枚举中添加新周期
2. 更新 `application.yml` 中的 `hkd.market.kline.intervals` 配置
3. 重启服务

### 添加新的WebSocket频道

1. 在 `WebSocketMessageType` 枚举中添加新消息类型
2. 在 `WebSocketServerHandler` 中添加处理逻辑
3. 实现数据推送逻辑

### 数据库迁移

使用Flyway进行数据库版本管理：

```bash
# 创建新的迁移脚本
# market-infrastructure/src/main/resources/db/migration/V4__description.sql

# Flyway会在服务启动时自动执行
```

## 监控与运维

### Prometheus指标

```bash
# 访问Prometheus指标
curl http://localhost:8010/actuator/prometheus
```

关键指标：
- `websocket_connections_total` - WebSocket连接数
- `websocket_subscriptions_total` - 订阅总数
- `kline_generation_duration_seconds` - K线生成耗时
- `websocket_message_sent_total` - WebSocket消息发送总数

### 健康检查

```bash
# 服务健康状态
curl http://localhost:8010/actuator/health

# 数据库连接状态
curl http://localhost:8010/actuator/health/db

# Redis连接状态
curl http://localhost:8010/actuator/health/redis
```

## 依赖服务

- **matching-engine (8005)**: 获取实时订单簿数据
- **trading-service**: 消费成交记录 (Kafka topic: `trade.executed`)

## 性能指标

- WebSocket并发连接: > 100,000
- 推送延迟: < 50ms (P99)
- K线查询: < 100ms (P99)
- 深度查询: < 50ms (P99)
- 服务可用性: 99.95%

## 文档参考

- [Epic文档](.claude/docs/epic.md)
- [Git工作流](.claude/docs/git-workflow.md)
- [Spring Boot模板](.claude/docs/spring-boot-template.md)

## 许可证

Copyright © 2025 HKD Exchange. All rights reserved.
