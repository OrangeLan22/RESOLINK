package com.orangelan.resolinkserver.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 加密解密工具类
 * 提供RSA加密解密功能
 */
public class EncryptionUtil {
    
    private static final String ALGORITHM = "RSA";
    
    /**
     * 使用私钥解密数据
     * @param encryptedData Base64编码的加密数据
     * @param privateKey 私钥对象
     * @return 解密后的原始数据
     * @throws IllegalArgumentException 如果输入参数无效
     * @throws RuntimeException 如果解密过程中发生错误
     */
    public static String decrypt(String encryptedData, PrivateKey privateKey) {
        // 输入验证
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            throw new IllegalArgumentException("加密数据不能为空");
        }
        
        if (privateKey == null) {
            throw new IllegalArgumentException("私钥不能为空");
        }
        
        try {
            // 验证Base64格式
            if (!isValidBase64(encryptedData)) {
                throw new IllegalArgumentException("加密数据不是有效的Base64格式");
            }
            
            // 填充方式PKCS1Padding，兼容前端JSEncrypt库
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            
            // 初始化Cipher为解密模式
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            // 解码Base64加密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            
            // 验证数据长度是否符合RSA要求
            if (encryptedBytes.length == 0) {
                throw new IllegalArgumentException("加密数据无效，解码后为空");
            }
            
            // 解密数据
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            // 转换为字符串
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (BadPaddingException e) {
            throw new RuntimeException("解密数据失败：填充错误，请检查加密数据和密钥是否匹配", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("解密数据失败：块大小错误，请检查加密数据格式", e);
        } catch (Exception e) {
            throw new RuntimeException("解密数据失败：未知错误", e);
        }
    }
    
    /**
     * 验证字符串是否为有效的Base64格式
     * @param str 要验证的字符串
     * @return 是否为有效的Base64格式
     */
    private static boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        // Base64格式验证正则表达式
        String base64Pattern = "^[A-Za-z0-9+/]*={0,2}$";
        return str.matches(base64Pattern);
    }
    
    /**
     * 使用Base64编码的私钥字符串解密数据
     * @param encryptedData Base64编码的加密数据
     * @param privateKeyStr Base64编码的私钥字符串
     * @return 解密后的原始数据
     */
    public static String decrypt(String encryptedData, String privateKeyStr) {
        try {
            // 解码Base64私钥
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            
            // 创建私钥对象
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            
            // 调用解密方法
            return decrypt(encryptedData, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("使用Base64私钥解密失败", e);
        }
    }
    
    /**
     * 生成RSA密钥对
     * @param keySize 密钥大小，推荐2048
     * @return 生成的密钥对
     */
    public static KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            SecureRandom secureRandom = new SecureRandom();
            keyPairGenerator.initialize(keySize, secureRandom);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥对失败", e);
        }
    }
    
    /**
     * 将公钥转换为Base64编码字符串
     * @param publicKey 公钥对象
     * @return Base64编码的公钥字符串
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        byte[] publicKeyBytes = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }
    
    /**
     * 将私钥转换为Base64编码字符串
     * @param privateKey 私钥对象
     * @return Base64编码的私钥字符串
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        byte[] privateKeyBytes = privateKey.getEncoded();
        return Base64.getEncoder().encodeToString(privateKeyBytes);
    }
}