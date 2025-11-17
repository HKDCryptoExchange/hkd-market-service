# HKD Exchange - API 接口治理规范

## 1. 服务间通信协议标准

### 1.1 通信协议选择矩阵

| 场景 | 协议 | 理由 | 示例 |
|------|------|------|------|
| **同步核心业务调用** | gRPC | 高性能、类型安全、代码生成 | account-service.freezeBalance() |
| **对外API网关** | REST (JSON) | 标准化、易调试、广泛支持 | gateway → 各服务 |
| **异步事件通知** | Kafka | 解耦、可靠、削峰填谷 | matching-engine → settlement |
| **实时数据推送** | WebSocket | 双向、低延迟 | market-service → Web前端 |

### 1.2 强制规则

✅ **必须使用 gRPC 的场景**：
- 资金操作：`account-service` 的冻结/解冻/转账接口
- 风控检查：`risk-service` 的风控评估接口
- 用户认证：`auth-service` 的Token验证接口
- 钱包操作：`wallet-service` 的签名/转账接口

✅ **必须使用 REST 的场景**：
- API Gateway 对外暴露的所有接口
- 管理后台的所有操作接口
- 第三方集成接口

✅ **必须使用 Kafka 的场景**：
- 撮合引擎成交通知 → 清算服务
- 业务事件通知 → 通知服务
- 充值到账通知 → 账户服务
- 所有审计日志

---

## 2. gRPC 服务定义规范

### 2.1 核心服务 Proto 定义

#### account-service.proto
```protobuf
syntax = "proto3";

package hkd.account.v1;

option java_package = "com.hkd.account.grpc";
option java_multiple_files = true;
option go_package = "github.com/HKDCryptoExchange/hkd-order-gateway/pb/account/v1";

// 账户服务
service AccountService {
  // 冻结资金
  rpc FreezeBalance(FreezeBalanceRequest) returns (FreezeBalanceResponse);

  // 解冻资金
  rpc UnfreezeBalance(UnfreezeBalanceRequest) returns (UnfreezeBalanceResponse);

  // 资金转移（清算专用）
  rpc TransferBalance(TransferBalanceRequest) returns (TransferBalanceResponse);

  // 查询余额
  rpc GetBalance(GetBalanceRequest) returns (GetBalanceResponse);

  // 批量查询余额（settlement批量清算）
  rpc BatchGetBalance(BatchGetBalanceRequest) returns (BatchGetBalanceResponse);

  // 批量资金转移（settlement批量清算）
  rpc BatchTransferBalance(BatchTransferBalanceRequest) returns (BatchTransferBalanceResponse);
}

message FreezeBalanceRequest {
  string user_id = 1;
  string currency = 2;          // BTC, ETH, USDT
  string amount = 3;             // 金额（字符串避免精度丢失）
  string business_id = 4;        // 业务ID（订单ID/提现ID）
  string business_type = 5;      // ORDER, WITHDRAW
  string idempotent_key = 6;     // 幂等键（防止重复冻结）
}

message FreezeBalanceResponse {
  bool success = 1;
  string message = 2;
  string freeze_id = 3;          // 冻结记录ID
  string available_balance = 4;  // 冻结后可用余额
  string frozen_balance = 5;     // 冻结后冻结余额
}

message UnfreezeBalanceRequest {
  string user_id = 1;
  string currency = 2;
  string amount = 3;
  string business_id = 4;
  string business_type = 5;
  string idempotent_key = 6;
}

message UnfreezeBalanceResponse {
  bool success = 1;
  string message = 2;
  string available_balance = 3;
  string frozen_balance = 4;
}

message TransferBalanceRequest {
  string from_user_id = 1;
  string to_user_id = 2;
  string currency = 3;
  string amount = 4;
  string transfer_type = 5;      // TRADE_SETTLEMENT, WITHDRAW, DEPOSIT
  string business_id = 6;        // 业务ID（trade_id/withdraw_id）
  string idempotent_key = 7;
}

message TransferBalanceResponse {
  bool success = 1;
  string message = 2;
  string transfer_id = 3;
}

message GetBalanceRequest {
  string user_id = 1;
  string currency = 2;
}

message GetBalanceResponse {
  string user_id = 1;
  string currency = 2;
  string available_balance = 3;  // 可用余额
  string frozen_balance = 4;     // 冻结余额
  string total_balance = 5;      // 总余额
}

message BatchGetBalanceRequest {
  repeated string user_ids = 1;
  string currency = 2;
}

message BatchGetBalanceResponse {
  repeated GetBalanceResponse balances = 1;
}

message BatchTransferBalanceRequest {
  repeated TransferBalanceRequest transfers = 1;
}

message BatchTransferBalanceResponse {
  bool success = 1;
  int32 success_count = 2;
  int32 failed_count = 3;
  repeated TransferResult results = 4;
}

message TransferResult {
  string business_id = 1;
  bool success = 2;
  string message = 3;
}
```

#### auth-service.proto
```protobuf
syntax = "proto3";

package hkd.auth.v1;

option java_package = "com.hkd.auth.grpc";
option java_multiple_files = true;
option go_package = "github.com/HKDCryptoExchange/hkd-order-gateway/pb/auth/v1";

// 认证服务
service AuthService {
  // 验证 JWT Token
  rpc ValidateToken(ValidateTokenRequest) returns (ValidateTokenResponse);

  // 验证 TOTP（双因素认证）
  rpc ValidateTOTP(ValidateTOTPRequest) returns (ValidateTOTPResponse);

  // 检查用户权限
  rpc CheckPermission(CheckPermissionRequest) returns (CheckPermissionResponse);
}

message ValidateTokenRequest {
  string access_token = 1;
}

message ValidateTokenResponse {
  bool valid = 1;
  string user_id = 2;
  string username = 3;
  string email = 4;
  repeated string roles = 5;
  int64 expires_at = 6;  // Unix timestamp
}

message ValidateTOTPRequest {
  string user_id = 1;
  string totp_code = 2;
}

message ValidateTOTPResponse {
  bool valid = 1;
  string message = 2;
}

message CheckPermissionRequest {
  string user_id = 1;
  string resource = 2;  // TRADE, WITHDRAW, ADMIN
  string action = 3;    // CREATE, READ, UPDATE, DELETE
}

message CheckPermissionResponse {
  bool allowed = 1;
  string message = 2;
}
```

#### risk-service.proto
```protobuf
syntax = "proto3";

package hkd.risk.v1;

option java_package = "com.hkd.risk.grpc";
option java_multiple_files = true;
option go_package = "github.com/HKDCryptoExchange/hkd-order-gateway/pb/risk/v1";

// 风控服务
service RiskService {
  // 订单风控检查
  rpc CheckOrderRisk(CheckOrderRiskRequest) returns (CheckOrderRiskResponse);

  // 提现风控检查
  rpc CheckWithdrawRisk(CheckWithdrawRiskRequest) returns (CheckWithdrawRiskResponse);

  // 用户行为异常检测
  rpc DetectAnomalyBehavior(DetectAnomalyRequest) returns (DetectAnomalyResponse);
}

message CheckOrderRiskRequest {
  string user_id = 1;
  string symbol = 2;
  string side = 3;           // BUY, SELL
  string order_type = 4;     // LIMIT, MARKET
  string price = 5;
  string quantity = 6;
}

message CheckOrderRiskResponse {
  bool allowed = 1;
  string risk_level = 2;     // LOW, MEDIUM, HIGH, CRITICAL
  string reason = 3;
  repeated string warnings = 4;
}

message CheckWithdrawRiskRequest {
  string user_id = 1;
  string currency = 2;
  string amount = 3;
  string address = 4;
  string network = 5;
}

message CheckWithdrawRiskResponse {
  bool allowed = 1;
  bool need_manual_review = 2;
  string risk_level = 3;
  string reason = 4;
}

message DetectAnomalyRequest {
  string user_id = 1;
  string behavior_type = 2;  // LOGIN, TRADE, WITHDRAW
  map<string, string> features = 3;  // 行为特征
}

message DetectAnomalyResponse {
  bool is_anomaly = 1;
  double anomaly_score = 2;  // 0.0-1.0
  string description = 3;
}
```

#### wallet-service.proto
```protobuf
syntax = "proto3";

package hkd.wallet.v1;

option java_package = "com.hkd.wallet.grpc";
option java_multiple_files = true;

// 钱包服务
service WalletService {
  // 分配充值地址
  rpc AssignDepositAddress(AssignDepositAddressRequest) returns (AssignDepositAddressResponse);

  // 签名交易（提现）
  rpc SignTransaction(SignTransactionRequest) returns (SignTransactionResponse);

  // 广播交易
  rpc BroadcastTransaction(BroadcastTransactionRequest) returns (BroadcastTransactionResponse);
}

message AssignDepositAddressRequest {
  string user_id = 1;
  string currency = 2;
  string network = 3;  // ERC20, BEP20, TRC20
}

message AssignDepositAddressResponse {
  bool success = 1;
  string address = 2;
  string tag = 3;      // 某些币种需要tag（如XRP）
}

message SignTransactionRequest {
  string currency = 1;
  string network = 2;
  string to_address = 3;
  string amount = 4;
  string withdraw_id = 5;
}

message SignTransactionResponse {
  bool success = 1;
  string signed_tx = 2;  // 签名后的交易数据
  string tx_hash = 3;
}

message BroadcastTransactionRequest {
  string currency = 1;
  string network = 2;
  string signed_tx = 3;
}

message BroadcastTransactionResponse {
  bool success = 1;
  string tx_hash = 2;
  string message = 3;
}
```

---

## 3. Kafka 消息格式规范

### 3.1 Topic 命名规范

格式：`{domain}.{service}.{event-type}.{version}`

示例：
- `trading.matching-engine.trades.v1` - 成交消息
- `trading.settlement.completed.v1` - 清算完成
- `asset.deposit.confirmed.v1` - 充值确认
- `asset.withdraw.completed.v1` - 提现完成
- `notification.email.sent.v1` - 邮件发送
- `audit.account.balance-changed.v1` - 余额变动审计

### 3.2 消息格式

所有Kafka消息必须包含以下字段：

```json
{
  "event_id": "uuid",           // 事件唯一ID
  "event_type": "string",       // 事件类型
  "event_version": "v1",        // 事件版本
  "timestamp": "ISO8601",       // 事件时间
  "source_service": "string",   // 来源服务
  "correlation_id": "uuid",     // 关联ID（追踪整个业务流程）
  "payload": {                  // 事件载荷（具体业务数据）
    ...
  }
}
```

### 3.3 核心消息定义

#### 成交消息（matching-engine → settlement）
**Topic**: `trading.matching-engine.trades.v1`

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "TRADE_EXECUTED",
  "event_version": "v1",
  "timestamp": "2024-11-17T10:30:00Z",
  "source_service": "matching-engine",
  "correlation_id": "550e8400-e29b-41d4-a716-446655440001",
  "payload": {
    "trade_id": "uuid",
    "symbol": "BTC/USDT",
    "price": "50000.00",
    "quantity": "1.5",
    "buyer_order_id": "uuid",
    "seller_order_id": "uuid",
    "buyer_user_id": "user123",
    "seller_user_id": "user456",
    "buyer_fee": "0.15",         // BTC
    "seller_fee": "75.00",       // USDT
    "maker_side": "BUY",         // 谁是Maker
    "timestamp": "2024-11-17T10:30:00.123Z"
  }
}
```

#### 充值确认消息（deposit-service → account-service）
**Topic**: `asset.deposit.confirmed.v1`

```json
{
  "event_id": "uuid",
  "event_type": "DEPOSIT_CONFIRMED",
  "event_version": "v1",
  "timestamp": "2024-11-17T10:30:00Z",
  "source_service": "deposit-service",
  "correlation_id": "uuid",
  "payload": {
    "deposit_id": "uuid",
    "user_id": "user123",
    "currency": "USDT",
    "network": "ERC20",
    "amount": "10000.00",
    "tx_hash": "0x123abc...",
    "confirmations": 12,
    "from_address": "0xabc...",
    "to_address": "0xdef...",
    "block_height": 18500000
  }
}
```

#### 通知事件（各服务 → notification-service）
**Topic**: `notification.events.v1`

```json
{
  "event_id": "uuid",
  "event_type": "NOTIFICATION_REQUEST",
  "event_version": "v1",
  "timestamp": "2024-11-17T10:30:00Z",
  "source_service": "kyc-service",
  "correlation_id": "uuid",
  "payload": {
    "user_id": "user123",
    "notification_type": "EMAIL",
    "template_code": "KYC_APPROVED",
    "language": "zh_CN",
    "variables": {
      "username": "张三",
      "kyc_level": "L3"
    },
    "priority": "NORMAL"  // HIGH, NORMAL, LOW
  }
}
```

---

## 4. REST API 规范

### 4.1 URL 命名规范

- 使用复数名词：`/api/v1/orders`, `/api/v1/users`
- 资源嵌套不超过2层：`/api/v1/users/{userId}/accounts`
- 使用kebab-case：`/api/v1/user-profiles`

### 4.2 HTTP 方法映射

| 方法 | 语义 | 示例 |
|------|------|------|
| GET | 查询资源 | `GET /api/v1/orders/{orderId}` |
| POST | 创建资源 | `POST /api/v1/orders` |
| PUT | 完整更新资源 | `PUT /api/v1/users/{userId}` |
| PATCH | 部分更新资源 | `PATCH /api/v1/orders/{orderId}` |
| DELETE | 删除资源 | `DELETE /api/v1/orders/{orderId}` |

### 4.3 统一响应格式

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    // 业务数据
  },
  "timestamp": "2024-11-17T10:30:00Z",
  "request_id": "uuid"  // 用于追踪
}
```

错误响应：
```json
{
  "success": false,
  "code": "INSUFFICIENT_BALANCE",
  "message": "余额不足",
  "errors": [
    {
      "field": "amount",
      "message": "可用余额: 100 USDT, 需要: 500 USDT"
    }
  ],
  "timestamp": "2024-11-17T10:30:00Z",
  "request_id": "uuid"
}
```

---

## 5. 服务调用依赖图

```
order-gateway (Go)
  ├─> auth-service (gRPC) - Token验证
  ├─> account-service (gRPC) - 冻结资金
  ├─> risk-service (gRPC) - 风控检查
  └─> matching-engine (REST) - 提交订单

settlement-service (Java)
  ├─> account-service (gRPC) - 批量资金转移
  └─> Kafka (consumer) - 消费成交消息

withdraw-service (Java)
  ├─> account-service (gRPC) - 扣减余额
  ├─> risk-service (gRPC) - 风控检查
  ├─> wallet-service (gRPC) - 签名交易
  └─> Kafka (producer) - 发送提现完成事件

deposit-service (Java)
  ├─> wallet-service (gRPC) - 分配地址
  ├─> account-service (gRPC) - 充值入账
  └─> Kafka (producer) - 发送充值确认事件

notification-service (Java)
  └─> Kafka (consumer) - 消费通知事件

market-service (Java)
  ├─> matching-engine (WebSocket) - 订阅成交
  └─> Kafka (consumer) - 消费成交消息

gateway (Java)
  ├─> auth-service (gRPC) - Token验证
  └─> 各服务 (REST) - 路由转发
```

---

## 6. 接口契约测试

### 6.1 gRPC 契约测试

使用 **gRPC 官方测试框架**：

Java示例：
```java
@SpringBootTest
class AccountServiceGrpcTest {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub stub;

    @Test
    void testFreezeBalance() {
        FreezeBalanceRequest request = FreezeBalanceRequest.newBuilder()
            .setUserId("user123")
            .setCurrency("USDT")
            .setAmount("100.00")
            .setBusinessId("order_123")
            .setBusinessType("ORDER")
            .setIdempotentKey(UUID.randomUUID().toString())
            .build();

        FreezeBalanceResponse response = stub.freezeBalance(request);

        assertTrue(response.getSuccess());
        assertNotNull(response.getFreezeId());
    }
}
```

Go示例：
```go
func TestFreezeBalance(t *testing.T) {
    conn, _ := grpc.Dial("localhost:9001", grpc.WithInsecure())
    defer conn.Close()

    client := accountpb.NewAccountServiceClient(conn)

    req := &accountpb.FreezeBalanceRequest{
        UserId:        "user123",
        Currency:      "USDT",
        Amount:        "100.00",
        BusinessId:    "order_123",
        BusinessType:  "ORDER",
        IdempotentKey: uuid.New().String(),
    }

    resp, err := client.FreezeBalance(context.Background(), req)

    assert.NoError(t, err)
    assert.True(t, resp.Success)
    assert.NotEmpty(t, resp.FreezeId)
}
```

### 6.2 Kafka 契约测试

使用 **Embedded Kafka** 测试：

```java
@SpringBootTest
@EmbeddedKafka(topics = "trading.matching-engine.trades.v1")
class SettlementServiceKafkaTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void testConsumeTradeMessage() {
        String tradeMessage = """
            {
              "event_id": "uuid",
              "event_type": "TRADE_EXECUTED",
              "payload": {
                "trade_id": "trade123",
                "symbol": "BTC/USDT",
                "price": "50000.00",
                "quantity": "1.0"
              }
            }
        """;

        kafkaTemplate.send("trading.matching-engine.trades.v1", tradeMessage);

        // 等待消费
        await().atMost(5, SECONDS).until(() -> {
            // 验证settlement逻辑已执行
            return settlementRepository.existsByTradeId("trade123");
        });
    }
}
```

---

## 7. 开发流程

### 7.1 接口定义优先（Contract-First）

1. **Instance 1** 在 `/hkd-project-management/api-contracts/` 定义所有 Proto 文件
2. 各 Instance 拉取最新 Proto 文件生成代码
3. 严格按照 Proto 定义实现接口
4. 任何接口变更必须先更新 Proto，通知所有依赖方

### 7.2 代码生成

**Java**:
```bash
mvn clean compile  # 自动生成gRPC代码
```

**Go**:
```bash
protoc --go_out=. --go-grpc_out=. api/proto/*.proto
```

### 7.3 版本管理

- Proto文件版本：`v1`, `v2`（包名体现版本）
- 向后兼容：只能添加字段，不能删除或修改现有字段
- 破坏性变更：发布新版本（如 `v2`），并行运行一段时间

---

## 8. 工具和监控

### 8.1 gRPC 监控

- **gRPC Prometheus Interceptor**：收集调用次数、延迟、错误率
- **Grafana Dashboard**：可视化gRPC调用链路

### 8.2 Kafka 监控

- **Kafka Manager**：监控Topic、消费组、延迟
- **Lenses.io**：实时查看消息内容

### 8.3 契约文档

- **gRPC Reflection**：启用反射，允许grpcurl动态调用
- **Buf Schema Registry**：集中管理Proto文件

---

## 9. 常见问题

**Q1: 为什么核心业务用gRPC而不是REST？**
A: gRPC基于HTTP/2和Protobuf，性能比REST高3-5倍，类型安全，支持双向流。

**Q2: 如何保证gRPC调用的幂等性？**
A: 每个请求携带`idempotent_key`，服务端Redis去重。

**Q3: Kafka消息丢失怎么办？**
A:
- Producer：`acks=all` + 重试
- Broker：`min.insync.replicas=2`
- Consumer：手动提交offset（业务处理完再提交）

**Q4: 如何处理Proto文件的破坏性变更？**
A:
- 发布新版本（如`v2`）
- 新旧版本并行运行3个月
- 客户端逐步迁移到新版本

---

Generated with Claude Code
