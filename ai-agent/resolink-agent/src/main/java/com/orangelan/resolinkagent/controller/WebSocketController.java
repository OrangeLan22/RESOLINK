package com.orangelan.resolinkagent.controller;

import com.orangelan.resolinkagent.websocket.ChatWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ws")
public class WebSocketController {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketController(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /**
     * 获取WebSocket连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeConnections", chatWebSocketHandler.getActiveSessionCount());
        status.put("endpoint", "/ws/chat");
        status.put("protocol", "WebSocket");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    /**
     * 获取活跃会话信息
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        Map<String, Object> sessionsInfo = new HashMap<>();
        sessionsInfo.put("totalSessions", chatWebSocketHandler.getActiveSessionCount());
        sessionsInfo.put("sessionIds", chatWebSocketHandler.getActiveSessions().keySet());
        sessionsInfo.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(sessionsInfo);
    }
}