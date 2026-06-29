package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.UserInfo;
import com.orangelan.resolinkserver.service.LarkMessageService;
import com.orangelan.resolinkserver.service.LarkAuthService;
import com.orangelan.resolinkserver.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lark")
public class LarkMessageController {
    
    @Autowired
    private LarkMessageService larkMessageService;
    
    @Autowired
    private LarkAuthService larkAuthService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     * 发送飞书文本消息
     * @param requestMap 包含userId和content的请求参数
     * @return 发送结果
     */
    @PostMapping("/sendTextMessage")
    public Map<String, Object> sendTextMessage(@RequestBody Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String content = requestMap.get("content");
        
        boolean success = larkMessageService.sendTextMessage(userId, content);
        
        return Map.of(
            "success", success,
            "message", success ? "消息发送成功" : "消息发送失败"
        );
    }
    
    /**
     * 发送飞书卡片消息
     * @param requestMap 包含userId和cardContent的请求参数
     * @return 发送结果
     */
    @PostMapping("/sendCardMessage")
    public Map<String, Object> sendCardMessage(@RequestBody Map<String, String> requestMap) {
        String userId = requestMap.get("userId");
        String cardContent = requestMap.get("cardContent");
        
        boolean success = larkMessageService.sendCardMessage(userId, cardContent);
        
        return Map.of(
            "success", success,
            "message", success ? "卡片消息发送成功" : "卡片消息发送失败"
        );
    }
    
    /**
     * 生成飞书授权URL
     * @param token 请求头中的token
     * @return 授权URL
     */
    @PostMapping("/generateAuthUrl")
    public Map<String, Object> generateAuthUrl(@RequestHeader("token") String token) {
        try {
            // 从token中解析用户名
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return Map.of(
                    "success", false,
                    "message", "无效的token"
                );
            }
            
            // 根据username查找用户信息
            UserInfo userInfo = larkAuthService.findUserInfoByUserAccount(username);
            if (userInfo == null) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }
            
            String state = larkAuthService.generateState();
            
            // 将用户ID和state关联存储（可以使用Redis等）
            // 这里简化处理，直接将用户ID作为state的一部分
            String stateWithUserId = state + "_" + userInfo.getId();
            
            String authUrl = larkAuthService.generateAuthUrl(stateWithUserId);
            
            return Map.of(
                "success", true,
                "authUrl", authUrl
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "生成授权URL失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 处理飞书授权回调
     * @param code 飞书授权码
     * @param state 状态参数
     * @return 绑定结果
     */
    @GetMapping("/authCallback")
    public Map<String, Object> authCallback(@RequestParam("code") String code, @RequestParam("state") String state) {
        try {
            // 从state中提取用户ID
            String[] parts = state.split("_");
            if (parts.length < 2) {
                return Map.of(
                    "success", false,
                    "message", "无效的state参数"
                );
            }
            
            Long userId = Long.parseLong(parts[1]);
            boolean success = larkAuthService.handleAuthCallback(code, userId);
            
            return Map.of(
                "success", success,
                "message", success ? "飞书账号绑定成功,刷新首页即可更新数据" : "飞书账号绑定失败"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "处理授权回调失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 解除飞书账号绑定
     * @param token 请求头中的token
     * @return 解除绑定结果
     */
    @PostMapping("/unbind")
    public Map<String, Object> unbindFeishuAccount(@RequestHeader("token") String token) {
        try {
            // 从token中解析用户名
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return Map.of(
                    "success", false,
                    "message", "无效的token"
                );
            }
            
            // 根据username查找用户信息
            UserInfo userInfo = larkAuthService.findUserInfoByUserAccount(username);
            if (userInfo == null) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }
            
            boolean success = larkAuthService.unbindFeishuAccount(userInfo.getId());
            
            return Map.of(
                "success", success,
                "message", success ? "飞书账号解除绑定成功" : "飞书账号解除绑定失败"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "解除绑定失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取用户飞书绑定状态
     * @param token 请求头中的token
     * @return 绑定状态
     */
    @PostMapping("/getBindStatus")
    public Map<String, Object> getBindStatus(@RequestHeader("token") String token) {
        try {
            // 从token中解析用户名
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return Map.of(
                    "success", false,
                    "message", "无效的token"
                );
            }
            
            // 根据username查找用户信息
            UserInfo userInfo = larkAuthService.findUserInfoByUserAccount(username);
            if (userInfo == null) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }
            
            String feishuUserId = larkAuthService.getFeishuUserIdByUserId(userInfo.getId());
            
            return Map.of(
                "success", true,
                "isBound", feishuUserId != null,
                "feishuUserId", feishuUserId
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "获取绑定状态失败: " + e.getMessage()
            );
        }
    }
}