package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orangelan.resolinkagent.dto.ChatRequest;
import com.orangelan.resolinkagent.dto.ModelStatusResponse;
import com.orangelan.resolinkagent.dto.SimpleModelResponse;
import com.orangelan.resolinkagent.dto.ToolCallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 流式聊天服务，实现真正的实时响应
 */
@Service
public class StreamingChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingChatService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SessionService sessionService;
    
    @Value("${deepseek.api.key}")
    private String deepseekApiKey;
    
    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;
    
    /**
     * 处理消息请求，实现流式响应
     * @param chatRequest 聊天请求
     * @param streamCallback 流式回调函数，用于实时推送每轮思考结果
     * @return 最终响应
     */
    public SimpleModelResponse processMessageWithStreaming(ChatRequest chatRequest, 
                                                     Consumer<StreamingChunk> streamCallback) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 构建请求体
            ObjectNode requestBody = buildStreamingRequestBody(chatRequest, actualSessionId, isNew);
            
            // 调用DeepSeek流式API
            List<StreamingChunk> chunks = callDeepSeekStreamingAPI(requestBody, streamCallback);
            
            // 保存完整的对话记录
            saveConversationHistory(chunks, actualSessionId);
            
            // 构建最终响应
            SimpleModelResponse response = buildFinalResponse(chunks, actualSessionId, isNew);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理流式消息失败", e);
            throw new RuntimeException("Failed to process message with streaming", e);
        }
    }
    
    /**
     * 构建流式请求体
     */
    private ObjectNode buildStreamingRequestBody(ChatRequest chatRequest, String actualSessionId, boolean isNew) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        // 设置基本参数
        requestBody.put("model", "deepseek-chat");
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 4000);
        requestBody.put("temperature", 0.7);
        
        // 构建消息数组
        ArrayNode messages = objectMapper.createArrayNode();
        
        // 添加系统提示词
        String processedSystemPrompt = processSystemPromptWithUserAccount(chatRequest.getToken());
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", processedSystemPrompt);
        messages.add(systemMessage);
        
        // 添加历史消息
        if (!isNew) {
            List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
            for (Map<String, Object> msg : history) {
                if (!"system".equals(msg.get("role"))) {
                    ObjectNode historyMessage = objectMapper.valueToTree(msg);
                    messages.add(historyMessage);
                }
            }
        }
        
        // 添加当前用户消息
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", chatRequest.getMessage());
        messages.add(userMessage);
        
        requestBody.set("messages", messages);
        
        // 添加工具定义
        requestBody.set("tools", buildToolsDefinition());
        
        return requestBody;
    }
    
    /**
     * 调用DeepSeek流式API
     */
    private List<StreamingChunk> callDeepSeekStreamingAPI(ObjectNode requestBody, 
                                                      Consumer<StreamingChunk> streamCallback) throws IOException {
        List<StreamingChunk> chunks = new ArrayList<>();
        
        URL url = new URL(deepseekApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求属性
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + deepseekApiKey);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);
            
            // 发送请求
            connection.getOutputStream().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            
            // 读取流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                StringBuilder contentBuffer = new StringBuilder();
                StringBuilder reasoningBuffer = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if (data.equals("[DONE]")) {
                            break;
                        }
                        
                        try {
                            JsonNode chunkData = objectMapper.readTree(data);
                            JsonNode choices = chunkData.get("choices");
                            
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode choice = choices.get(0);
                                JsonNode delta = choice.get("delta");
                                
                                if (delta != null) {
                                    StreamingChunk chunk = new StreamingChunk();
                                    
                                    // 处理内容
                                    if (delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        if (content != null && !content.isEmpty()) {
                                            contentBuffer.append(content);
                                            chunk.setContent(content);
                                            chunk.setChunkType(StreamingChunk.ChunkType.CONTENT);
                                        }
                                    }
                                    
                                    // 处理推理内容
                                    if (delta.has("reasoning_content")) {
                                        String reasoningContent = delta.get("reasoning_content").asText();
                                        if (reasoningContent != null && !reasoningContent.isEmpty()) {
                                            reasoningBuffer.append(reasoningContent);
                                            chunk.setReasoningContent(reasoningContent);
                                            chunk.setChunkType(StreamingChunk.ChunkType.REASONING);
                                        }
                                    }
                                    
                                    // 处理工具调用
                                    if (delta.has("tool_calls")) {
                                        JsonNode toolCalls = delta.get("tool_calls");
                                        if (toolCalls != null && toolCalls.isArray()) {
                                            for (JsonNode toolCall : toolCalls) {
                                                StreamingChunk.ToolCallInfo toolCallInfo = new StreamingChunk.ToolCallInfo();
                                                
                                                if (toolCall.has("index")) {
                                                    toolCallInfo.setIndex(toolCall.get("index").asInt());
                                                }
                                                
                                                if (toolCall.has("id")) {
                                                    toolCallInfo.setId(toolCall.get("id").asText());
                                                }
                                                
                                                JsonNode function = toolCall.get("function");
                                                if (function != null) {
                                                    if (function.has("name")) {
                                                        toolCallInfo.setFunctionName(function.get("name").asText());
                                                    }
                                                    
                                                    if (function.has("arguments")) {
                                                        toolCallInfo.setArguments(function.get("arguments").asText());
                                                    }
                                                }
                                                
                                                chunk.addToolCall(toolCallInfo);
                                                chunk.setChunkType(StreamingChunk.ChunkType.TOOL_CALL);
                                            }
                                        }
                                    }
                                    
                                    // 处理完成状态
                                    if (choice.has("finish_reason")) {
                                        String finishReason = choice.get("finish_reason").asText();
                                        chunk.setFinishReason(finishReason);
                                        chunk.setChunkType(StreamingChunk.ChunkType.FINISH);
                                    }
                                    
                                    // 添加到列表并回调
                                    if (chunk.getChunkType() != null) {
                                        chunks.add(chunk);
                                        if (streamCallback != null) {
                                            streamCallback.accept(chunk);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("解析流式数据块失败: " + data, e);
                        }
                    }
                }
            }
            
        } finally {
            connection.disconnect();
        }
        
        return chunks;
    }
    
    /**
     * 构建工具定义
     */
    private ArrayNode buildToolsDefinition() {
        ArrayNode tools = objectMapper.createArrayNode();
        
        // 时间工具
        ObjectNode timeTool = objectMapper.createObjectNode();
        timeTool.put("type", "function");
        ObjectNode timeFunction = objectMapper.createObjectNode();
        timeFunction.put("name", "get_current_time");
        timeFunction.put("description", "获取当前时间");
        timeTool.set("function", timeFunction);
        tools.add(timeTool);
        
        // 添加预约工具
        ObjectNode appointmentTool = objectMapper.createObjectNode();
        appointmentTool.put("type", "function");
        ObjectNode appointmentFunction = objectMapper.createObjectNode();
        appointmentFunction.put("name", "add_appointment");
        appointmentFunction.put("description", "添加预约记录");
        ObjectNode appointmentParameters = objectMapper.createObjectNode();
        appointmentParameters.put("type", "object");
        ObjectNode appointmentProperties = objectMapper.createObjectNode();
        appointmentProperties.put("title", objectMapper.createObjectNode().put("type", "string"));
        appointmentProperties.put("date", objectMapper.createObjectNode().put("type", "string"));
        appointmentProperties.put("time", objectMapper.createObjectNode().put("type", "string"));
        appointmentParameters.set("properties", appointmentProperties);
        appointmentParameters.set("required", objectMapper.createArrayNode().add("title").add("date").add("time"));
        appointmentFunction.set("parameters", appointmentParameters);
        appointmentTool.set("function", appointmentFunction);
        tools.add(appointmentTool);
        
        // 查询资源工具
        ObjectNode resourceTool = objectMapper.createObjectNode();
        resourceTool.put("type", "function");
        ObjectNode resourceFunction = objectMapper.createObjectNode();
        resourceFunction.put("name", "query_resources");
        resourceFunction.put("description", "查询资源");
        ObjectNode resourceParameters = objectMapper.createObjectNode();
        resourceParameters.put("type", "object");
        ObjectNode resourceProperties = objectMapper.createObjectNode();
        resourceProperties.put("date", objectMapper.createObjectNode().put("type", "string"));
        resourceParameters.set("properties", resourceProperties);
        resourceParameters.set("required", objectMapper.createArrayNode().add("date"));
        resourceFunction.set("parameters", resourceParameters);
        resourceTool.set("function", resourceFunction);
        tools.add(resourceTool);
        
        // 检查可用性工具
        ObjectNode availabilityTool = objectMapper.createObjectNode();
        availabilityTool.put("type", "function");
        ObjectNode availabilityFunction = objectMapper.createObjectNode();
        availabilityFunction.put("name", "check_availability");
        availabilityFunction.put("description", "检查资源可用性");
        ObjectNode availabilityParameters = objectMapper.createObjectNode();
        availabilityParameters.put("type", "object");
        ObjectNode availabilityProperties = objectMapper.createObjectNode();
        availabilityProperties.put("resource_id", objectMapper.createObjectNode().put("type", "string"));
        availabilityProperties.put("date", objectMapper.createObjectNode().put("type", "string"));
        availabilityProperties.put("time", objectMapper.createObjectNode().put("type", "string"));
        availabilityParameters.set("properties", availabilityProperties);
        availabilityParameters.set("required", objectMapper.createArrayNode().add("resource_id").add("date").add("time"));
        availabilityFunction.set("parameters", availabilityParameters);
        availabilityTool.set("function", availabilityFunction);
        tools.add(availabilityTool);
        
        // 分配资源工具
        ObjectNode allocationTool = objectMapper.createObjectNode();
        allocationTool.put("type", "function");
        ObjectNode allocationFunction = objectMapper.createObjectNode();
        allocationFunction.put("name", "query_reservation_count");
        allocationFunction.put("description", "分配资源");
        ObjectNode allocationParameters = objectMapper.createObjectNode();
        allocationParameters.put("type", "object");
        ObjectNode allocationProperties = objectMapper.createObjectNode();
        allocationProperties.put("date", objectMapper.createObjectNode().put("type", "string"));
        allocationProperties.put("resource_type", objectMapper.createObjectNode().put("type", "string"));
        allocationParameters.set("properties", allocationProperties);
        allocationParameters.set("required", objectMapper.createArrayNode().add("date").add("resource_type"));
        allocationFunction.set("parameters", allocationParameters);
        allocationTool.set("function", allocationFunction);
        tools.add(allocationTool);
        
        return tools;
    }
    
    /**
     * 保存对话历史
     */
    private void saveConversationHistory(List<StreamingChunk> chunks, String sessionId) {
        // 这里需要将chunks转换为对话格式并保存
        // 实现细节取决于具体需求
        logger.info("保存对话历史，会话ID: {}, 数据块数量: {}", sessionId, chunks.size());
    }
    
    /**
     * 构建最终响应
     */
    private SimpleModelResponse buildFinalResponse(List<StreamingChunk> chunks, String sessionId, boolean isNew) {
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        List<ToolCallResponse> toolCalls = new ArrayList<>();
        
        for (StreamingChunk chunk : chunks) {
            if (chunk.getContent() != null) {
                contentBuilder.append(chunk.getContent());
            }
            
            if (chunk.getReasoningContent() != null) {
                reasoningBuilder.append(chunk.getReasoningContent());
            }
            
            if (chunk.getToolCalls() != null) {
                for (StreamingChunk.ToolCallInfo toolCallInfo : chunk.getToolCalls()) {
                    // 这里需要处理工具调用信息
                    // 实现细节取决于具体需求
                }
            }
        }
        
        return new SimpleModelResponse(contentBuilder.toString(), sessionId, isNew);
    }
    
    /**
     * 处理系统提示词，替换用户账号
     */
    private String processSystemPromptWithUserAccount(String token) {
        // 这里实现token验证和用户账号替换逻辑
        // 简化实现，返回默认系统提示词
        return "你是一个智能助手，可以帮助用户处理各种任务。";
    }
    
    /**
     * 流式数据块
     */
    public static class StreamingChunk {
        private ChunkType chunkType;
        private String content;
        private String reasoningContent;
        private List<ToolCallInfo> toolCalls;
        private String finishReason;
        
        public enum ChunkType {
            CONTENT,      // 内容
            REASONING,    // 推理内容
            TOOL_CALL,    // 工具调用
            FINISH        // 完成
        }
        
        public StreamingChunk() {
            this.toolCalls = new ArrayList<>();
        }
        
        // Getter和Setter方法
        public ChunkType getChunkType() {
            return chunkType;
        }
        
        public void setChunkType(ChunkType chunkType) {
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
        
        public List<ToolCallInfo> getToolCalls() {
            return toolCalls;
        }
        
        public void setToolCalls(List<ToolCallInfo> toolCalls) {
            this.toolCalls = toolCalls;
        }
        
        public void addToolCall(ToolCallInfo toolCall) {
            this.toolCalls.add(toolCall);
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
            private int index;
            private String id;
            private String functionName;
            private String arguments;
            
            // Getter和Setter方法
            public int getIndex() {
                return index;
            }
            
            public void setIndex(int index) {
                this.index = index;
            }
            
            public String getId() {
                return id;
            }
            
            public void setId(String id) {
                this.id = id;
            }
            
            public String getFunctionName() {
                return functionName;
            }
            
            public void setFunctionName(String functionName) {
                this.functionName = functionName;
            }
            
            public String getArguments() {
                return arguments;
            }
            
            public void setArguments(String arguments) {
                this.arguments = arguments;
            }
        }
    }
}