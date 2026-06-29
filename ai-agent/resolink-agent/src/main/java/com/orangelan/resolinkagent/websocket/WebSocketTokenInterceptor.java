package com.orangelan.resolinkagent.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.service.TokenValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketTokenInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTokenInterceptor.class);
    
    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;

    public WebSocketTokenInterceptor(TokenValidationService tokenValidationService, ObjectMapper objectMapper) {
        this.tokenValidationService = tokenValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        logger.info("WebSocket握手请求: {}", request.getURI());
        
        // 从查询参数中获取token
        String query = request.getURI().getQuery();
        String token = extractTokenFromQuery(query);
        
        if (token == null || token.isEmpty()) {
            logger.warn("WebSocket连接缺少token");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        
        // 验证token
        try {
            Boolean isValid = tokenValidationService.validateToken(token).block();
            
            if (Boolean.TRUE.equals(isValid)) {
                logger.info("Token验证成功，允许WebSocket连接");
                // 将token存储在attributes中，供WebSocket处理器使用
                attributes.put("token", token);
                return true;
            } else {
                logger.warn("Token验证失败，拒绝WebSocket连接");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Token验证过程中发生错误: {}", e.getMessage(), e);
            response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                              WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后调用，可以在这里进行一些清理工作
        if (exception != null) {
            logger.error("WebSocket握手过程中发生错误: {}", exception.getMessage(), exception);
        } else {
            logger.info("WebSocket握手完成: {}", request.getURI());
        }
    }

    /**
     * 从查询字符串中提取token参数
     */
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        
        return null;
    }
}