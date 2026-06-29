package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 包含工具调用的聊天响应DTO
 */
public class ChatResponseWithTools {
    
    @JsonProperty("type")
    private String type = "chat"; // 默认类型为chat
    
    @JsonProperty("response")
    private String response;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("is_new")
    private boolean isNew;
    
    @JsonProperty("tool_calls")
    private List<ToolCallResponse> toolCalls;
    
    @JsonProperty("has_tools")
    private boolean hasTools;
    
    public ChatResponseWithTools() {}
    
    public ChatResponseWithTools(String response, String sessionId, boolean isNew) {
        this.response = response;
        this.sessionId = sessionId;
        this.isNew = isNew;
        this.hasTools = false;
    }
    
    public ChatResponseWithTools(String response, String sessionId, boolean isNew, 
                                List<ToolCallResponse> toolCalls) {
        this.response = response;
        this.sessionId = sessionId;
        this.isNew = isNew;
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
    
    public void setNew(boolean aNew) {
        isNew = aNew;
    }
    
    public List<ToolCallResponse> getToolCalls() {
        return toolCalls;
    }
    
    public void setToolCalls(List<ToolCallResponse> toolCalls) {
        this.toolCalls = toolCalls;
        this.hasTools = toolCalls != null && !toolCalls.isEmpty();
    }
    
    public boolean isHasTools() {
        return hasTools;
    }
    
    public void setHasTools(boolean hasTools) {
        this.hasTools = hasTools;
    }
}