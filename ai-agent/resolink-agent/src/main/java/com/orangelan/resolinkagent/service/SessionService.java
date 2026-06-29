package com.orangelan.resolinkagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 会话服务，管理会话ID和历史记录
 */
@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    private static final String SESSION_KEY_PREFIX = "resolink:session:";
    private static final int SESSION_EXPIRE_MINUTES = 30;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取或创建会话ID
     * @param sessionId 客户端提供的会话ID
     * @return 包含会话ID和是否为新会话的Map
     */
    public Map<String, Object> getOrCreateSession(String sessionId) {
        Map<String, Object> result = new HashMap<>();
        boolean isNew = false;
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            // 创建新的会话ID
            sessionId = UUID.randomUUID().toString();
            isNew = true;
            logger.info("创建新的会话ID: {}", sessionId);
        } else {
            // 检查Redis中是否存在该会话
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            Boolean exists = redisTemplate.hasKey(sessionKey);
            
            if (exists == null || !exists) {
                // 会话不存在，创建新的会话ID
                sessionId = UUID.randomUUID().toString();
                isNew = true;
                logger.info("会话ID {} 不存在，创建新的会话ID: {}", sessionId, sessionId);
            } else {
                logger.info("使用现有会话ID: {}", sessionId);
            }
        }
        
        result.put("session_id", sessionId);
        result.put("is_new", isNew);
        
        return result;
    }
    
    /**
     * 获取会话历史记录
     * @param sessionId 会话ID
     * @return 历史记录列表
     */
    public List<Map<String, Object>> getSessionHistory(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String historyJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (historyJson != null && !historyJson.isEmpty()) {
                List<Map<String, Object>> history = objectMapper.readValue(historyJson, 
                    new TypeReference<List<Map<String, Object>>>() {});
                logger.info("获取会话 {} 的历史记录，共 {} 条", sessionId, history.size());
                return history;
            }
        } catch (Exception e) {
            logger.error("获取会话历史记录失败: {}", sessionId, e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 保存会话历史记录
     * @param sessionId 会话ID
     * @param history 历史记录
     */
    public void saveSessionHistory(String sessionId, List<Map<String, Object>> history) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String historyJson = objectMapper.writeValueAsString(history);
            
            redisTemplate.opsForValue().set(sessionKey, historyJson, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
            logger.info("保存会话 {} 的历史记录，共 {} 条", sessionId, history.size());
        } catch (JsonProcessingException e) {
            logger.error("保存会话历史记录失败: {}", sessionId, e);
        }
    }
    
    /**
     * 添加用户消息到会话历史
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param assistantResponse 助手响应
     */
    public void addMessagesToSession(String sessionId, String message, String assistantResponse) {
        List<Map<String, Object>> history = getSessionHistory(sessionId);
        
        // 添加用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        history.add(userMessage);
        
        // 添加助手响应
        Map<String, Object> assistantMessage = new HashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", assistantResponse);
        history.add(assistantMessage);
        
        // 保存更新后的历史记录
        saveSessionHistory(sessionId, history);
    }
    
    /**
     * 添加完整的对话记录到会话历史（包括工具调用）
     * @param sessionId 会话ID
     * @param messages 完整的消息列表，包括用户消息、助手响应、工具调用和工具结果
     */
    public void addCompleteConversationToSession(String sessionId, List<Map<String, Object>> messages) {
        // 保存完整的历史记录
        saveSessionHistory(sessionId, messages);
    }
    
    /**
     * 清除会话历史记录
     * @param sessionId 会话ID
     */
    public void clearSessionHistory(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);
        logger.info("清除会话 {} 的历史记录", sessionId);
    }
}