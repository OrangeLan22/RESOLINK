package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 完整模型响应DTO，包含所有模型返回的数据
 */
public class FullModelResponse {
    
    @JsonProperty("type")
    private String type = "full_model";
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    @JsonProperty("tool_calls")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToolCallResponse> toolCalls;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("is_new")
    private boolean isNew;
    
    @JsonProperty("has_tools")
    private boolean hasTools;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    public FullModelResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public FullModelResponse(String content, String reasoningContent, String sessionId, boolean isNew) {
        this();
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.sessionId = sessionId;
        this.isNew = isNew;
        this.hasTools = false;
    }
    
    public FullModelResponse(String content, String reasoningContent, String sessionId, boolean isNew, 
                           List<ToolCallResponse> toolCalls) {
        this(content, reasoningContent, sessionId, isNew);
        this.toolCalls = toolCalls;
        this.hasTools = toolCalls != null && !toolCalls.isEmpty();
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
    
    public String getReasoningContent() {
        return reasoningContent;
    }
    
    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }
    
    public List<ToolCallResponse> getToolCalls() {
        return toolCalls;
    }
    
    public void setToolCalls(List<ToolCallResponse> toolCalls) {
        this.toolCalls = toolCalls;
        this.hasTools = toolCalls != null && !toolCalls.isEmpty();
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
    
    public boolean isHasTools() {
        return hasTools;
    }
    
    public void setHasTools(boolean hasTools) {
        this.hasTools = hasTools;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}