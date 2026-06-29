package com.orangelan.resolinkserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class KeyPairService {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    
    // 密钥对过期时间（毫秒），默认5分钟
    private static final long KEY_PAIR_EXPIRY_TIME = TimeUnit.MINUTES.toMillis(5);
    
    // 客户端密钥对存储，使用ConcurrentHashMap确保线程安全
    private final Map<String, KeyPairEntry> clientKeyPairs = new ConcurrentHashMap<>();
    
    /**
     * 密钥对条目，包含密钥对和创建时间
     */
    private static class KeyPairEntry {
        private final KeyPair keyPair;
        private final long createTime;
        
        public KeyPairEntry(KeyPair keyPair) {
            this.keyPair = keyPair;
            this.createTime = System.currentTimeMillis();
        }
        
        public KeyPair getKeyPair() {
            return keyPair;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createTime > KEY_PAIR_EXPIRY_TIME;
        }
    }
    
    /**
     * 生成新的密钥对
     * @return 生成的密钥对
     */
    private KeyPair generateNewKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom secureRandom = new SecureRandom();
            keyPairGenerator.initialize(KEY_SIZE, secureRandom);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥对失败", e);
        }
    }
    
    /**
     * 为指定客户端获取公钥
     * @param clientId 客户端标识
     * @return Base64编码的公钥字符串
     * @throws IllegalArgumentException 如果clientId无效
     */
    public String getPublicKeyForClient(String clientId) {
        // 验证clientId格式
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("客户端ID不能为空");
        }
        
        // 清理过期的密钥对
        cleanupExpiredKeyPairs();
        
        String finalClientId = clientId.trim();
        KeyPairEntry entry = clientKeyPairs.get(finalClientId);
        
        try {
            // 如果客户端没有密钥对或密钥对已过期，生成新的密钥对
            if (entry == null || entry.isExpired()) {
                KeyPair newKeyPair = generateNewKeyPair();
                entry = new KeyPairEntry(newKeyPair);
                clientKeyPairs.put(finalClientId, entry);
            }
            
            PublicKey publicKey = entry.getKeyPair().getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();
            return Base64.getEncoder().encodeToString(publicKeyBytes);
        } catch (Exception e) {
            throw new RuntimeException("获取公钥失败，请稍后重试", e);
        }
    }
    
    /**
     * 为指定客户端获取私钥（用于内部解密）
     * @param clientId 客户端标识
     * @return 私钥对象
     * @throws IllegalArgumentException 如果clientId无效
     * @throws RuntimeException 如果客户端密钥对不存在或已过期
     */
    public PrivateKey getPrivateKeyForClient(String clientId) {
        // 验证clientId格式
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("客户端ID不能为空");
        }
        
        String finalClientId = clientId.trim();
        KeyPairEntry entry = clientKeyPairs.get(finalClientId);
        
        if (entry == null) {
            throw new RuntimeException("客户端密钥对不存在，请重新获取公钥");
        }
        
        if (entry.isExpired()) {
            clientKeyPairs.remove(finalClientId); // 移除过期的密钥对
            throw new RuntimeException("客户端密钥对已过期，请重新获取公钥");
        }
        
        try {
            PrivateKey privateKey = entry.getKeyPair().getPrivate();
            if (privateKey == null) {
                throw new RuntimeException("私钥获取失败，请重新获取公钥");
            }
            return privateKey;
        } catch (Exception e) {
            throw new RuntimeException("私钥获取失败，请稍后重试", e);
        }
    }
    
    /**
     * 清理所有过期的密钥对
     */
    private void cleanupExpiredKeyPairs() {
        clientKeyPairs.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}