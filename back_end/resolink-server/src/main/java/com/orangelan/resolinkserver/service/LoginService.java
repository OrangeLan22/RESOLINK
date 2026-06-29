package com.orangelan.resolinkserver.service;

import com.orangelan.resolinkserver.entity.AdminAccount;
import com.orangelan.resolinkserver.entity.UserAccount;
import com.orangelan.resolinkserver.repository.AdminAccountRepository;
import com.orangelan.resolinkserver.repository.UserAccountRepository;
import com.orangelan.resolinkserver.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

@Service
public class LoginService {
    
    private final AdminAccountRepository adminAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final KeyPairService keyPairService;

    public LoginService(AdminAccountRepository adminAccountRepository, UserAccountRepository userAccountRepository, KeyPairService keyPairService) {
        this.adminAccountRepository = adminAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.keyPairService = keyPairService;
    }
    
    /**
     * 用户登录验证
     * @param username 用户名
     * @param encryptedPassword 加密后的密码
     * @param clientId 客户端标识
     * @return 登录结果
     */
    /**
     * 根据用户名获取管理员账户信息
     * @param username 用户名
     * @return 管理员账户信息
     */
    public AdminAccount getAdminAccountByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return adminAccountRepository.findByAdacc(username.trim());
    }
    
    /**
     * 根据用户名获取普通用户账户信息
     * @param username 用户名
     * @return 普通用户账户信息
     */
    public UserAccount getUserAccountByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userAccountRepository.findByUseracc(username.trim());
    }
    
    public boolean login(String username, String encryptedPassword, String clientId) {
        try {
            // 检查用户名是否为空
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            
            // 检查密码是否为空
            if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            
            // 检查客户端ID是否为空
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new RuntimeException("客户端ID不能为空");
            }
            
            // 检查密码格式是否为Base64格式
            if (!isValidBase64(encryptedPassword)) {
                throw new RuntimeException("密码格式错误，请使用有效的加密格式");
            }
            
            try {
                // 第一层判断：检查用户名是否在adminaccount表中
                if (adminAccountRepository.existsByAdacc(username)) {
                    // 获取管理员账户信息
                    AdminAccount adminAccount = adminAccountRepository.findByAdacc(username);
                    if (adminAccount == null) {
                        return false;
                    }
                    
                    // 获取客户端对应的私钥
                    PrivateKey privateKey = keyPairService.getPrivateKeyForClient(clientId);
                    
                    // 解密密码 - 使用客户端对应的私钥
                    String decryptedPassword = EncryptionUtil.decrypt(encryptedPassword, privateKey);
                    
                    // 检查解密后的密码是否为空
                    if (decryptedPassword.trim().isEmpty()) {
                        throw new RuntimeException("密码解密失败，请重新获取公钥加密后重试");
                    }
                    
                    // 将解密后的密码与盐拼接后进行SHA512加密
                    String hashedPassword = hashPassword(decryptedPassword, adminAccount.getSalt());
                    
                    // 比较加密后的密码与数据库中存储的密码

                    return hashedPassword.equals(adminAccount.getAdpass());
                } else if (userAccountRepository.existsByUseracc(username)) {
                    // 第二层判断：检查用户名是否在useraccount表中（普通用户）
                    UserAccount userAccount = userAccountRepository.findByUseracc(username);
                    if (userAccount == null) {
                        return false;
                    }
                    
                    // 获取客户端对应的私钥
                    PrivateKey privateKey = keyPairService.getPrivateKeyForClient(clientId);
                    
                    // 解密密码 - 使用客户端对应的私钥
                    String decryptedPassword = EncryptionUtil.decrypt(encryptedPassword, privateKey);
                    
                    // 检查解密后的密码是否为空
                    if (decryptedPassword.trim().isEmpty()) {
                        throw new RuntimeException("密码解密失败，请重新获取公钥加密后重试");
                    }
                    
                    // 将解密后的密码与盐拼接后进行SHA512加密
                    String hashedPassword = hashPassword(decryptedPassword, userAccount.getSalt());
                    
                    // 比较加密后的密码与数据库中存储的密码
                    return hashedPassword.equals(userAccount.getUserpass());
                } else {
                    // 用户名既不在管理员表也不在普通用户表中
                    return false;
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("密码加密格式错误，请重新获取公钥加密后重试", e);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("客户端密钥对不存在") || e.getMessage().contains("已过期")) {
                    throw new RuntimeException("登录密钥已过期，请重新获取公钥加密后重试", e);
                } else if (e.getMessage().contains("填充错误") || e.getMessage().contains("块大小错误")) {
                    throw new RuntimeException("密码加密错误，请重新获取公钥加密后重试", e);
                }
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("登录失败", e);
        }
    }
    
    /**
     * 验证字符串是否为有效的Base64格式
     * @param str 要验证的字符串
     * @return 是否为有效的Base64格式
     */
    private boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        // Base64格式验证正则表达式
        String base64Pattern = "^[A-Za-z0-9+/]*={0,2}$";
        return str.matches(base64Pattern);
    }
    
    /**
     * 使用SHA512加密密码
     * @param password 原始密码
     * @param salt 盐值
     * @return 加密后的密码
     * @throws IllegalArgumentException 如果密码或盐值无效
     */
    private String hashPassword(String password, String salt) {
        // 验证参数
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        if (salt == null || salt.trim().isEmpty()) {
            throw new IllegalArgumentException("盐值不能为空");
        }
        
        try {
            // 将密码与盐拼接
            String passwordWithSalt = password + salt;
            
            // 创建SHA512消息摘要
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            
            // 计算哈希值
            byte[] hashedBytes = md.digest(passwordWithSalt.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果算法不存在，抛出运行时异常
            throw new RuntimeException("密码加密算法错误", e);
        }
    }
}