package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 模型状态响应DTO，只告诉用户模型当前在做什么
 */
public class ModelStatusResponse {
    
    @JsonProperty("type")
    private String type = "model_status";
    
    @JsonProperty("status")
    private StatusType status;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    /**
     * 状态类型枚举
     */
    public enum StatusType {
        THINKING("正在思考问题"),
        PARSING_TIME("正在解析时间"),
        ADDING_APPOINTMENT("正在添加预约记录"),
        ALLOCATING_RESOURCES("正在分配资源"),
        QUERYING_RESOURCES("正在查询资源"),
        CHECKING_AVAILABILITY("正在检查资源可用性"),
        FINALIZING("正在生成最终回复");
        
        private final String description;
        
        StatusType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public ModelStatusResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ModelStatusResponse(StatusType status, String sessionId) {
        this();
        this.status = status;
        this.sessionId = sessionId;
        this.description = status.getDescription();
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public StatusType getStatus() {
        return status;
    }
    
    public void setStatus(StatusType status) {
        this.status = status;
        this.description = status.getDescription();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
}