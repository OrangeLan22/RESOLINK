package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.dto.PublicKeyResponse;
import com.orangelan.resolinkserver.service.KeyPairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 加密控制器
 * 处理公钥获取等加密相关API
 */
@RestController
@RequestMapping("/api/encryption")
public class EncryptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionController.class);
    private final KeyPairService keyPairService;

    public EncryptionController(KeyPairService keyPairService) {
        this.keyPairService = keyPairService;
    }

    /**
     * 获取公钥接口（为每个客户端分配独立密钥对）
     * @param clientId 客户端标识，如果不提供则生成一个新的UUID
     * @return 公钥响应，包含公钥和客户端标识
     */
    @GetMapping("/public-key")
    public PublicKeyResponse getPublicKey(@RequestParam(required = false) String clientId) {
        try {
            // 如果客户端没有提供标识，生成一个新的UUID
            String finalClientId = (clientId == null || clientId.trim().isEmpty()) ? UUID.randomUUID().toString() : clientId.trim();
            logger.info("客户端请求获取公钥，clientId：{}", finalClientId);
            
            // 为该客户端获取公钥（如果不存在则生成新的密钥对）
            String publicKey = keyPairService.getPublicKeyForClient(finalClientId);
            
            logger.info("公钥获取成功，clientId：{}", finalClientId);
            return new PublicKeyResponse(true, publicKey, "公钥获取成功", finalClientId);
        } catch (IllegalArgumentException e) {
            logger.error("公钥获取失败：参数验证错误", e);
            return new PublicKeyResponse(false, null, "公钥获取失败：参数格式错误");
        } catch (RuntimeException e) {
            logger.error("公钥获取失败：业务逻辑错误", e);
            return new PublicKeyResponse(false, null, "公钥获取失败：" + e.getMessage());
        } catch (Exception e) {
            logger.error("公钥获取失败：系统错误", e);
            return new PublicKeyResponse(false, null, "公钥获取失败：系统内部错误，请稍后重试");
        }
    }
}