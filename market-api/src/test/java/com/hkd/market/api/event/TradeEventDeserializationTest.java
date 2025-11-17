package com.hkd.market.api.event;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TradeExecutedEvent JSON反序列化测试
 *
 * 验证Kafka消息格式是否正确解析
 */
class TradeEventDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 配置
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void testDeserializeTradeExecutedEvent() throws Exception {
        String json = """
            {
              "event_id": "550e8400-e29b-41d4-a716-446655440000",
              "event_type": "TRADE_EXECUTED",
              "event_version": "v1",
              "timestamp": "2024-11-17T10:30:00Z",
              "source_service": "matching-engine",
              "correlation_id": "test-001",
              "payload": {
                "trade_id": "trade_001",
                "symbol": "BTC/USDT",
                "price": "50000.00",
                "quantity": "0.1",
                "buyer_order_id": "order_buy_001",
                "seller_order_id": "order_sell_001",
                "buyer_user_id": "user123",
                "seller_user_id": "user456",
                "buyer_fee": "0.0001",
                "seller_fee": "5.00",
                "maker_side": "SELL",
                "timestamp": "2024-11-17T10:30:00.123Z"
              }
            }
        """;

        // 反序列化
        TradeExecutedEvent event = objectMapper.readValue(json, TradeExecutedEvent.class);

        // 验证事件元数据
        assertNotNull(event);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", event.getEventId());
        assertEquals("TRADE_EXECUTED", event.getEventType());
        assertEquals("v1", event.getEventVersion());
        assertEquals("matching-engine", event.getSourceService());
        assertEquals("test-001", event.getCorrelationId());
        assertNotNull(event.getTimestamp());

        // 验证载荷
        TradeExecutedEvent.TradePayload payload = event.getPayload();
        assertNotNull(payload);
        assertEquals("trade_001", payload.getTradeId());
        assertEquals("BTC/USDT", payload.getSymbol());
        assertEquals("50000.00", payload.getPrice());
        assertEquals("0.1", payload.getQuantity());
        assertEquals("order_buy_001", payload.getBuyerOrderId());
        assertEquals("order_sell_001", payload.getSellerOrderId());
        assertEquals("user123", payload.getBuyerUserId());
        assertEquals("user456", payload.getSellerUserId());
        assertEquals("0.0001", payload.getBuyerFee());
        assertEquals("5.00", payload.getSellerFee());
        assertEquals("SELL", payload.getMakerSide());
        assertNotNull(payload.getTimestamp());

        System.out.println("✅ JSON反序列化测试通过！");
        System.out.println("   Event ID: " + event.getEventId());
        System.out.println("   Symbol: " + payload.getSymbol());
        System.out.println("   Price: " + payload.getPrice());
        System.out.println("   Quantity: " + payload.getQuantity());
    }

    @Test
    void testSerializeAndDeserialize() throws Exception {
        // 创建事件对象
        TradeExecutedEvent.TradePayload payload = TradeExecutedEvent.TradePayload.builder()
                .tradeId("trade_002")
                .symbol("ETH/USDT")
                .price("3000.50")
                .quantity("1.5")
                .buyerOrderId("order_buy_002")
                .sellerOrderId("order_sell_002")
                .buyerUserId("user789")
                .sellerUserId("user456")
                .buyerFee("0.0015")
                .sellerFee("4.50")
                .makerSide("BUY")
                .build();

        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .eventId("test-event-123")
                .eventType("TRADE_EXECUTED")
                .eventVersion("v1")
                .sourceService("matching-engine")
                .correlationId("test-corr-123")
                .payload(payload)
                .build();

        // 序列化
        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("ETH/USDT"));
        assertTrue(json.contains("3000.50"));

        // 反序列化
        TradeExecutedEvent deserialized = objectMapper.readValue(json, TradeExecutedEvent.class);
        assertEquals("test-event-123", deserialized.getEventId());
        assertEquals("ETH/USDT", deserialized.getPayload().getSymbol());
        assertEquals("3000.50", deserialized.getPayload().getPrice());

        System.out.println("✅ 序列化/反序列化往返测试通过！");
    }

    @Test
    void testMissingOptionalFields() throws Exception {
        // 测试缺少可选字段时的反序列化
        String json = """
            {
              "event_id": "test-id",
              "event_type": "TRADE_EXECUTED",
              "event_version": "v1",
              "timestamp": "2024-11-17T10:30:00Z",
              "source_service": "matching-engine",
              "payload": {
                "trade_id": "trade_003",
                "symbol": "BTC/USDT",
                "price": "50000.00",
                "quantity": "0.1",
                "buyer_order_id": "order_buy",
                "seller_order_id": "order_sell",
                "buyer_user_id": "user1",
                "seller_user_id": "user2",
                "maker_side": "BUY",
                "timestamp": "2024-11-17T10:30:00.123Z"
              }
            }
        """;

        TradeExecutedEvent event = objectMapper.readValue(json, TradeExecutedEvent.class);
        assertNotNull(event);
        assertEquals("test-id", event.getEventId());
        assertNull(event.getCorrelationId()); // 可选字段

        System.out.println("✅ 缺少可选字段测试通过！");
    }
}
