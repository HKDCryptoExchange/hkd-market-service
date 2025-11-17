package com.hkd.market.controller;

import com.hkd.market.websocket.server.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查Controller
 *
 * 提供服务健康状态检查接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final WebSocketServer webSocketServer;

    /**
     * 健康检查
     *
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "market-service");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * WebSocket服务器状态
     *
     * GET /api/v1/websocket/stats
     */
    @GetMapping("/websocket/stats")
    public ResponseEntity<Map<String, Object>> websocketStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("connections", webSocketServer.getConnectionCount());
        response.put("subscriptions", webSocketServer.getSubscriptionCount());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
