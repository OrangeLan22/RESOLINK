package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 工具调用响应DTO
 */
public class ToolCallResponse {
    
    /**
     * 工具类型枚举
     */
    public enum ToolType {
        TIMETOOL("解析时间"),
        ADDAPPOINTMENTTOOL("添加预约记录"),
        APPOINTMENTCOUNTTOOL("分配资源"),
        RESOURCEQUERYTOOL("查询资源"),
        AVAILABILITYCHECKTOOL("检查资源可用性");
        
        private final String description;
        
        ToolType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @JsonProperty("type")
    private String type = "tools";
    
    @JsonProperty("tool_type")
    private ToolType toolType;
    
    @JsonProperty("tool_name")
    private String toolName;
    
    @JsonProperty("tool_description")
    private String toolDescription;
    
    @JsonProperty("arguments")
    private Map<String, Object> arguments;
    
    @JsonProperty("result")
    private String result;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("is_success")
    private boolean isSuccess;
    
    public ToolCallResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ToolCallResponse(ToolType toolType, String toolName, Map<String, Object> arguments, 
                           String result, String sessionId, boolean isSuccess) {
        this();
        this.toolType = toolType;
        this.toolName = toolName;
        this.toolDescription = toolType.getDescription();
        this.arguments = arguments;
        this.result = result;
        this.sessionId = sessionId;
        this.isSuccess = isSuccess;
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public ToolType getToolType() {
        return toolType;
    }
    
    public void setToolType(ToolType toolType) {
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
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
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
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}