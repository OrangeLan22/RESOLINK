package com.orangelan.resolinkserver.dto;

public class PublicKeyResponse {
    private boolean success;
    private String publicKey;
    private String message;
    private String clientId; // 客户端标识
    
    // 构造函数
    public PublicKeyResponse() {
    }
    
    public PublicKeyResponse(boolean success, String publicKey, String message) {
        this.success = success;
        this.publicKey = publicKey;
        this.message = message;
    }
    
    public PublicKeyResponse(boolean success, String publicKey, String message, String clientId) {
        this.success = success;
        this.publicKey = publicKey;
        this.message = message;
        this.clientId = clientId;
    }
    
    // getter和setter
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}