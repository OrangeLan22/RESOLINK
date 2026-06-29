package com.orangelan.resolinkagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 模型步骤响应DTO，表示每次模型返回的中间结果
 */
public class ModelStepResponse {
    
    @JsonProperty("type")
    private String type = "model_step";
    
    @JsonProperty("step_type")
    private StepType stepType;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    @JsonProperty("tool_calls")
    private List<ToolCallResponse> toolCalls;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("step_index")
    private int stepIndex;
    
    @JsonProperty("total_steps")
    private int totalSteps;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("is_final")
    private boolean isFinal;
    
    /**
     * 步骤类型枚举
     */
    public enum StepType {
        THINKING,      // 思考过程
        TOOL_CALL,     // 工具调用
        FINAL_RESPONSE // 最终响应
    }
    
    public ModelStepResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ModelStepResponse(StepType stepType, String sessionId, int stepIndex, int totalSteps) {
        this();
        this.stepType = stepType;
        this.sessionId = sessionId;
        this.stepIndex = stepIndex;
        this.totalSteps = totalSteps;
        this.isFinal = (stepIndex == totalSteps);
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public StepType getStepType() {
        return stepType;
    }
    
    public void setStepType(StepType stepType) {
        this.stepType = stepType;
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
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public int getStepIndex() {
        return stepIndex;
    }
    
    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }
    
    public int getTotalSteps() {
        return totalSteps;
    }
    
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}