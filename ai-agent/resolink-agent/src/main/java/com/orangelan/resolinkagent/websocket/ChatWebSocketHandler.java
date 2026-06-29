package com.orangelan.resolinkagent.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.dto.ChatRequest;
import com.orangelan.resolinkagent.dto.ChatResponse;
import com.orangelan.resolinkagent.dto.ChatResponseWithTools;
import com.orangelan.resolinkagent.dto.FullModelResponse;
import com.orangelan.resolinkagent.dto.ModelStepResponse;
import com.orangelan.resolinkagent.dto.ModelStatusResponse;
import com.orangelan.resolinkagent.dto.SimpleModelResponse;
import com.orangelan.resolinkagent.dto.StreamingResponse;
import com.orangelan.resolinkagent.service.ChatService;
import com.orangelan.resolinkagent.service.StreamingChatService;
import com.orangelan.resolinkagent.service.TokenValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ChatService chatService;
    private final StreamingChatService streamingChatService;
    private final ObjectMapper objectMapper;
    private final TokenValidationService tokenValidationService;
    
    // 存储活跃的WebSocket会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, StreamingChatService streamingChatService, ObjectMapper objectMapper, 
                               TokenValidationService tokenValidationService) {
        this.chatService = chatService;
        this.streamingChatService = streamingChatService;
        this.objectMapper = objectMapper;
        this.tokenValidationService = tokenValidationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        // 从握手属性中获取token
        String token = (String) session.getAttributes().get("token");
        
        logger.info("WebSocket连接建立: {}, token: {}", sessionId, token != null ? "已提供" : "未提供");
        
        // 发送连接成功消息
        Map<String, Object> connectMessage = new HashMap<>();
        connectMessage.put("message", "连接成功，可以开始聊天");
        connectMessage.put("sessionId", sessionId);
        connectMessage.put("tokenValid", token != null);
        
        WebSocketMessage webSocketMessage = new WebSocketMessage(
            WebSocketMessage.MessageType.CONNECT, 
            connectMessage, 
            sessionId
        );
        sendWebSocketMessage(session, webSocketMessage);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        logger.info("收到WebSocket消息: {} from {}", payload, sessionId);

        try {
            // 解析WebSocket消息
            WebSocketMessage webSocketMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            if (webSocketMessage.getType() == WebSocketMessage.MessageType.CHAT) {
                // 处理聊天消息
                ChatRequest chatRequest = objectMapper.convertValue(webSocketMessage.getContent(), ChatRequest.class);
                
                // 验证消息中的token（如果提供了的话）
                if (chatRequest.getToken() != null && !chatRequest.getToken().isEmpty()) {
                    Boolean isValid = tokenValidationService.validateToken(chatRequest.getToken()).block();
                    if (Boolean.FALSE.equals(isValid)) {
                        // Token验证失败
                        Map<String, Object> errorContent = new HashMap<>();
                        errorContent.put("error", "Token验证失败，请检查token有效性");
                        
                        WebSocketMessage errorMessage = new WebSocketMessage(
                            WebSocketMessage.MessageType.ERROR, 
                            errorContent, 
                            sessionId
                        );
                        sendWebSocketMessage(session, errorMessage);
                        return;
                    }
                }
                
                // 使用支持状态更新的聊天服务处理消息
                FullModelResponse finalResponse = chatService.processMessageWithStatusUpdates(
                    chatRequest, 
                    (ModelStatusResponse status) -> {
                        // 实时推送模型状态
                        WebSocketMessage statusMessage = new WebSocketMessage(
                            WebSocketMessage.MessageType.MODEL_STATUS, 
                            status, 
                            sessionId
                        );
                        sendWebSocketMessage(session, statusMessage);
                    }
                );
                
                // 发送完整最终响应
                WebSocketMessage finalMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.FULL_MODEL, 
                    finalResponse, 
                    sessionId
                );
                sendWebSocketMessage(session, finalMessage);
            } else {
                // 处理其他类型的消息
                handleOtherMessageTypes(session, webSocketMessage);
            }
            
        } catch (Exception e) {
            logger.error("处理WebSocket消息时出错: {}", e.getMessage(), e);
            
            // 发送错误响应
            Map<String, Object> errorContent = new HashMap<>();
            errorContent.put("error", "处理消息时发生错误: " + e.getMessage());
            
            WebSocketMessage errorMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.ERROR, 
                errorContent, 
                sessionId
            );
            sendWebSocketMessage(session, errorMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("WebSocket连接关闭: {}, 状态: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket传输错误: {}, {}", sessionId, exception.getMessage(), exception);
        
        // 发送错误消息
        Map<String, Object> errorContent = new HashMap<>();
        errorContent.put("error", "连接发生错误: " + exception.getMessage());
        
        WebSocketMessage errorMessage = new WebSocketMessage(
            WebSocketMessage.MessageType.ERROR, 
            errorContent, 
            sessionId
        );
        sendWebSocketMessage(session, errorMessage);
    }

    /**
     * 处理其他类型的消息
     */
    private void handleOtherMessageTypes(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        
        switch (message.getType()) {
            case DISCONNECT:
                logger.info("收到断开连接请求: {}", sessionId);
                try {
                    session.close();
                } catch (IOException e) {
                    logger.error("关闭WebSocket会话时出错: {}", e.getMessage(), e);
                }
                break;
            default:
                logger.warn("未知的消息类型: {}", message.getType());
                Map<String, Object> errorContent = new HashMap<>();
                errorContent.put("error", "不支持的消息类型: " + message.getType());
                
                WebSocketMessage errorMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.ERROR, 
                    errorContent, 
                    sessionId
                );
                sendWebSocketMessage(session, errorMessage);
        }
    }

    /**
     * 发送WebSocket消息给指定会话
     */
    private void sendWebSocketMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将流式数据块转换为响应
     * @param chunk 流式数据块
     * @param sessionId 会话ID
     * @return 流式响应
     */
    private StreamingResponse convertChunkToResponse(StreamingChatService.StreamingChunk chunk, String sessionId) {
        StreamingResponse response = new StreamingResponse();
        response.setSessionId(sessionId);
        
        switch (chunk.getChunkType()) {
            case CONTENT:
                response.setChunkType("content");
                response.setContent(chunk.getContent());
                break;
                
            case REASONING:
                response.setChunkType("reasoning");
                response.setReasoningContent(chunk.getReasoningContent());
                break;
                
            case TOOL_CALL:
                response.setChunkType("tool_call");
                if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                    StreamingChatService.StreamingChunk.ToolCallInfo toolCallInfo = chunk.getToolCalls().get(0);
                    StreamingResponse.ToolCallInfo toolCall = new StreamingResponse.ToolCallInfo();
                    
                    // 根据函数名设置工具类型和描述
                    String functionName = toolCallInfo.getFunctionName();
                    toolCall.setToolName(functionName);
                    
                    switch (functionName) {
                        case "get_current_time":
                            toolCall.setToolType("TIMETOOL");
                            toolCall.setToolDescription("解析时间");
                            break;
                        case "add_appointment":
                            toolCall.setToolType("ADDAPPOINTMENTTOOL");
                            toolCall.setToolDescription("添加预约记录");
                            break;
                        case "query_reservation_count":
                            toolCall.setToolType("APPOINTMENTCOUNTTOOL");
                            toolCall.setToolDescription("分配资源");
                            break;
                        case "query_resources":
                            toolCall.setToolType("RESOURCEQUERYTOOL");
                            toolCall.setToolDescription("查询资源");
                            break;
                        case "check_availability":
                            toolCall.setToolType("AVAILABILITYCHECKTOOL");
                            toolCall.setToolDescription("检查资源可用性");
                            break;
                        default:
                            toolCall.setToolType("UNKNOWN");
                            toolCall.setToolDescription("未知工具");
                            break;
                    }
                    
                    toolCall.setArguments(toolCallInfo.getArguments());
                    response.setToolCall(toolCall);
                }
                break;
                
            case FINISH:
                response.setChunkType("finish");
                response.setFinished(true);
                response.setFinishReason(chunk.getFinishReason());
                break;
                
            default:
                response.setChunkType("unknown");
                break;
        }
        
        return response;
    }

    /**
     * 发送消息给指定会话（兼容旧版ChatResponse格式）
     */
    private void sendMessage(WebSocketSession session, ChatResponse response) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessage.MessageType.CHAT, 
            response, 
            session.getId()
        );
        sendWebSocketMessage(session, message);
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 获取所有活跃会话ID
     */
    public Map<String, WebSocketSession> getActiveSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}