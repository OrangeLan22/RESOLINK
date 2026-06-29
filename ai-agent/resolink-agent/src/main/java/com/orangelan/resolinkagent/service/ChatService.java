package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.dto.ChatRequest;
import com.orangelan.resolinkagent.dto.ChatResponse;
import com.orangelan.resolinkagent.dto.ToolCallResponse;
import com.orangelan.resolinkagent.dto.ChatResponseWithTools;
import com.orangelan.resolinkagent.dto.FullModelResponse;
import com.orangelan.resolinkagent.dto.ModelStepResponse;
import com.orangelan.resolinkagent.dto.ModelStatusResponse;
import com.orangelan.resolinkagent.dto.SimpleModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final WebClient webClient;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${deepseek.api.model}")
    private String deepseekApiModel;

    @Value("${deepseek.api.max-tokens}")
    private int deepseekApiMaxTokens;

    @Value("${deepseek.api.temperature}")
    private double deepseekApiTemperature;

    @Autowired
    private TimeToolService timeToolService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private ResourceQueryToolService resourceQueryToolService;
    
    @Autowired
    private AppointmentCountToolService appointmentCountToolService;
    
    @Autowired
    private AvailabilityCheckToolService availabilityCheckToolService;
    
    @Autowired
    private AddAppointmentToolService addAppointmentToolService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String systemPrompt;

    @Autowired
    public ChatService(WebClient webClient) {
        logger.info("初始化ChatService，使用注入的WebClient");
        
        // 添加请求和响应日志过滤器
        this.webClient = webClient.mutate()
                .filter(logRequest())
                .filter(logResponse())
                .build();
                
        // 加载系统提示词
        loadSystemPrompt();
    }
    
    /**
     * 加载系统提示词
     */
    private void loadSystemPrompt() {
        try {
            Resource resource = new ClassPathResource("system-prompt.md");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                systemPrompt = FileCopyUtils.copyToString(reader);
                logger.info("成功加载系统提示词");
            }
        } catch (IOException e) {
            logger.error("加载系统提示词失败，使用默认提示词", e);
            systemPrompt = "你是RESOLINK智能体，你需要根据用户的问题和上下文，提供准确的回答。";
        }
    }
    
    /**
     * 处理系统提示词，替换用户账号
     * @param token JWT令牌
     * @return 处理后的系统提示词
     */
    private String processSystemPromptWithUserAccount(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("未提供token，使用原始系统提示词");
            return systemPrompt.replace("{userAccount}", "未知用户");
        }
        
        try {
            // 这里应该解析JWT token获取sub字段
            // 简化实现，实际应该使用JWT库解析
            String userAccount = extractUserAccountFromToken(token);
            if (userAccount != null) {
                logger.info("成功从token中提取用户账号: {}", userAccount);
                return systemPrompt.replace("{userAccount}", userAccount);
            } else {
                logger.warn("无法从token中提取用户账号，使用默认值");
                return systemPrompt.replace("{userAccount}", "未知用户");
            }
        } catch (Exception e) {
            logger.error("处理token时出错，使用默认用户账号", e);
            return systemPrompt.replace("{userAccount}", "未知用户");
        }
    }
    
    /**
     * 从JWT token中提取用户账号（sub字段）
     * @param token JWT令牌
     * @return 用户账号
     */
    private String extractUserAccountFromToken(String token) {
        try {
            // 简化实现，实际应该使用JWT库解析
            // 这里假设token格式为: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                logger.warn("token格式不正确");
                return null;
            }
            
            // 解码payload部分
            String payload = parts[1];
            // 补全Base64填充
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            String decodedPayload = new String(java.util.Base64.getDecoder().decode(payload));
            logger.debug("解码后的payload: {}", decodedPayload);
            
            // 使用简单的JSON解析提取sub字段
            // 实际应该使用Jackson等JSON库
            int subIndex = decodedPayload.indexOf("\"sub\":");
            if (subIndex != -1) {
                int startQuote = decodedPayload.indexOf("\"", subIndex + 6);
                int endQuote = decodedPayload.indexOf("\"", startQuote + 1);
                if (startQuote != -1 && endQuote != -1) {
                    return decodedPayload.substring(startQuote + 1, endQuote);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("解析token失败", e);
            return null;
        }
    }

    public String processMessage(String message) {
        try {
            // 构建请求体，包含工具定义
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型，并启用DSML格式支持
            requestBody.put("model", "deepseek-reasoner");
            
            // 开启思考模式，启用DSML格式
            Map<String, Object> thinkingConfig = new HashMap<>();
            thinkingConfig.put("type", "enabled");
            thinkingConfig.put("dsml", true); // 启用DSML格式支持
            requestBody.put("thinking", thinkingConfig);
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            
            // 添加消息
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            requestBody.put("messages", messages);
            
            // 添加工具定义
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送到DeepSeek API的请求: {}", requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的响应: {}", result);
            
            // 解析响应，检查是否有工具调用
            List<Map<String, Object>> completeConversation = processResponseWithToolsAndReturnCompleteConversation(result, messages);
            
            // 提取最终助手响应
            return extractFinalAssistantResponse(completeConversation);
            
        } catch (Exception e) {
            logger.error("调用DeepSeek API失败", e);
            throw new RuntimeException("Failed to process message with DeepSeek API", e);
        }
    }
    
    /**
     * 处理带会话ID的消息请求
     * @param chatRequest 聊天请求
     * @return 聊天响应，包含会话ID和是否为新会话标志
     */
    public ChatResponse processMessageWithSession(ChatRequest chatRequest) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCalls(messages);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 提取最终助手响应
            String finalAssistantResponse = extractFinalAssistantResponse(completeConversation);
            
            // 返回响应
            return new ChatResponse(finalAssistantResponse, actualSessionId, isNew);
            
        } catch (Exception e) {
            logger.error("处理带会话的消息请求失败", e);
            throw new RuntimeException("Failed to process message with session", e);
        }
    }
    
    /**
     * 调用DeepSeek API
     * @param messages 消息历史
     * @return 助手响应
     */
    private String callDeepSeekAPI(List<Map<String, Object>> messages) {
        try {
            // 构建请求体，包含工具定义
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型（不需要额外的thinking参数）
            requestBody.put("model", "deepseek-reasoner");
            
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            requestBody.put("messages", messages);
            
            // 添加工具定义
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送到DeepSeek API的请求: {}", requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的响应: {}", result);
            
            // 解析响应，检查是否有工具调用
            List<Map<String, Object>> completeConversation = processResponseWithToolsAndReturnCompleteConversation(result, messages);
            
            // 提取最终助手响应
            return extractFinalAssistantResponse(completeConversation);
            
        } catch (Exception e) {
            logger.error("调用DeepSeek API失败", e);
            throw new RuntimeException("Failed to call DeepSeek API", e);
        }
    }
    
    /**
     * 调用DeepSeek API，返回包含工具调用的完整对话
     * @param messages 消息历史
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> callDeepSeekAPIWithToolCalls(List<Map<String, Object>> messages) {
        try {
            // 构建请求体，包含工具定义
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型（不需要额外的thinking参数）
            requestBody.put("model", "deepseek-reasoner");
            
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            requestBody.put("messages", messages);
            
            // 添加工具定义
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送到DeepSeek API的请求: {}", requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的响应: {}", result);
            
            // 解析响应，检查是否有工具调用
            return processResponseWithToolsAndReturnCompleteConversation(result, messages);
            
        } catch (Exception e) {
            logger.error("调用DeepSeek API失败", e);
            throw new RuntimeException("Failed to call DeepSeek API", e);
        }
    }
    
    /**
     * 调用DeepSeek API，返回包含工具调用的完整对话，并实时推送模型状态
     * @param messages 消息历史
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> callDeepSeekAPIWithToolCallsAndStatusUpdates(List<Map<String, Object>> messages, 
                                                                                  String sessionId,
                                                                                  java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        try {
            // 构建请求体，包含工具定义
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型（不需要额外的thinking参数）
            requestBody.put("model", "deepseek-reasoner");
            
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            requestBody.put("messages", messages);
            
            // 添加工具定义
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送到DeepSeek API的请求: {}", requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的响应: {}", result);
            
            // 解析响应，检查是否有工具调用，并实时推送状态
            return processResponseWithToolsAndStatusUpdates(result, messages, sessionId, statusCallback);
            
        } catch (Exception e) {
            logger.error("调用DeepSeek API失败", e);
            throw new RuntimeException("Failed to call DeepSeek API", e);
        }
    }
    
    /**
     * 从完整对话中提取最终的助手响应
     * @param completeConversation 完整对话
     * @return 最终助手响应
     */
    private String extractFinalAssistantResponse(List<Map<String, Object>> completeConversation) {
        // 从后往前遍历，找到最后一个助手响应
        for (int i = completeConversation.size() - 1; i >= 0; i--) {
            Map<String, Object> message = completeConversation.get(i);
            if ("assistant".equals(message.get("role")) && message.containsKey("content")) {
                // 跳过思维内容，只返回最终答案
                Boolean isThinking = (Boolean) message.get("is_thinking");
                if (isThinking != null && isThinking) {
                    continue;
                }
                return (String) message.get("content");
            }
        }
        return "无法获取助手响应";
    }
    
    /**
     * 处理API响应，执行工具调用（如果有），并返回完整对话
     * @param response API响应
     * @param messages 消息历史
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> processResponseWithToolsAndReturnCompleteConversation(String response, List<Map<String, Object>> messages) {
        try {
            JsonNode responseJson = objectMapper.readTree(response);
            JsonNode choices = responseJson.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    // 检查是否有思维链内容（思考模式）
                    JsonNode reasoningContent = message.get("reasoning_content");
                    
                    // 检查是否有工具调用 - 支持标准tool_calls字段和DSML格式
                    JsonNode toolCalls = message.get("tool_calls");
                    String content = message.get("content") != null ? message.get("content").asText() : "";
                    
                    // 检查是否包含DSML格式的工具调用
                    List<Map<String, Object>> dsmlToolCalls = parseDSMLToolCalls(content);
                    
                    if ((toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) || !dsmlToolCalls.isEmpty()) {
                        // 执行工具调用（支持标准JSON格式和DSML格式）
                        List<Map<String, Object>> toolResults = new ArrayList<>();
                        
                        logger.info("检测到工具调用 - 标准tool_calls: {}, DSML格式: {}", 
                            toolCalls != null ? toolCalls.size() : 0, dsmlToolCalls.size());
                        
                        // 处理标准tool_calls格式
                        if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                            logger.info("开始处理标准JSON格式工具调用，数量: {}", toolCalls.size());
                            for (JsonNode toolCall : toolCalls) {
                                JsonNode function = toolCall.get("function");
                                if (function != null) {
                                    String functionName = function.get("name").asText();
                                    JsonNode arguments = function.get("arguments");
                                    
                                    logger.info("处理工具调用 - 函数名: {}, 参数: {}", functionName, arguments);
                                    
                                    Map<String, Object> argsMap = new HashMap<>();
                                    if (arguments != null && !arguments.asText().isEmpty()) {
                                        try {
                                            argsMap = objectMapper.readValue(arguments.asText(), Map.class);
                                            logger.info("成功解析参数: {}", argsMap);
                                        } catch (JsonProcessingException e) {
                                            logger.error("解析工具参数失败: {}", arguments.asText(), e);
                                        }
                                    }
                                    
                                    // 执行工具函数
                                    logger.info("开始执行工具函数: {}", functionName);
                                    String toolResult = executeTool(functionName, argsMap);
                                    logger.info("工具函数执行结果: {}", toolResult);
                                    
                                    // 记录工具调用结果
                                    Map<String, Object> toolResultMap = new HashMap<>();
                                    toolResultMap.put("tool_call_id", toolCall.get("id").asText());
                                    toolResultMap.put("role", "tool");
                                    toolResultMap.put("content", toolResult);
                                    toolResults.add(toolResultMap);
                                    logger.info("工具调用结果已添加到列表");
                                } else {
                                    logger.warn("工具调用缺少function字段: {}", toolCall);
                                }
                            }
                        }
                        
                        // 处理DSML格式工具调用
                        if (!dsmlToolCalls.isEmpty()) {
                            for (Map<String, Object> dsmlToolCall : dsmlToolCalls) {
                                String functionName = (String) dsmlToolCall.get("name");
                                Map<String, Object> argsMap = (Map<String, Object>) dsmlToolCall.get("arguments");
                                
                                // 执行工具函数
                                String toolResult = executeTool(functionName, argsMap);
                                
                                // 记录工具调用结果
                                Map<String, Object> toolResultMap = new HashMap<>();
                                toolResultMap.put("tool_call_id", "dsml_" + System.currentTimeMillis());
                                toolResultMap.put("role", "tool");
                                toolResultMap.put("content", toolResult);
                                toolResults.add(toolResultMap);
                            }
                        }
                        
                        // 添加助手消息到历史（必须包含tool_calls字段以便后续添加tool角色消息）
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("思维链内容: {}", reasoningContent.asText());
                        }
                        // 必须添加tool_calls字段，否则后续的tool角色消息会报错
                        if (toolCalls != null) {
                            assistantMessage.put("tool_calls", toolCalls);
                        }
                        messages.add(assistantMessage);
                        
                        // 添加工具结果到历史
                        for (Map<String, Object> toolResult : toolResults) {
                            messages.add(toolResult);
                        }
                        
                        // 发送下一次请求，支持多轮思考（循环处理直到没有工具调用）
                        return sendNextRequestAndReturnCompleteConversation(messages, 1);
                    } else {
                        // 没有工具调用，直接添加助手响应（deepseek-reasoner要求包含reasoning_content字段）
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("思维链内容: {}", reasoningContent.asText());
                        }
                        messages.add(assistantMessage);
                        
                        return messages;
                    }
                }
            }
            
            return messages;
        } catch (Exception e) {
            logger.error("处理工具调用响应失败", e);
            throw new RuntimeException("处理工具调用响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理API响应，执行工具调用（如果有），实时推送模型状态，并返回完整对话
     * @param response API响应
     * @param messages 消息历史
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> processResponseWithToolsAndStatusUpdates(String response, List<Map<String, Object>> messages, 
                                                                              String sessionId, java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        try {
            JsonNode responseJson = objectMapper.readTree(response);
            JsonNode choices = responseJson.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    // 检查是否有思维链内容
                    JsonNode reasoningContent = message.get("reasoning_content");
                    String content = message.get("content") != null ? message.get("content").asText() : "";
                    
                    // 检查是否有工具调用 - 支持标准tool_calls字段和DSML格式
                    JsonNode toolCalls = message.get("tool_calls");
                    
                    // 检查是否包含DSML格式的工具调用
                    List<Map<String, Object>> dsmlToolCalls = parseDSMLToolCalls(content);
                    
                    if ((toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) || !dsmlToolCalls.isEmpty()) {
                        // 有工具调用，需要执行工具并继续对话
                        logger.info("检测到工具调用，需要执行工具");
                        
                        // 实时推送工具调用状态
                        if (statusCallback != null) {
                            // 推送工具调用状态
                            pushToolCallStatus(toolCalls, dsmlToolCalls, sessionId, statusCallback);
                        }
                        
                        // 添加助手消息到历史（必须包含tool_calls字段以便后续添加tool角色消息）
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("思维链内容: {}", reasoningContent.asText());
                        }
                        
                        // 合并标准工具调用和DSML工具调用
                        List<Map<String, Object>> allToolCalls = new ArrayList<>();
                        if (toolCalls != null && toolCalls.isArray()) {
                            for (JsonNode toolCall : toolCalls) {
                                allToolCalls.add(objectMapper.convertValue(toolCall, Map.class));
                            }
                        }
                        allToolCalls.addAll(dsmlToolCalls);
                        
                        if (!allToolCalls.isEmpty()) {
                            assistantMessage.put("tool_calls", allToolCalls);
                        }
                        messages.add(assistantMessage);
                        
                        // 执行工具调用
                        List<Map<String, Object>> toolResults = executeToolCalls(allToolCalls);
                        
                        // 添加工具结果到历史
                        for (Map<String, Object> toolResult : toolResults) {
                            messages.add(toolResult);
                        }
                        
                        // 发送下一次请求，支持多轮思考（循环处理直到没有工具调用）
                        return sendNextRequestAndStatusUpdates(messages, 1, sessionId, statusCallback);
                    } else {
                        // 没有工具调用，直接添加助手响应（deepseek-reasoner要求包含reasoning_content字段）
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("思维链内容: {}", reasoningContent.asText());
                        }
                        messages.add(assistantMessage);
                        
                        return messages;
                    }
                }
            }
            
            return messages;
        } catch (Exception e) {
            logger.error("处理工具调用响应失败", e);
            throw new RuntimeException("处理工具调用响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送下一次请求，支持多轮思考（循环处理直到没有工具调用）
     * @param messages 消息历史
     * @param requestCount 请求次数（用于限制最大循环次数）
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> sendNextRequestAndReturnCompleteConversation(List<Map<String, Object>> messages, int requestCount) {
        try {
            // 限制最大请求次数，避免无限循环
            if (requestCount > 50) {
                logger.warn("已达到最大请求次数限制（{}），停止多轮思考", requestCount);
                return messages;
            }
            
            logger.info("发送第{}次请求，支持多轮思考", requestCount + 1);
            
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型（不需要额外的thinking参数）
            requestBody.put("model", "deepseek-reasoner");
            
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            requestBody.put("messages", messages);
            
            // 添加工具定义（每次请求都需要工具定义）
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送第{}次请求到DeepSeek API: {}", requestCount + 1, requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的第{}次响应: {}", requestCount + 1, result);
            
            // 检查是否有错误响应
            if (result.contains("\"error\"")) {
                logger.error("DeepSeek API返回错误: {}", result);
                throw new RuntimeException("DeepSeek API返回错误: " + result);
            }
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(result);
            JsonNode choices = responseJson.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    // 检查是否有思维链内容
                    JsonNode reasoningContent = message.get("reasoning_content");
                    String content = message.get("content") != null ? message.get("content").asText() : "";
                    
                    // 检查是否有工具调用 - 支持标准tool_calls字段和DSML格式
                    JsonNode toolCalls = message.get("tool_calls");
                    
                    // 检查是否包含DSML格式的工具调用
                    List<Map<String, Object>> dsmlToolCalls = parseDSMLToolCalls(content);
                    
                    if ((toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) || !dsmlToolCalls.isEmpty()) {
                        logger.info("第{}次响应检测到工具调用，继续多轮思考", requestCount + 1);
                        
                        // 执行工具调用（支持标准JSON格式和DSML格式）
                        List<Map<String, Object>> toolResults = new ArrayList<>();
                        
                        // 处理标准tool_calls格式
                        if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                            logger.info("处理第{}次响应的标准JSON格式工具调用，数量: {}", requestCount + 1, toolCalls.size());
                            for (JsonNode toolCall : toolCalls) {
                                JsonNode function = toolCall.get("function");
                                if (function != null) {
                                    String functionName = function.get("name").asText();
                                    JsonNode arguments = function.get("arguments");
                                    
                                    logger.info("处理工具调用 - 函数名: {}, 参数: {}", functionName, arguments);
                                    
                                    Map<String, Object> argsMap = new HashMap<>();
                                    if (arguments != null && !arguments.asText().isEmpty()) {
                                        try {
                                            argsMap = objectMapper.readValue(arguments.asText(), Map.class);
                                            logger.info("成功解析参数: {}", argsMap);
                                        } catch (JsonProcessingException e) {
                                            logger.error("解析工具参数失败: {}", arguments.asText(), e);
                                        }
                                    }
                                    
                                    // 执行工具函数
                                    logger.info("开始执行工具函数: {}", functionName);
                                    String toolResult = executeTool(functionName, argsMap);
                                    logger.info("工具函数执行结果: {}", toolResult);
                                    
                                    // 记录工具调用结果
                                    Map<String, Object> toolResultMap = new HashMap<>();
                                    toolResultMap.put("tool_call_id", toolCall.get("id").asText());
                                    toolResultMap.put("role", "tool");
                                    toolResultMap.put("content", toolResult);
                                    toolResults.add(toolResultMap);
                                    logger.info("工具调用结果已添加到列表");
                                } else {
                                    logger.warn("工具调用缺少function字段: {}", toolCall);
                                }
                            }
                        }
                        
                        // 处理DSML格式工具调用
                        if (!dsmlToolCalls.isEmpty()) {
                            logger.info("处理第{}次响应的DSML格式工具调用，数量: {}", requestCount + 1, dsmlToolCalls.size());
                            for (Map<String, Object> dsmlToolCall : dsmlToolCalls) {
                                String functionName = (String) dsmlToolCall.get("name");
                                Map<String, Object> argsMap = (Map<String, Object>) dsmlToolCall.get("arguments");
                                
                                // 执行工具函数
                                String toolResult = executeTool(functionName, argsMap);
                                
                                // 记录工具调用结果
                                Map<String, Object> toolResultMap = new HashMap<>();
                                toolResultMap.put("tool_call_id", "dsml_" + System.currentTimeMillis());
                                toolResultMap.put("role", "tool");
                                toolResultMap.put("content", toolResult);
                                toolResults.add(toolResultMap);
                            }
                        }
                        
                        // 添加助手消息到历史（必须包含tool_calls字段以便后续添加tool角色消息）
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("第{}次响应思维链内容: {}", requestCount + 1, reasoningContent.asText());
                        }
                        // 必须添加tool_calls字段，否则后续的tool角色消息会报错
                        if (toolCalls != null) {
                            assistantMessage.put("tool_calls", toolCalls);
                        }
                        messages.add(assistantMessage);
                        
                        // 添加工具结果到历史
                        for (Map<String, Object> toolResult : toolResults) {
                            messages.add(toolResult);
                        }
                        
                        // 继续发送下一次请求，支持多轮思考
                        return sendNextRequestAndReturnCompleteConversation(messages, requestCount + 1);
                    } else {
                        // 没有工具调用，添加最终助手响应
                        logger.info("第{}次响应没有检测到工具调用，结束多轮思考", requestCount + 1);
                        
                        Map<String, Object> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", content);
                        // deepseek-reasoner要求在多轮对话中必须包含reasoning_content字段
                        if (reasoningContent != null) {
                            assistantMessage.put("reasoning_content", reasoningContent.asText());
                            logger.info("最终响应思维链内容: {}", reasoningContent.asText());
                        }
                        messages.add(assistantMessage);
                        
                        return messages;
                    }
                }
            }
            
            return messages;
        } catch (Exception e) {
            logger.error("发送第{}次请求失败", requestCount + 1, e);
            throw new RuntimeException("发送第" + (requestCount + 1) + "次请求失败: " + e.getMessage(), e);
        }
    }
    

    
    /**
     * 解析DSML格式的工具调用
     * @param content 消息内容
     * @return DSML工具调用列表
     */
    private List<Map<String, Object>> parseDSMLToolCalls(String content) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return toolCalls;
        }
        
        try {
            // 首先尝试解析XML风格的DSML格式（如模型返回的格式）
            List<Map<String, Object>> xmlStyleCalls = parseXMLStyleDSML(content);
            if (!xmlStyleCalls.isEmpty()) {
                return xmlStyleCalls;
            }
            
            // 如果没有找到XML风格的DSML，尝试解析JSON风格的DSML
            String dsmlPattern = "<DSML>\\s*\\{(.*?)\\s*\\}\\s*</DSML>";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(dsmlPattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String jsonContent = "{" + matcher.group(1) + "}";
                try {
                    Map<String, Object> toolCall = objectMapper.readValue(jsonContent, Map.class);
                    toolCalls.add(toolCall);
                    logger.info("解析到JSON风格DSML格式工具调用: {}", jsonContent);
                } catch (Exception e) {
                    logger.error("解析JSON风格DSML工具调用失败: {}", jsonContent, e);
                }
            }
        } catch (Exception e) {
            logger.error("解析DSML格式失败", e);
        }
        
        return toolCalls;
    }
    
    /**
     * 解析XML风格的DSML格式工具调用
     * @param content 消息内容
     * @return DSML工具调用列表
     */
    private List<Map<String, Object>> parseXMLStyleDSML(String content) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        
        try {
            // 查找XML风格的DSML格式，如：<｜DSML｜invoke name="query_reservation_count">...</｜DSML｜invoke>
            String invokePattern = "<｜DSML｜invoke name=\\\"([^\\\"]+)\\\">(.*?)</｜DSML｜invoke>";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(invokePattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String functionName = matcher.group(1);
                String parametersContent = matcher.group(2);
                
                // 解析参数
                Map<String, Object> arguments = new HashMap<>();
                String paramPattern = "<｜DSML｜parameter name=\\\"([^\\\"]+)\\\" string=\\\"([^\\\"]+)\\\">([^<]+)</｜DSML｜parameter>";
                java.util.regex.Pattern paramPatternObj = java.util.regex.Pattern.compile(paramPattern);
                java.util.regex.Matcher paramMatcher = paramPatternObj.matcher(parametersContent);
                
                while (paramMatcher.find()) {
                    String paramName = paramMatcher.group(1);
                    String paramType = paramMatcher.group(2);
                    String paramValue = paramMatcher.group(3);
                    
                    // 根据类型转换参数值
                    if ("true".equals(paramType)) {
                        arguments.put(paramName, paramValue);
                    } else if ("false".equals(paramType)) {
                        // 尝试转换为数字
                        try {
                            if (paramValue.contains(".")) {
                                arguments.put(paramName, Double.parseDouble(paramValue));
                            } else {
                                arguments.put(paramName, Long.parseLong(paramValue));
                            }
                        } catch (NumberFormatException e) {
                            arguments.put(paramName, paramValue);
                        }
                    }
                }
                
                // 构建工具调用对象
                Map<String, Object> toolCall = new HashMap<>();
                toolCall.put("name", functionName);
                toolCall.put("arguments", arguments);
                toolCalls.add(toolCall);
                
                logger.info("解析到XML风格DSML格式工具调用: name={}, arguments={}", functionName, arguments);
            }
        } catch (Exception e) {
            logger.error("解析XML风格DSML格式失败", e);
        }
        
        return toolCalls;
    }
    
    /**
     * 批量执行工具调用
     * @param toolCalls 工具调用列表
     * @return 工具调用结果列表
     */
    private List<Map<String, Object>> executeToolCalls(List<Map<String, Object>> toolCalls) {
        List<Map<String, Object>> toolResults = new ArrayList<>();
        
        try {
            logger.info("开始批量执行工具调用，数量: {}", toolCalls.size());
            
            for (Map<String, Object> toolCall : toolCalls) {
                // 提取函数名和参数
                String functionName = null;
                Map<String, Object> arguments = new HashMap<>();
                
                // 处理标准JSON格式的工具调用
                if (toolCall.containsKey("function")) {
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    functionName = (String) function.get("name");
                    
                    if (function.containsKey("arguments")) {
                        Object argsObj = function.get("arguments");
                        if (argsObj instanceof String) {
                            // 参数是JSON字符串
                            try {
                                arguments = objectMapper.readValue((String) argsObj, Map.class);
                            } catch (Exception e) {
                                logger.error("解析工具参数失败: {}", argsObj, e);
                            }
                        } else if (argsObj instanceof Map) {
                            // 参数已经是Map
                            arguments = (Map<String, Object>) argsObj;
                        }
                    }
                } else if (toolCall.containsKey("name")) {
                    // 处理DSML格式的工具调用
                    functionName = (String) toolCall.get("name");
                    arguments = (Map<String, Object>) toolCall.get("arguments");
                }
                
                if (functionName != null) {
                    logger.info("执行工具调用 - 函数名: {}, 参数: {}", functionName, arguments);
                    
                    // 执行工具函数
                    String toolResult = executeTool(functionName, arguments);
                    
                    // 记录工具调用结果
                    Map<String, Object> toolResultMap = new HashMap<>();
                    toolResultMap.put("tool_call_id", toolCall.get("id") != null ? toolCall.get("id") : "dsml_" + System.currentTimeMillis());
                    toolResultMap.put("role", "tool");
                    toolResultMap.put("content", toolResult);
                    toolResults.add(toolResultMap);
                    
                    logger.info("工具调用结果已添加到列表");
                } else {
                    logger.warn("工具调用缺少函数名信息: {}", toolCall);
                }
            }
            
            logger.info("批量执行工具调用完成，共执行 {} 个工具调用", toolResults.size());
            
        } catch (Exception e) {
            logger.error("批量执行工具调用失败", e);
        }
        
        return toolResults;
    }
    
    /**
     * 执行工具函数
     * @param functionName 函数名
     * @param arguments 参数
     * @return 执行结果
     */
    private String executeTool(String functionName, Map<String, Object> arguments) {
        try {
            logger.info("在executeTool中查找函数: {}, 参数: {}", functionName, arguments);
            
            // 尝试从时间工具中获取函数
            Map<String, Function<Map<String, Object>, String>> timeToolFunctions = timeToolService.getToolFunctions();
            Function<Map<String, Object>, String> timeFunction = timeToolFunctions.get(functionName);
            
            if (timeFunction != null) {
                logger.info("找到时间工具函数: {}", functionName);
                return timeFunction.apply(arguments);
            }
            
            // 尝试从资源查询工具中获取函数
            Map<String, Function<Map<String, Object>, String>> resourceToolFunctions = resourceQueryToolService.getToolFunctions();
            Function<Map<String, Object>, String> resourceFunction = resourceToolFunctions.get(functionName);
            
            if (resourceFunction != null) {
                logger.info("找到资源查询工具函数: {}", functionName);
                return resourceFunction.apply(arguments);
            }
            
            // 尝试从预约次数查询工具中获取函数
            Map<String, Function<Map<String, Object>, String>> appointmentCountToolFunctions = appointmentCountToolService.getToolFunctions();
            Function<Map<String, Object>, String> appointmentCountFunction = appointmentCountToolFunctions.get(functionName);
            
            if (appointmentCountFunction != null) {
                logger.info("找到预约次数查询工具函数: {}", functionName);
                return appointmentCountFunction.apply(arguments);
            }
            
            // 尝试从预约可用性检查工具中获取函数
            Map<String, Function<Map<String, Object>, String>> availabilityCheckToolFunctions = availabilityCheckToolService.getToolFunctions();
            Function<Map<String, Object>, String> availabilityCheckFunction = availabilityCheckToolFunctions.get(functionName);
            
            if (availabilityCheckFunction != null) {
                logger.info("找到预约可用性检查工具函数: {}", functionName);
                return availabilityCheckFunction.apply(arguments);
            }
            
            // 尝试从添加预约记录工具中获取函数
            Map<String, Function<Map<String, Object>, String>> addAppointmentToolFunctions = addAppointmentToolService.getToolFunctions();
            Function<Map<String, Object>, String> addAppointmentFunction = addAppointmentToolFunctions.get(functionName);
            
            if (addAppointmentFunction != null) {
                logger.info("找到添加预约记录工具函数: {}", functionName);
                return addAppointmentFunction.apply(arguments);
            }
            
            logger.warn("未找到工具函数: {}", functionName);
            return "未找到工具函数: " + functionName;
        } catch (Exception e) {
            logger.error("执行工具函数失败: {}", functionName, e);
            return "执行工具函数失败: " + e.getMessage();
        }
    }
    
    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            logger.info("请求: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> 
                logger.info("请求头 {}: {}", name, values));
            return next.exchange(clientRequest);
        };
    }
    
    private ExchangeFilterFunction logResponse() {
        return (clientRequest, next) -> next.exchange(clientRequest)
                .doOnSuccess(response -> {
                    logger.info("响应状态码: {}", response.statusCode());
                    response.headers().asHttpHeaders().forEach((name, values) -> 
                        logger.info("响应头 {}: {}", name, values));
                });
    }
    
    /**
     * 处理带工具调用的消息请求，返回包含工具调用信息的响应
     * @param chatRequest 聊天请求
     * @return 包含工具调用信息的聊天响应
     */
    public ChatResponseWithTools processMessageWithTools(ChatRequest chatRequest) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCalls(messages);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 提取最终助手响应
            String finalAssistantResponse = extractFinalAssistantResponse(completeConversation);
            
            // 提取工具调用信息
            List<ToolCallResponse> toolCalls = extractToolCallsFromConversation(completeConversation);
            
            // 返回包含工具调用信息的响应
            ChatResponseWithTools response = new ChatResponseWithTools(finalAssistantResponse, actualSessionId, isNew, toolCalls);
            
            // 如果有工具调用，设置类型为tools
            if (response.isHasTools()) {
                response.setType("tools");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理带工具调用的消息请求失败", e);
            throw new RuntimeException("Failed to process message with tools", e);
        }
    }
    
    /**
     * 从完整对话中提取工具调用信息
     * @param completeConversation 完整对话
     * @return 工具调用信息列表
     */
    private List<ToolCallResponse> extractToolCallsFromConversation(List<Map<String, Object>> completeConversation) {
        List<ToolCallResponse> toolCalls = new ArrayList<>();
        
        logger.info("开始提取工具调用信息，对话长度: {}", completeConversation.size());
        
        for (int i = 0; i < completeConversation.size(); i++) {
            Map<String, Object> message = completeConversation.get(i);
            String role = (String) message.get("role");
            
            logger.info("检查消息 {}: role={}, 包含tool_calls: {}", i, role, message.containsKey("tool_calls"));
            
            // 检查是否是助手消息且包含工具调用
            if ("assistant".equals(role) && message.containsKey("tool_calls")) {
                try {
                    Object toolCallsObj = message.get("tool_calls");
                    logger.info("找到工具调用对象: {}", toolCallsObj.getClass().getName());
                    
                    // 处理JsonNode类型的tool_calls（DeepSeek API返回的格式）
                    if (toolCallsObj instanceof com.fasterxml.jackson.databind.JsonNode) {
                        com.fasterxml.jackson.databind.JsonNode toolCallsJson = (com.fasterxml.jackson.databind.JsonNode) toolCallsObj;
                        
                        if (toolCallsJson.isArray()) {
                            logger.info("找到JSON数组格式的工具调用，数量: {}", toolCallsJson.size());
                            
                            for (com.fasterxml.jackson.databind.JsonNode toolCall : toolCallsJson) {
                                com.fasterxml.jackson.databind.JsonNode function = toolCall.get("function");
                                if (function != null) {
                                    String functionName = function.get("name").asText();
                                    String argumentsJson = function.get("arguments").asText();
                                    
                                    logger.info("处理工具调用 - 函数名: {}, 参数: {}", functionName, argumentsJson);
                                    
                                    // 解析参数
                                    Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
                                    
                                    // 查找对应的工具结果
                                    String toolResult = findToolResult(completeConversation, i, toolCall);
                                    
                                    // 创建工具调用响应
                                    ToolCallResponse.ToolType toolType = mapFunctionNameToToolType(functionName);
                                    ToolCallResponse toolCallResponse = new ToolCallResponse(
                                        toolType, functionName, arguments, toolResult, "", true
                                    );
                                    
                                    toolCalls.add(toolCallResponse);
                                    logger.info("成功添加工具调用响应: {}", functionName);
                                }
                            }
                        }
                    } else if (toolCallsObj instanceof List) {
                        // 处理List类型的tool_calls（兼容旧格式）
                        List<Map<String, Object>> toolCallList = (List<Map<String, Object>>) toolCallsObj;
                        logger.info("找到List格式的工具调用，数量: {}", toolCallList.size());
                        
                        for (Map<String, Object> toolCall : toolCallList) {
                            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                            String functionName = (String) function.get("name");
                            String argumentsJson = (String) function.get("arguments");
                            
                            // 解析参数
                            Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
                            
                            // 查找对应的工具结果
                            String toolResult = findToolResult(completeConversation, i, toolCall);
                            
                            // 创建工具调用响应
                            ToolCallResponse.ToolType toolType = mapFunctionNameToToolType(functionName);
                            ToolCallResponse toolCallResponse = new ToolCallResponse(
                                toolType, functionName, arguments, toolResult, "", true
                            );
                            
                            toolCalls.add(toolCallResponse);
                        }
                    }
                } catch (Exception e) {
                    logger.error("提取工具调用信息失败", e);
                }
            }
        }
        
        logger.info("提取工具调用信息完成，共找到 {} 个工具调用", toolCalls.size());
        return toolCalls;
    }
    
    /**
     * 查找工具调用的结果
     * @param completeConversation 完整对话
     * @param assistantIndex 助手消息的索引
     * @param toolCall 工具调用信息
     * @return 工具调用结果
     */
    private String findToolResult(List<Map<String, Object>> completeConversation, int assistantIndex, 
                                 Object toolCall) {
        String toolCallId = null;
        
        // 根据工具调用对象的类型提取ID
        if (toolCall instanceof Map) {
            toolCallId = (String) ((Map<String, Object>) toolCall).get("id");
        } else if (toolCall instanceof com.fasterxml.jackson.databind.JsonNode) {
            com.fasterxml.jackson.databind.JsonNode toolCallJson = (com.fasterxml.jackson.databind.JsonNode) toolCall;
            toolCallId = toolCallJson.get("id") != null ? toolCallJson.get("id").asText() : null;
        }
        
        if (toolCallId == null) {
            logger.warn("工具调用缺少ID字段");
            return "未找到工具调用结果";
        }
        
        logger.info("查找工具调用结果，工具调用ID: {}", toolCallId);
        
        // 从助手消息之后开始查找
        for (int j = assistantIndex + 1; j < completeConversation.size(); j++) {
            Map<String, Object> message = completeConversation.get(j);
            String role = (String) message.get("role");
            
            if ("tool".equals(role)) {
                String currentToolCallId = (String) message.get("tool_call_id");
                logger.info("检查工具消息 {}: tool_call_id={}", j, currentToolCallId);
                
                if (toolCallId.equals(currentToolCallId)) {
                    String result = (String) message.get("content");
                    logger.info("找到工具调用结果: {}", result);
                    return result;
                }
            }
        }
        
        logger.warn("未找到工具调用结果，工具调用ID: {}", toolCallId);
        return "未找到工具调用结果";
    }
    
    /**
     * 将函数名映射到工具类型
     * @param functionName 函数名
     * @return 工具类型
     */
    private ToolCallResponse.ToolType mapFunctionNameToToolType(String functionName) {
        switch (functionName) {
            case "get_current_time":
                return ToolCallResponse.ToolType.TIMETOOL;
            case "add_appointment":
                return ToolCallResponse.ToolType.ADDAPPOINTMENTTOOL;
            case "query_reservation_count":
                return ToolCallResponse.ToolType.APPOINTMENTCOUNTTOOL;
            case "query_resources":
                return ToolCallResponse.ToolType.RESOURCEQUERYTOOL;
            case "check_availability":
                return ToolCallResponse.ToolType.AVAILABILITYCHECKTOOL;
            default:
                return ToolCallResponse.ToolType.TIMETOOL; // 默认类型
        }
    }
    
    /**
     * 处理消息请求，返回完整的模型响应数据
     * @param chatRequest 聊天请求
     * @return 完整的模型响应，包含content、reasoning_content和tool_calls
     */
    public FullModelResponse processMessageWithFullResponse(ChatRequest chatRequest) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCalls(messages);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 提取最后一次助手响应
            Map<String, Object> lastAssistantMessage = getLastAssistantMessage(completeConversation);
            
            // 提取content和reasoning_content
            String content = extractContent(lastAssistantMessage);
            String reasoningContent = extractReasoningContent(lastAssistantMessage);
            
            // 提取工具调用信息（只返回特定类型的工具）
            List<ToolCallResponse> toolCalls = extractSpecificToolCalls(completeConversation);
            
            // 返回完整的模型响应
            FullModelResponse response = new FullModelResponse(content, reasoningContent, actualSessionId, isNew, toolCalls);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理完整模型响应失败", e);
            throw new RuntimeException("Failed to process message with full response", e);
        }
    }
    
    /**
     * 获取最后一次助手消息
     * @param completeConversation 完整对话
     * @return 最后一次助手消息
     */
    private Map<String, Object> getLastAssistantMessage(List<Map<String, Object>> completeConversation) {
        // 从后往前查找最后一次助手消息
        for (int i = completeConversation.size() - 1; i >= 0; i--) {
            Map<String, Object> message = completeConversation.get(i);
            if ("assistant".equals(message.get("role"))) {
                return message;
            }
        }
        return new HashMap<>();
    }
    
    /**
     * 提取content字段
     * @param assistantMessage 助手消息
     * @return content内容
     */
    private String extractContent(Map<String, Object> assistantMessage) {
        Object content = assistantMessage.get("content");
        if (content instanceof String) {
            return (String) content;
        }
        return "";
    }
    
    /**
     * 提取reasoning_content字段
     * @param assistantMessage 助手消息
     * @return reasoning_content内容
     */
    private String extractReasoningContent(Map<String, Object> assistantMessage) {
        Object reasoningContent = assistantMessage.get("reasoning_content");
        if (reasoningContent instanceof String) {
            return (String) reasoningContent;
        }
        return "";
    }
    
    /**
     * 提取特定类型的工具调用信息
     * @param completeConversation 完整对话
     * @return 特定类型的工具调用列表
     */
    private List<ToolCallResponse> extractSpecificToolCalls(List<Map<String, Object>> completeConversation) {
        List<ToolCallResponse> allToolCalls = extractToolCallsFromConversation(completeConversation);
        List<ToolCallResponse> specificToolCalls = new ArrayList<>();
        
        // 只返回特定类型的工具调用
        for (ToolCallResponse toolCall : allToolCalls) {
            if (isSpecificToolType(toolCall.getToolType())) {
                specificToolCalls.add(toolCall);
            }
        }
        
        logger.info("提取特定类型工具调用，总数: {}，特定类型: {}", allToolCalls.size(), specificToolCalls.size());
        return specificToolCalls;
    }
    
    /**
     * 检查是否为特定类型的工具
     * @param toolType 工具类型
     * @return 是否为特定类型
     */
    private boolean isSpecificToolType(ToolCallResponse.ToolType toolType) {
        return toolType == ToolCallResponse.ToolType.TIMETOOL ||
               toolType == ToolCallResponse.ToolType.ADDAPPOINTMENTTOOL ||
               toolType == ToolCallResponse.ToolType.APPOINTMENTCOUNTTOOL ||
               toolType == ToolCallResponse.ToolType.RESOURCEQUERYTOOL ||
               toolType == ToolCallResponse.ToolType.AVAILABILITYCHECKTOOL;
    }
    
    /**
     * 处理消息请求，实时推送每次模型返回的中间结果
     * @param chatRequest 聊天请求
     * @param stepCallback 步骤回调函数，用于实时推送中间结果
     * @return 最终响应
     */
    public FullModelResponse processMessageWithRealTimeSteps(ChatRequest chatRequest, 
                                                           java.util.function.Consumer<ModelStepResponse> stepCallback) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCalls(messages);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 分析对话，提取所有步骤
            List<ModelStepResponse> allSteps = analyzeConversationSteps(completeConversation, actualSessionId);
            
            // 实时推送每个步骤
            for (int i = 0; i < allSteps.size(); i++) {
                ModelStepResponse step = allSteps.get(i);
                step.setStepIndex(i + 1);
                step.setTotalSteps(allSteps.size());
                step.setFinal(i == allSteps.size() - 1);
                
                // 推送步骤
                if (stepCallback != null) {
                    stepCallback.accept(step);
                }
                
                // 添加延迟，让客户端有时间显示
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 提取最后一次助手响应
            Map<String, Object> lastAssistantMessage = getLastAssistantMessage(completeConversation);
            
            // 提取content和reasoning_content
            String content = extractContent(lastAssistantMessage);
            String reasoningContent = extractReasoningContent(lastAssistantMessage);
            
            // 返回最终的完整模型响应（不包含tool_calls）
            FullModelResponse response = new FullModelResponse(content, reasoningContent, actualSessionId, isNew);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理实时步骤消息失败", e);
            throw new RuntimeException("Failed to process message with real-time steps", e);
        }
    }
    
    /**
     * 分析对话，提取所有步骤
     * @param completeConversation 完整对话
     * @param sessionId 会话ID
     * @return 所有步骤列表
     */
    private List<ModelStepResponse> analyzeConversationSteps(List<Map<String, Object>> completeConversation, String sessionId) {
        List<ModelStepResponse> steps = new ArrayList<>();
        
        logger.info("开始分析对话步骤，对话长度: {}", completeConversation.size());
        
        for (int i = 0; i < completeConversation.size(); i++) {
            Map<String, Object> message = completeConversation.get(i);
            String role = (String) message.get("role");
            
            if ("assistant".equals(role)) {
                // 助手消息可能包含思考过程或工具调用
                ModelStepResponse step = createStepFromAssistantMessage(message, sessionId, i);
                if (step != null) {
                    steps.add(step);
                }
            } else if ("tool".equals(role)) {
                // 工具调用结果
                ModelStepResponse step = createStepFromToolMessage(message, sessionId, i);
                if (step != null) {
                    steps.add(step);
                }
            }
        }
        
        logger.info("分析完成，共找到 {} 个步骤", steps.size());
        return steps;
    }
    
    /**
     * 从助手消息创建步骤
     * @param message 助手消息
     * @param sessionId 会话ID
     * @param index 索引
     * @return 步骤响应
     */
    private ModelStepResponse createStepFromAssistantMessage(Map<String, Object> message, String sessionId, int index) {
        ModelStepResponse step = new ModelStepResponse();
        step.setSessionId(sessionId);
        
        // 提取content
        String content = extractContent(message);
        if (content != null && !content.trim().isEmpty()) {
            step.setContent(content);
        }
        
        // 提取reasoning_content
        String reasoningContent = extractReasoningContent(message);
        if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
            step.setReasoningContent(reasoningContent);
        }
        
        // 检查是否包含工具调用
        if (message.containsKey("tool_calls")) {
            step.setStepType(ModelStepResponse.StepType.TOOL_CALL);
            
            // 提取工具调用信息
            List<ToolCallResponse> toolCalls = extractToolCallsFromSingleMessage(message);
            if (!toolCalls.isEmpty()) {
                step.setToolCalls(toolCalls);
            }
        } else {
            // 纯思考过程
            step.setStepType(ModelStepResponse.StepType.THINKING);
        }
        
        return step;
    }
    
    /**
     * 从工具消息创建步骤
     * @param message 工具消息
     * @param sessionId 会话ID
     * @param index 索引
     * @return 步骤响应
     */
    private ModelStepResponse createStepFromToolMessage(Map<String, Object> message, String sessionId, int index) {
        ModelStepResponse step = new ModelStepResponse();
        step.setSessionId(sessionId);
        step.setStepType(ModelStepResponse.StepType.TOOL_CALL);
        
        // 工具消息包含工具调用结果
        String content = extractContent(message);
        if (content != null && !content.trim().isEmpty()) {
            step.setContent("工具调用结果: " + content);
        }
        
        return step;
    }
    
    /**
     * 从单个消息中提取工具调用信息
     * @param message 消息
     * @return 工具调用列表
     */
    private List<ToolCallResponse> extractToolCallsFromSingleMessage(Map<String, Object> message) {
        List<ToolCallResponse> toolCalls = new ArrayList<>();
        
        if (message.containsKey("tool_calls")) {
            try {
                Object toolCallsObj = message.get("tool_calls");
                
                // 处理JsonNode类型的tool_calls
                if (toolCallsObj instanceof com.fasterxml.jackson.databind.JsonNode) {
                    com.fasterxml.jackson.databind.JsonNode toolCallsJson = (com.fasterxml.jackson.databind.JsonNode) toolCallsObj;
                    
                    if (toolCallsJson.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode toolCall : toolCallsJson) {
                            com.fasterxml.jackson.databind.JsonNode function = toolCall.get("function");
                            if (function != null) {
                                String functionName = function.get("name").asText();
                                String argumentsJson = function.get("arguments").asText();
                                
                                // 解析参数
                                Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
                                
                                // 创建工具调用响应
                                ToolCallResponse.ToolType toolType = mapFunctionNameToToolType(functionName);
                                ToolCallResponse toolCallResponse = new ToolCallResponse(
                                    toolType, functionName, arguments, "等待执行...", "", false
                                );
                                
                                toolCalls.add(toolCallResponse);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("从单个消息提取工具调用失败", e);
            }
        }
        
        return toolCalls;
    }
    
    /**
     * 处理消息请求，实时推送模型状态信息
     * @param chatRequest 聊天请求
     * @param statusCallback 状态回调函数，用于实时推送模型状态
     * @return 最终响应
     */
    public FullModelResponse processMessageWithStatusUpdates(ChatRequest chatRequest, 
                                                            java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 推送初始状态：正在思考问题
            if (statusCallback != null) {
                ModelStatusResponse thinkingStatus = new ModelStatusResponse(
                    ModelStatusResponse.StatusType.THINKING, actualSessionId
                );
                statusCallback.accept(thinkingStatus);
                Thread.sleep(300); // 让客户端有时间显示
            }
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话，并实时推送状态
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCallsAndStatusUpdates(messages, actualSessionId, statusCallback);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 推送最终状态：正在生成最终回复
            if (statusCallback != null) {
                ModelStatusResponse finalizingStatus = new ModelStatusResponse(
                    ModelStatusResponse.StatusType.FINALIZING, actualSessionId
                );
                statusCallback.accept(finalizingStatus);
                Thread.sleep(300);
            }
            
            // 提取最后一次助手响应
            Map<String, Object> lastAssistantMessage = getLastAssistantMessage(completeConversation);
            
            // 提取content和reasoning_content
            String content = extractContent(lastAssistantMessage);
            String reasoningContent = extractReasoningContent(lastAssistantMessage);
            
            // 提取工具调用信息（只返回特定类型的工具）
            List<ToolCallResponse> toolCalls = extractSpecificToolCalls(completeConversation);
            
            // 返回最终的完整模型响应
            FullModelResponse response = new FullModelResponse(content, reasoningContent, actualSessionId, isNew, toolCalls);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理状态更新消息失败", e);
            throw new RuntimeException("Failed to process message with status updates", e);
        }
    }
    
    /**
     * 分析对话，推送模型状态更新
     * @param completeConversation 完整对话
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     */
    private void pushModelStatusUpdates(List<Map<String, Object>> completeConversation, String sessionId,
                                       java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        if (statusCallback == null) return;
        
        logger.info("开始推送模型状态更新，对话长度: {}", completeConversation.size());
        
        for (int i = 0; i < completeConversation.size(); i++) {
            Map<String, Object> message = completeConversation.get(i);
            String role = (String) message.get("role");
            
            if ("assistant".equals(role)) {
                // 助手消息可能包含工具调用
                if (message.containsKey("tool_calls")) {
                    try {
                        Object toolCallsObj = message.get("tool_calls");
                        
                        // 处理JsonNode类型的tool_calls
                        if (toolCallsObj instanceof com.fasterxml.jackson.databind.JsonNode) {
                            com.fasterxml.jackson.databind.JsonNode toolCallsJson = (com.fasterxml.jackson.databind.JsonNode) toolCallsObj;
                            
                            if (toolCallsJson.isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode toolCall : toolCallsJson) {
                                    com.fasterxml.jackson.databind.JsonNode function = toolCall.get("function");
                                    if (function != null) {
                                        String functionName = function.get("name").asText();
                                        
                                        // 根据函数名推送对应的状态
                                        ModelStatusResponse.StatusType statusType = getStatusTypeFromFunctionName(functionName);
                                        if (statusType != null) {
                                            ModelStatusResponse status = new ModelStatusResponse(statusType, sessionId);
                                            statusCallback.accept(status);
                                            Thread.sleep(500); // 让客户端有时间显示
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("推送工具调用状态失败", e);
                    }
                }
            }
        }
        
        logger.info("模型状态更新推送完成");
    }
    
    /**
     * 根据函数名获取对应的状态类型
     * @param functionName 函数名
     * @return 状态类型
     */
    private ModelStatusResponse.StatusType getStatusTypeFromFunctionName(String functionName) {
        switch (functionName) {
            case "get_current_time":
                return ModelStatusResponse.StatusType.PARSING_TIME;
            case "add_appointment":
            case "add_reservation":
                return ModelStatusResponse.StatusType.ADDING_APPOINTMENT;
            case "query_reservation_count":
            case "query_appointment_count":
                return ModelStatusResponse.StatusType.ALLOCATING_RESOURCES;
            case "query_resources":
                return ModelStatusResponse.StatusType.QUERYING_RESOURCES;
            case "check_availability":
                return ModelStatusResponse.StatusType.CHECKING_AVAILABILITY;
            default:
                return null; // 只返回特定类型的工具状态
        }
    }
    
    /**
     * 处理消息请求，只推送工具调用状态和最终结果
     * @param chatRequest 聊天请求
     * @param statusCallback 状态回调函数，用于推送工具调用状态
     * @return 简化的最终响应
     */
    public SimpleModelResponse processMessageWithSimpleUpdates(ChatRequest chatRequest, 
                                                           java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        try {
            String message = chatRequest.getMessage();
            String sessionId = chatRequest.getSessionId();
            String token = chatRequest.getToken();
            
            // 获取或创建会话
            Map<String, Object> sessionInfo = sessionService.getOrCreateSession(sessionId);
            String actualSessionId = (String) sessionInfo.get("session_id");
            boolean isNew = (boolean) sessionInfo.get("is_new");
            
            // 获取会话历史记录
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词，并替换用户账号
            String processedSystemPrompt = processSystemPromptWithUserAccount(token);
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", processedSystemPrompt);
            messages.add(systemMessage);
            
            if (!isNew) {
                // 获取历史记录，但排除系统提示词（因为已经添加了）
                List<Map<String, Object>> history = sessionService.getSessionHistory(actualSessionId);
                for (Map<String, Object> msg : history) {
                    if (!"system".equals(msg.get("role"))) {
                        messages.add(msg);
                    }
                }
            }
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);
            
            // 调用DeepSeek API，获取包含工具调用的完整对话
            List<Map<String, Object>> completeConversation = callDeepSeekAPIWithToolCalls(messages);
            
            // 保存完整的对话记录（包括工具调用）
            sessionService.addCompleteConversationToSession(actualSessionId, completeConversation);
            
            // 分析对话，只推送工具调用状态
            pushToolStatusUpdates(completeConversation, actualSessionId, statusCallback);
            
            // 提取最后一次助手响应
            Map<String, Object> lastAssistantMessage = getLastAssistantMessage(completeConversation);
            
            // 提取content
            String content = extractContent(lastAssistantMessage);
            
            // 返回简化的最终响应（不包含tool_calls）
            SimpleModelResponse response = new SimpleModelResponse(content, actualSessionId, isNew);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理简化消息失败", e);
            throw new RuntimeException("Failed to process message with simple updates", e);
        }
    }
    
    /**
     * 分析对话，只推送工具调用状态
     * @param completeConversation 完整对话
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     */
    private void pushToolStatusUpdates(List<Map<String, Object>> completeConversation, String sessionId,
                                       java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        if (statusCallback == null) return;
        
        logger.info("开始推送工具调用状态，对话长度: {}", completeConversation.size());
        
        for (int i = 0; i < completeConversation.size(); i++) {
            Map<String, Object> message = completeConversation.get(i);
            String role = (String) message.get("role");
            
            if ("assistant".equals(role)) {
                // 助手消息可能包含工具调用
                if (message.containsKey("tool_calls")) {
                    try {
                        Object toolCallsObj = message.get("tool_calls");
                        
                        // 处理JsonNode类型的tool_calls
                        if (toolCallsObj instanceof com.fasterxml.jackson.databind.JsonNode) {
                            com.fasterxml.jackson.databind.JsonNode toolCallsJson = (com.fasterxml.jackson.databind.JsonNode) toolCallsObj;
                            
                            if (toolCallsJson.isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode toolCall : toolCallsJson) {
                                    com.fasterxml.jackson.databind.JsonNode function = toolCall.get("function");
                                    if (function != null) {
                                        String functionName = function.get("name").asText();
                                        
                                        // 根据函数名推送对应的状态
                                        ModelStatusResponse.StatusType statusType = getStatusTypeFromFunctionName(functionName);
                                        if (statusType != null) {
                                            ModelStatusResponse status = new ModelStatusResponse(statusType, sessionId);
                                            statusCallback.accept(status);
                                            Thread.sleep(300); // 让客户端有时间显示
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("推送工具调用状态失败", e);
                    }
                }
            }
        }
        
        logger.info("工具调用状态推送完成");
    }
    
    /**
     * 提取特定类型的工具调用信息（不包含结果）
     * @param completeConversation 完整对话
     * @return 特定类型的工具调用列表（不包含结果）
     */
    private List<ToolCallResponse> extractSpecificToolCallsWithoutResults(List<Map<String, Object>> completeConversation) {
        List<ToolCallResponse> allToolCalls = extractToolCallsFromConversation(completeConversation);
        List<ToolCallResponse> specificToolCalls = new ArrayList<>();
        
        // 只返回特定类型的工具调用，并清除结果
        for (ToolCallResponse toolCall : allToolCalls) {
            if (isSpecificToolType(toolCall.getToolType())) {
                // 创建新的工具调用响应，不包含结果
                ToolCallResponse toolCallWithoutResult = new ToolCallResponse(
                    toolCall.getToolType(), 
                    toolCall.getToolName(), 
                    toolCall.getArguments(), 
                    "", // 清除结果
                    toolCall.getToolDescription(), 
                    false
                );
                specificToolCalls.add(toolCallWithoutResult);
            }
        }
        
        logger.info("提取特定类型工具调用（无结果），总数: {}，特定类型: {}", allToolCalls.size(), specificToolCalls.size());
        return specificToolCalls;
    }
    
    /**
     * 推送工具调用状态
     * @param toolCalls 标准工具调用
     * @param dsmlToolCalls DSML格式工具调用
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     */
    private void pushToolCallStatus(JsonNode toolCalls, List<Map<String, Object>> dsmlToolCalls, String sessionId,
                                   java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        if (statusCallback == null) return;
        
        try {
            // 处理标准工具调用
            if (toolCalls != null && toolCalls.isArray()) {
                for (JsonNode toolCall : toolCalls) {
                    JsonNode function = toolCall.get("function");
                    if (function != null) {
                        String functionName = function.get("name").asText();
                        
                        // 根据函数名推送对应的状态
                        ModelStatusResponse.StatusType statusType = getStatusTypeFromFunctionName(functionName);
                        if (statusType != null) {
                            ModelStatusResponse status = new ModelStatusResponse(statusType, sessionId);
                            statusCallback.accept(status);
                            Thread.sleep(300); // 让客户端有时间显示
                        }
                    }
                }
            }
            
            // 处理DSML格式工具调用
            if (!dsmlToolCalls.isEmpty()) {
                for (Map<String, Object> dsmlToolCall : dsmlToolCalls) {
                    String functionName = (String) dsmlToolCall.get("name");
                    
                    // 根据函数名推送对应的状态
                    ModelStatusResponse.StatusType statusType = getStatusTypeFromFunctionName(functionName);
                    if (statusType != null) {
                        ModelStatusResponse status = new ModelStatusResponse(statusType, sessionId);
                        statusCallback.accept(status);
                        Thread.sleep(300); // 让客户端有时间显示
                    }
                }
            }
        } catch (Exception e) {
            logger.error("推送工具调用状态失败", e);
        }
    }
    
    /**
     * 发送下一次请求，支持多轮思考，并实时推送状态
     * @param messages 消息历史
     * @param requestCount 请求次数
     * @param sessionId 会话ID
     * @param statusCallback 状态回调函数
     * @return 包含工具调用的完整对话
     */
    private List<Map<String, Object>> sendNextRequestAndStatusUpdates(List<Map<String, Object>> messages, int requestCount,
                                                                     String sessionId, java.util.function.Consumer<ModelStatusResponse> statusCallback) {
        try {
            // 限制最大请求次数，避免无限循环
            if (requestCount > 50) {
                logger.warn("已达到最大请求次数限制（{}），停止多轮思考", requestCount);
                return messages;
            }
            
            logger.info("发送第{}次请求，支持多轮思考", requestCount + 1);
            
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            
            // 使用deepseek-reasoner模型（不需要额外的thinking参数）
            requestBody.put("model", "deepseek-reasoner");
            
            requestBody.put("max_tokens", deepseekApiMaxTokens);
            requestBody.put("temperature", deepseekApiTemperature);
            requestBody.put("messages", messages);
            
            // 添加工具定义（每次请求都需要工具定义）
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(timeToolService.getCurrentTimeToolDefinition());
            tools.add(resourceQueryToolService.getResourceQueryToolDefinition());
            tools.add(appointmentCountToolService.getAppointmentCountToolDefinition());
            tools.add(availabilityCheckToolService.getAvailabilityCheckToolDefinition());
            tools.add(addAppointmentToolService.getAddAppointmentToolDefinition());
            requestBody.put("tools", tools);
            
            // 设置工具选择模式为auto
            requestBody.put("tool_choice", "auto");
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("发送第{}次请求到DeepSeek API: {}", requestCount + 1, requestBodyJson);

            // 发送请求
            Mono<String> response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .bodyValue(requestBodyJson)
                    .retrieve()
                    .bodyToMono(String.class);

            String result = response.block();
            logger.info("从DeepSeek API收到的第{}次响应: {}", requestCount + 1, result);
            
            // 检查是否有错误响应
            if (result.contains("\"error\"")) {
                logger.error("DeepSeek API返回错误: {}", result);
                throw new RuntimeException("DeepSeek API返回错误: " + result);
            }
            
            // 解析响应，检查是否有工具调用，并实时推送状态
            return processResponseWithToolsAndStatusUpdates(result, messages, sessionId, statusCallback);
            
        } catch (Exception e) {
            logger.error("发送第{}次请求失败", requestCount + 1, e);
            throw new RuntimeException("发送第" + (requestCount + 1) + "次请求失败: " + e.getMessage(), e);
        }
    }
}