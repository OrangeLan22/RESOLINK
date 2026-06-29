package com.orangelan.resolinkserver.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token工具类
 * 提供生成token和验证token的功能
 */
@Component
public class JwtTokenUtil {

    // 从配置文件中读取Token有效期（毫秒）
    @Value("${jwt.expiration-time}")
    private long expirationTime;

    // 从配置文件中读取用于签名的密钥
    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * 生成JWT Token
     * @param username 登录账号
     * @param authority 权限标识
     * @return 生成的JWT Token
     */
    public String generateToken(String username, int authority) {
        // 输入验证
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 创建JWT声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username); // subject，放入登录账号
        claims.put("iat", new Date()); // 签发时间
        claims.put("exp", new Date(System.currentTimeMillis() + expirationTime)); // 过期时间
        claims.put("authority", authority); // 权限标识

        // 生成Token - 使用最新API
        return Jwts.builder()
                .claims(claims)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 生成JWT Token（重载方法，默认权限为0）
     * @param username 登录账号
     * @return 生成的JWT Token
     */
    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    /**
     * 验证JWT Token
     * @param token 要验证的Token
     * @return Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 解析Token - 使用最新API
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token已过期
            System.out.println("Token已过期: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.SignatureException e) {
            // Token签名错误
            System.out.println("Token签名错误: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // Token格式错误
            System.out.println("Token格式错误: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            // Token不支持
            System.out.println("Token不支持: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他Token无效情况
            System.out.println("Token无效: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取登录账号
     * @param token JWT Token
     * @return 登录账号
     */
    public String getUsernameFromToken(String token) {
        try {
            // 解析Token获取声明 - 使用最新API
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 返回登录账号
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("从Token中获取用户名失败", e);
        }
    }
    
    /**
     * 从Token中获取权限标识
     * @param token JWT Token
     * @return 权限标识
     */
    public int getAuthorityFromToken(String token) {
        try {
            // 解析Token获取声明 - 使用最新API
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 返回权限标识
            return (int) claims.get("authority");
        } catch (Exception e) {
            throw new RuntimeException("从Token中获取权限标识失败", e);
        }
    }

    /**
     * 获取用于签名的密钥
     * @return 签名密钥
     */
    private Key getSigningKey() {
        // 从配置文件中获取密钥
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 检查Token是否过期
     * @param token JWT Token
     * @return Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            // 解析Token获取声明 - 使用最新API
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 检查过期时间
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // Token无效或已过期
            return true;
        }
    }
}