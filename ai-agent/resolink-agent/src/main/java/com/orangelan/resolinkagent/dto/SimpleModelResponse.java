package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 简化的模型响应DTO，只包含必要的信息
 */
public class SimpleModelResponse {
    
    @JsonProperty("type")
    private String type = "simple_model";
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("tool_calls")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToolCallResponse> toolCalls;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("is_new")
    private boolean isNew;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    public SimpleModelResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public SimpleModelResponse(String content, String sessionId, boolean isNew) {
        this();
        this.content = content;
        this.sessionId = sessionId;
        this.isNew = isNew;
    }
    
    public SimpleModelResponse(String content, String sessionId, boolean isNew, 
                           List<ToolCallResponse> toolCalls) {
        this(content, sessionId, isNew);
        this.toolCalls = toolCalls;
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<ToolCallResponse> getToolCalls() {
        return toolCalls;
    }
    
    public void setToolCalls(List<ToolCallResponse> toolCalls) {
        this.toolCalls = toolCalls;
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
    
    public void setNew(boolean aNew) {
        isNew = aNew;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}