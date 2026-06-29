package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_info")
public class UserInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "auth_id", nullable = false)
    private Long authId;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "dep_id", nullable = false)
    private Long depId;
    
    @Column(name = "sta_id", nullable = false)
    private Long staId;
    
    @Column(name = "emp_id", nullable = false, length = 255)
    private String empId;
    
    @Column(name = "feishu_user_id", length = 255)
    private String feishuUserId;
    
    // 非数据库字段，用于返回给前端
    @Transient
    private String depName;
    
    @Transient
    private String staName;
    
    // 构造函数
    public UserInfo() {
    }
    
    public UserInfo(String name, Long depId, Long staId, String empId, Long authId) {
        this.name = name;
        this.depId = depId;
        this.staId = staId;
        this.empId = empId;
        this.authId = authId;
    }
    
    public UserInfo(Long id, String name, Long depId, Long staId, String empId, Long authId) {
        this.id = id;
        this.name = name;
        this.depId = depId;
        this.staId = staId;
        this.empId = empId;
        this.authId = authId;
    }
    
    public UserInfo(Long id, String name, Long depId, Long staId, String empId, Long authId, String feishuUserId) {
        this.id = id;
        this.name = name;
        this.depId = depId;
        this.staId = staId;
        this.empId = empId;
        this.authId = authId;
        this.feishuUserId = feishuUserId;
    }
    
    public UserInfo(Long id, String name, Long depId, Long staId, String empId, Long authId, String feishuUserId, String depName, String staName) {
        this.id = id;
        this.name = name;
        this.depId = depId;
        this.staId = staId;
        this.empId = empId;
        this.authId = authId;
        this.feishuUserId = feishuUserId;
        this.depName = depName;
        this.staName = staName;
    }
    
    // getter和setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAuthId() {
        return authId;
    }
    
    public void setAuthId(Long authId) {
        this.authId = authId;
    }
    
    public Long getDepId() {
        return depId;
    }
    
    public void setDepId(Long depId) {
        this.depId = depId;
    }
    
    public Long getStaId() {
        return staId;
    }
    
    public void setStaId(Long staId) {
        this.staId = staId;
    }
    
    public String getEmpId() {
        return empId;
    }
    
    public void setEmpId(String empId) {
        this.empId = empId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDepName() {
        return depName;
    }
    
    public void setDepName(String depName) {
        this.depName = depName;
    }
    
    public String getStaName() {
        return staName;
    }
    
    public void setStaName(String staName) {
        this.staName = staName;
    }
    
    public String getFeishuUserId() {
        return feishuUserId;
    }
    
    public void setFeishuUserId(String feishuUserId) {
        this.feishuUserId = feishuUserId;
    }
    

}