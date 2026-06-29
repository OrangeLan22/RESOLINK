package com.orangelan.resolinkagent.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkagent.dto.ChatRequest;
import com.orangelan.resolinkagent.service.TokenValidationService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class TokenValidationInterceptor implements HandlerInterceptor {
    
    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;
    
    public TokenValidationInterceptor(TokenValidationService tokenValidationService, ObjectMapper objectMapper) {
        this.tokenValidationService = tokenValidationService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只对agent/chat路径进行拦截
        if (!"/agent/chat".equals(request.getRequestURI())) {
            return true;
        }

        // 读取请求体
        String requestBody = getRequestBody(request);
        
        try {
            // 解析请求体获取token
            ChatRequest chatRequest = objectMapper.readValue(requestBody, ChatRequest.class);
            String token = chatRequest.getToken();
            
            if (token == null || token.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"Token is required\"}");
                return false;
            }
            
            // 验证token
            Boolean isValid = tokenValidationService.validateToken(token).block();
            
            if (Boolean.TRUE.equals(isValid)) {
                return true;
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"Invalid token\"}");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\": false, \"message\": \"Token validation failed\"}");
            return false;
        }
    }
    
    private String getRequestBody(HttpServletRequest request) {
        // 尝试从我们的自定义请求包装器中获取请求体
        if (request instanceof com.orangelan.resolinkagent.interceptor.RequestCachingFilter.CachedBodyHttpServletRequest) {
            byte[] buf = ((com.orangelan.resolinkagent.interceptor.RequestCachingFilter.CachedBodyHttpServletRequest) request).getContentAsByteArray();
            
            if (buf.length > 0) {
                try {
                    String body = new String(buf, 0, buf.length, request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");
                    return body;
                } catch (Exception e) {
                    return "";
                }
            }
        }
        
        // 尝试从Spring的ContentCachingRequestWrapper中获取请求体
        ContentCachingRequestWrapper wrappingRequest = 
            WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        
        if (wrappingRequest != null) {
            byte[] buf = wrappingRequest.getContentAsByteArray();
            
            if (buf.length > 0) {
                try {
                    String body = new String(buf, 0, buf.length, request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");
                    return body;
                } catch (UnsupportedEncodingException e) {
                    return "";
                }
            }
        }
        
        return "";
    }
}