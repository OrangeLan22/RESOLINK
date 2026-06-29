package com.orangelan.resolinkserver.dto;

public class LoginRequest {
    private String username;
    private String password;
    private String clientId; // 客户端标识，用于获取对应的私钥解密密码
    
    // getter和setter
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}