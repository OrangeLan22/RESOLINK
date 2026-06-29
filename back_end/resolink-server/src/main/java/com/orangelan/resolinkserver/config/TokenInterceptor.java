package com.orangelan.resolinkserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Token验证拦截器
 * 用于验证所有/api路径的请求是否具有有效的token
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);
    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;
    
    public TokenInterceptor(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.info("拦截到请求：{}", request.getRequestURI());
        logger.info("请求方法：{}", request.getMethod());
        
        // 对于OPTIONS请求，直接放行（处理CORS预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            logger.info("OPTIONS请求，直接放行");
            return true;
        }
        
        // public-key 接口不需要token验证
        if (request.getRequestURI().contains("public-key")) {
            logger.info("public-key接口，直接放行");
            return true;
        }
        
        // 根据请求方法选择获取Token的方式
        String token;
        if ("GET".equals(request.getMethod())) {
            // GET请求从请求头获取Token
            token = getTokenFromRequestHeader(request, response);
        } else {
            // 其他请求从请求体获取Token
            token = getTokenFromRequestBody(request, response);
        }
        
        if (token == null) {
            return false;
        }
        
        // 验证Token
        if (!validateToken(token, response)) {
            return false;
        }
        
        // Token验证通过
        logger.info("Token验证通过，允许访问：{}", request.getRequestURI());
        return true;
    }
    
    /**
     * 从请求头中获取Token
     * @param request 请求对象
     * @param response 响应对象
     * @return Token字符串或null
     */
    private String getTokenFromRequestHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 从请求头获取Token
        String token = request.getHeader("token");
        
        if (token != null && !token.trim().isEmpty()) {
            logger.info("从请求头中获取到Token");
            return token;
        } else {
            // 如果没有获取到Token，返回错误
            logger.error("请求头中未找到Token");
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "请求头中未找到Token");
            return null;
        }
    }
    
    /**
     * 从请求体中获取Token
     * @param request 请求对象
     * @param response 响应对象
     * @return Token字符串或null
     */
    private String getTokenFromRequestBody(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 检查请求是否是可重复读取的请求包装器
            if (request instanceof RequestBodyFilter.RepeatedlyReadRequestWrapper) {
                RequestBodyFilter.RepeatedlyReadRequestWrapper requestWrapper = (RequestBodyFilter.RepeatedlyReadRequestWrapper) request;
                String requestBody = requestWrapper.getRequestBody();
                
                // 打印请求体内容，用于调试
                System.out.println("请求体内容：" + requestBody);
                
                // 解析请求体
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
            
            // 如果没有获取到Token，返回错误
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
    
    /**
     * 验证Token的有效性
     * @param token 请求体中的Token
     * @param response 响应对象
     * @return Token是否有效
     */
    private boolean validateToken(String token, HttpServletResponse response) throws IOException {
        // 验证Token是否存在
        if (token == null || token.trim().isEmpty()) {
            logger.error("Token无效：{}", token);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token无效");
            return false;
        }
        
        // 提取Token（如果有Bearer前缀则移除）
        String tokenValue = token.trim();
        if (tokenValue.startsWith("Bearer ")) {
            tokenValue = tokenValue.substring(7).trim();
        }
        
        // 验证Token有效性
        if (!jwtTokenUtil.validateToken(tokenValue)) {
            logger.error("Token无效或已过期");
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token无效或已过期");
            return false;
        }
        
        return true;
    }
    
    /**
     * 发送错误响应
     * @param response 响应对象
     * @param status HTTP状态码
     * @param message 错误信息
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        String errorJson = String.format("{\"success\":false,\"message\":\"%s\"}", message);
        response.getWriter().write(errorJson);
    }
}