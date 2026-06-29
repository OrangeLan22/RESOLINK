package com.orangelan.resolinkserver.dto;

import com.orangelan.resolinkserver.entity.UserInfo;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfo userInfo;
    private int authority; // 权限字段，Admin用户为99
    
    // 构造函数
    public LoginResponse() {
    }
    
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public LoginResponse(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }
    
    public LoginResponse(boolean success, String message, String token, UserInfo userInfo) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userInfo = userInfo;
        this.authority = 0; // 默认权限为0
    }
    
    public LoginResponse(boolean success, String message, String token, UserInfo userInfo, int authority) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userInfo = userInfo;
        this.authority = authority;
    }
    
    // getter和setter
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserInfo getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
    
    public int getAuthority() {
        return authority;
    }
    
    public void setAuthority(int authority) {
        this.authority = authority;
    }
}