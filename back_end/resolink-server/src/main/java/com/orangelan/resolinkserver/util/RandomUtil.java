package com.orangelan.resolinkserver.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * 随机生成工具类
 * 提供生成随机字符串、盐和密码哈希的功能
 */
public class RandomUtil {
    
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * 生成指定长度的随机英文字母字符串
     * @param length 字符串长度
     * @return 随机英文字母字符串
     */
    public static String generateRandomAlphabet(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成指定长度的随机字母数字混合字符串
     * @param length 字符串长度
     * @return 随机字母数字混合字符串
     */
    public static String generateRandomAlphanumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
    
    /**
     * 使用SHA-512算法对密码+盐进行哈希运算
     * @param password 密码
     * @param salt 盐
     * @return 哈希后的密码（十六进制格式）
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("盐不能为空");
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(password.getBytes());
            byte[] hashedBytes = md.digest(salt.getBytes());
            
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                String hex = Integer.toHexString(0xff & hashedByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512算法不可用", e);
        }
    }
    
    /**
     * 生成8位随机英文字母作为盐
     * @return 8位随机盐
     */
    public static String generateSalt() {
        return generateRandomAlphabet(8);
    }
    
    /**
     * 生成8位随机字母数字混合作为默认密码
     * @return 8位随机密码
     */
    public static String generateDefaultPassword() {
        return generateRandomAlphanumeric(8);
    }
}