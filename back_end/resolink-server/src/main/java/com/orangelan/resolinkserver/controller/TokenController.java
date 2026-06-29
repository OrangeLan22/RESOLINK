package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class TokenController {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);
    private final JwtTokenUtil jwtTokenUtil;

    public TokenController(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 验证Token有效性API
     * @param request 要验证的JWT Token
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取请求中的token
            String token = request.get("token");
            
            // 验证token是否存在
            if (token == null || token.trim().isEmpty()) {
                logger.error("Token验证失败：Token不能为空");
                response.put("success", false);
                response.put("message", "Token不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // 验证token有效性
            boolean isValid = jwtTokenUtil.validateToken(token.trim());
            
            if (isValid) {
                // 如果token有效，获取用户名
                String username = jwtTokenUtil.getUsernameFromToken(token.trim());
                logger.info("Token验证成功，用户：{}", username);
                response.put("success", true);
                response.put("message", "Token有效");
                response.put("username", username);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Token验证失败：Token无效");
                response.put("success", false);
                response.put("message", "Token无效或已过期");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (RuntimeException e) {
            logger.error("Token验证失败：业务逻辑错误", e);
            response.put("success", false);
            response.put("message", "Token验证失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("Token验证失败：系统错误", e);
            response.put("success", false);
            response.put("message", "Token验证失败：系统内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}