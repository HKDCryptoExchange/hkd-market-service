---
name: market-domain
status: backlog
created: 2025-11-17T01:03:31Z
progress: 0%
prd: .claude/prds/market-domain.md
github: https://github.com/HKDCryptoExchange/hkd-project-management/blob/main/.claude/epics/market-domain/epic.md
---

# Epic: 行情域开发 (Market Domain)

## Overview

实现HKD交易所的实时行情数据管理与推送系统,包括market-service微服务。采用Java 21 + WebSocket + TimescaleDB技术栈,支持K线数据生成(9种周期)、实时深度数据、24h统计、Ticker数据、成交记录,通过WebSocket实现毫秒级行情推送,支持10万+并发连接。

## Architecture Decisions

### 1. 单服务架构
- **market-service(8010)**: K线 + 深度 + Ticker + WebSocket推送

**理由**: 行情数据关联性强,单服务便于数据共享和性能优化,减少跨服务调用

### 2. K线生成策略
- **Kafka消费**: 消费成交记录(trades topic)
- **实时聚合**: 按symbol+interval分组聚合生成K线
- **TimescaleDB存储**: 时序数据库优化K线查询
- **9种周期**: 1m/5m/15m/30m/1h/4h/1d/1w/1M

**理由**: Kafka流式处理实时生成K线,TimescaleDB时序优化,9种周期满足不同分析需求

### 3. WebSocket推送架构
- **Netty WebSocket Server**: 支持10万并发连接
- **订阅管理**: 用户订阅K线/深度/Ticker/成交记录
- **推送频率**: K线实时,深度100ms,Ticker 1秒
- **心跳机制**: 30秒心跳检测,断线自动重连

**理由**: Netty高性能,订阅模式减少无效推送,分级推送频率平衡实时性和性能

### 4. 深度数据获取
- **从matching-engine获取**: 实时订单簿数据
- **Redis缓存**: 10/20/50档深度,1秒刷新
- **WebSocket推送**: 100ms间隔推送

**理由**: 直接从撮合引擎获取最新数据,Redis缓存降低撮合引擎压力

## Technical Approach

### Backend Services

**market-service** (Java 21 + Spring Boot 3.2):
- **K线数据生成**:
  - Kafka消费成交记录
  - 按symbol+interval聚合(open/high/low/close/volume)
  - 存储TimescaleDB
  - WebSocket推送订阅者

- **K线查询**:
  - 查询最近N根K线(N≤1500)
  - 按时间范围查询
  - 分页查询历史K线
  - Redis缓存(最近100根K线,TTL 1分钟)

- **深度数据**:
  - 从matching-engine获取订单簿
  - 聚合10/20/50档深度
  - Redis缓存(1秒刷新)
  - WebSocket推送(100ms间隔)

- **24h统计**:
  - 滑动窗口(Redis Sorted Set存储24h成交)
  - 每分钟重新计算(price_change/high/low/volume等)
  - Redis缓存(1分钟TTL)

- **Ticker数据**:
  - 24h统计 + 最新价格 + 最优买卖价
  - 每秒更新
  - WebSocket推送所有交易对Ticker

- **成交记录**:
  - Kafka消费成交记录
  - Redis缓存最近1000笔(List)
  - PostgreSQL持久化(按月分表)
  - WebSocket实时推送

- **WebSocket推送**:
  - Netty WebSocket Server
  - 订阅管理(SUBSCRIBE/UNSUBSCRIBE)
  - 心跳机制(30秒ping/pong)
  - 断线重连

### Infrastructure

**数据库**:
- TimescaleDB (PostgreSQL 16 + Timescale extension)
  - klines表(Hypertable,按open_time分区)
  - 保留5年数据
- PostgreSQL 16
  - market_trades表(按月分表)
  - 保留1年数据

**缓存**:
- Redis 7.2集群
  - K线缓存: `market:kline:{symbol}:{interval}`
  - 深度缓存: `market:depth:{symbol}`
  - Ticker缓存: `market:ticker:{symbol}`
  - 24h统计: `market:24h:{symbol}`
  - 成交记录: `market:trades:{symbol}`

**消息队列**:
- Kafka: 消费trade.executed topic

**监控**:
- Prometheus: WebSocket连接数/推送延迟/K线生成延迟
- Grafana: 实时监控

## Implementation Strategy

**Phase 1 (2周)**: K线数据生成与存储
- Kafka消费成交记录
- K线聚合逻辑(9种周期)
- TimescaleDB存储
- K线查询接口
- 单元测试

**Phase 2 (2.5周)**: WebSocket推送服务
- Netty WebSocket Server
- 订阅管理
- K线推送
- 深度推送
- Ticker推送
- 成交记录推送
- 心跳机制
- 单元测试

**Phase 3 (1.5周)**: 深度数据与24h统计
- 深度数据获取(从matching-engine)
- Redis缓存(10/20/50档)
- 24h统计计算(滑动窗口)
- Ticker生成
- 单元测试

**Phase 4 (1周)**: 成交记录服务
- Kafka消费
- Redis缓存(最近1000笔)
- PostgreSQL持久化
- WebSocket推送
- 单元测试

**Phase 5 (1周)**: 集成测试 + 性能压测
- K线生成集成测试
- WebSocket推送集成测试
- 深度数据集成测试
- WebSocket并发压测(目标10万连接)
- 推送延迟测试

**风险缓解**:
- WebSocket连接数过多 → 水平扩展(Nginx负载均衡)
- Kafka消息堆积 → 增加分区+消费者
- TimescaleDB慢查询 → 索引优化+分区策略

## Task Breakdown Preview

1. **K线数据生成与存储** (32h) - Kafka消费/K线聚合/TimescaleDB存储/K线查询接口
2. **WebSocket推送服务** (40h) - Netty Server/订阅管理/K线/深度/Ticker/成交推送/心跳机制
3. **深度数据服务** (24h) - 深度获取/Redis缓存/深度查询接口/WebSocket推送
4. **24h统计计算** (20h) - 滑动窗口/24h统计/Ticker生成/统计查询接口
5. **成交记录服务** (16h) - Kafka消费/Redis缓存/PostgreSQL持久化/WebSocket推送
6. **数据库Schema设计** (8h) - klines表(Hypertable)/market_trades表(分表)/索引优化/Flyway
7. **集成测试** (16h) - K线生成/WebSocket推送/深度数据/24h统计集成测试
8. **性能压测** (16h) - WebSocket并发压测/K线查询压测/推送延迟测试
9. **文档编写** (8h) - API文档(REST + WebSocket)/部署文档/运维手册

**总估算**: 180小时 ≈ 6-8周 (按2人并行开发)

## Dependencies

**外部服务依赖**:
- matching-engine: 获取实时订单簿(深度数据)
- trading-service: 消费成交记录(Kafka)

**基础设施**:
- TimescaleDB (PostgreSQL 16 + Timescale extension)
- PostgreSQL 16
- Redis 7.2
- Kafka 3.6

## Success Criteria (Technical)

**功能**:
- ✅ 支持9种K线周期
- ✅ WebSocket推送正常
- ✅ K线数据准确率100%
- ✅ 24h统计准确

**性能**:
- ✅ WebSocket并发 > 10万连接
- ✅ 推送延迟 < 50ms
- ✅ K线查询P99 < 100ms
- ✅ 深度查询P99 < 50ms

**可用性**:
- ✅ 服务可用性99.95%
- ✅ K线数据零丢失

## Estimated Effort

**总工时**: 180小时

**时间线** (按2人并行开发):
- Week 1-2: K线数据生成与存储 + 数据库Schema
- Week 3-4.5: WebSocket推送服务
- Week 5-6.5: 深度数据 + 24h统计 + 成交记录
- Week 7: 集成测试
- Week 8: 性能压测 + 优化 + 文档

**资源需求**:
- Java开发工程师 × 2
- QA测试工程师 × 1 (兼职)
- DevOps工程师 × 1 (兼职)

**关键路径**:
1. K线数据生成 (阻塞WebSocket推送测试)
2. WebSocket推送服务 (复杂度高,耗时长)
3. 性能压测 (需达到10万连接指标)
