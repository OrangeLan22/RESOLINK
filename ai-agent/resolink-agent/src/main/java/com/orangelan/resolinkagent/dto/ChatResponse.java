package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 聊天响应DTO
 */
public class ChatResponse {
    
    @JsonProperty("response")
    private String response;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("is_new")
    private boolean isNew;
    
    public ChatResponse() {}
    
    public ChatResponse(String response, String sessionId, boolean isNew) {
        this.response = response;
        this.sessionId = sessionId;
        this.isNew = isNew;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}