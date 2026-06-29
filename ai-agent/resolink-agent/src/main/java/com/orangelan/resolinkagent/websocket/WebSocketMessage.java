package com.orangelan.resolinkagent.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket消息模型
 */
public class WebSocketMessage {
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        CHAT,      // 聊天消息
        CONNECT,   // 连接消息
        DISCONNECT, // 断开连接
        ERROR,     // 错误消息
        TOOLS,     // 工具调用消息
        FULL_MODEL, // 完整模型响应消息
        MODEL_STEP, // 模型步骤消息
        MODEL_STATUS, // 模型状态消息
        SIMPLE_MODEL, // 简化模型响应消息
        STREAMING  // 流式响应消息
    }
    
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("content")
    private Object content;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public WebSocketMessage(MessageType type, Object content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    public WebSocketMessage(MessageType type, Object content, String sessionId) {
        this.type = type;
        this.content = content;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getter和Setter方法
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public Object getContent() {
        return content;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}