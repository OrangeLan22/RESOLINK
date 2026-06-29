package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.service.LarkAuthService;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lark")
public class LarkController {
    
    @Autowired
    private LarkAuthService larkAuthService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     * 获取绑定的飞书用户信息
     * @return 飞书用户信息
     */
    @GetMapping("/user-info")
    public ResponseEntity<?> getLarkUserInfo(@RequestHeader("token") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从token中解析用户名
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                response.put("success", false);
                response.put("message", "无效的token");
                return ResponseEntity.ok(response);
            }
            
            // 根据用户名获取用户ID
            Long userId = larkAuthService.findUserInfoByUserAccount(username).getId();
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> userInfo = larkAuthService.getLarkUserInfo(userId);
            
            if (userInfo != null) {
                response.put("success", true);
                response.put("message", "获取飞书用户信息成功");
                response.put("data", userInfo);
            } else {
                response.put("success", false);
                response.put("message", "获取飞书用户信息失败，用户可能未绑定飞书账号");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取飞书用户信息异常: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}