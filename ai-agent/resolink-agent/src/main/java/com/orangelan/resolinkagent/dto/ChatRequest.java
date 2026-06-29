package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 聊天请求DTO
 */
public class ChatRequest {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("token")
    private String token;
    
    public ChatRequest() {}
    
    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}