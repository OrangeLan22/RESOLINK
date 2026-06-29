package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 流式响应DTO，用于实时推送每轮思考结果
 */
public class StreamingResponse {
    
    @JsonProperty("type")
    private String type = "streaming";
    
    @JsonProperty("chunk_type")
    private String chunkType;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    @JsonProperty("tool_call")
    private ToolCallInfo toolCall;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("is_finished")
    private boolean isFinished;
    
    @JsonProperty("finish_reason")
    private String finishReason;
    
    public StreamingResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public StreamingResponse(String chunkType, String sessionId) {
        this();
        this.chunkType = chunkType;
        this.sessionId = sessionId;
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getChunkType() {
        return chunkType;
    }
    
    public void setChunkType(String chunkType) {
        this.chunkType = chunkType;
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
    
    public ToolCallInfo getToolCall() {
        return toolCall;
    }
    
    public void setToolCall(ToolCallInfo toolCall) {
        this.toolCall = toolCall;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isFinished() {
        return isFinished;
    }
    
    public void setFinished(boolean finished) {
        isFinished = finished;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
    
    /**
     * 工具调用信息
     */
    public static class ToolCallInfo {
        @JsonProperty("tool_type")
        private String toolType;
        
        @JsonProperty("tool_name")
        private String toolName;
        
        @JsonProperty("tool_description")
        private String toolDescription;
        
        @JsonProperty("arguments")
        private String arguments;
        
        // Getter和Setter方法
        public String getToolType() {
            return toolType;
        }
        
        public void setToolType(String toolType) {
            this.toolType = toolType;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
        
        public String getToolDescription() {
            return toolDescription;
        }
        
        public void setToolDescription(String toolDescription) {
            this.toolDescription = toolDescription;
        }
        
        public String getArguments() {
            return arguments;
        }
        
        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}