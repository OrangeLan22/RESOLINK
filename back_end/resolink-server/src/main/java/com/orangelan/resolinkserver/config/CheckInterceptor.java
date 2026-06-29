package com.orangelan.resolinkserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangelan.resolinkserver.entity.Authority;
import com.orangelan.resolinkserver.repository.AuthorityRepository;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class CheckInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckInterceptor.class);
    private final AuthorityRepository authorityRepository;
    private final JwtTokenUtil jwtTokenUtil;
    
    private final ObjectMapper objectMapper;
    
    public CheckInterceptor(AuthorityRepository authorityRepository, JwtTokenUtil jwtTokenUtil) {
        this.authorityRepository = authorityRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.info("拦截到请求：{}", request.getRequestURI());
        logger.info("请求方法：{}", request.getMethod());
        
        if ("OPTIONS".equals(request.getMethod())) {
            logger.info("OPTIONS请求，直接放行");
            return true;
        }
        
        String token;
        if ("GET".equals(request.getMethod())) {
            token = getTokenFromRequestHeader(request, response);
        } else {
            token = getTokenFromRequestBody(request, response);
        }
        
        if (token == null) {
            return false;
        }
        
        if (!validateToken(token, response)) {
            return false;
        }
        
        String tokenValue = token.trim();
        if (tokenValue.startsWith("Bearer ")) {
            tokenValue = tokenValue.substring(7).trim();
        }
        int authorityId = jwtTokenUtil.getAuthorityFromToken(tokenValue);
        logger.info("从Token中获取的权限标识：{}", authorityId);
        
        Optional<Authority> authorityOptional = authorityRepository.findById((long) authorityId);
        if (!authorityOptional.isPresent()) {
            logger.error("权限信息不存在，权限标识：{}", authorityId);
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "权限信息不存在");
            return false;
        }
        
        Authority authority = authorityOptional.get();
        
        if (authority.getCheck() != 1) {
            logger.error("没有操作权限，check权限：{}", authority.getCheck());
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "没有操作权限");
            return false;
        }
        
        logger.info("权限验证通过，允许访问：{}", request.getRequestURI());
        return true;
    }
    
    private String getTokenFromRequestHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getHeader("token");
        
        if (token != null && !token.trim().isEmpty()) {
            logger.info("从请求头中获取到Token");
            return token;
        } else {
            logger.error("请求头中未找到Token");
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "请求头中未找到Token");
            return null;
        }
    }
    
    private String getTokenFromRequestBody(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (request instanceof RequestBodyFilter.RepeatedlyReadRequestWrapper) {
                RequestBodyFilter.RepeatedlyReadRequestWrapper requestWrapper = (RequestBodyFilter.RepeatedlyReadRequestWrapper) request;
                String requestBody = requestWrapper.getRequestBody();
                
                System.out.println("请求体内容：" + requestBody);
                
                if (requestBody != null && !requestBody.trim().isEmpty()) {
                    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);
                    String token = (String) requestMap.get("token");
                    if (token != null && !token.trim().isEmpty()) {
                        logger.info("从请求体中获取到Token");
                        System.out.println("获取到的Token：" + token);
                        return token;
                    }
                }
            }
            
            logger.error("请求体中未找到Token");
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "请求体中未找到Token");
            return null;
        } catch (Exception e) {
            logger.error("解析请求体失败：{}", e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "请求体格式错误");
            return null;
        }
    }
    
    private boolean validateToken(String token, HttpServletResponse response) throws IOException {
        if (token == null || token.trim().isEmpty()) {
            logger.error("Token无效：{}", token);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token无效");
            return false;
        }
        
        String tokenValue = token.trim();
        if (tokenValue.startsWith("Bearer ")) {
            tokenValue = tokenValue.substring(7).trim();
        }
        
        if (!jwtTokenUtil.validateToken(tokenValue)) {
            logger.error("Token无效或已过期");
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token无效或已过期");
            return false;
        }
        
        return true;
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        String errorJson = String.format("{\"success\":false,\"message\":\"%s\"}", message);
        response.getWriter().write(errorJson);
    }
}