package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authoritys")
public class Authority {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "tag", nullable = false, length = 255)
    private String tag; // 权限标签
    
    @Column(name = "appointment", nullable = false)
    private int appointment; // 预约管理权限，1表示有操作权限，0表示无操作权限
    
    @Column(name = "public-info", nullable = false)
    private int publicInfo; // 公共信息管理权限，1表示有操作权限，0表示无操作权限
    
    @Column(name = "account-mgm", nullable = false)
    private int accountMgm; // 账号管理权限，1表示有操作权限，0表示无操作权限
    
    @Column(name = "resource-mgm", nullable = false)
    private int resourceMgm; // 资源管理权限，1表示有操作权限，0表示无操作权限
    
    @Column(name = "history", nullable = false)
    private int history; // 历史记录权限，1表示有操作权限，0表示无操作权限
    
    @Column(name = "`check`", nullable = false)
    private int check; // 审核权限，1表示有操作权限，0表示无操作权限
    
    // 构造函数
    public Authority() {
    }
    
    public Authority(String tag, int appointment, int publicInfo, int accountMgm, int resourceMgm, int history, int check) {
        this.tag = tag;
        this.appointment = appointment;
        this.publicInfo = publicInfo;
        this.accountMgm = accountMgm;
        this.resourceMgm = resourceMgm;
        this.history = history;
        this.check = check;
    }
    
    public Authority(Long id, String tag, int appointment, int publicInfo, int accountMgm, int resourceMgm, int history, int check) {
        this.id = id;
        this.tag = tag;
        this.appointment = appointment;
        this.publicInfo = publicInfo;
        this.accountMgm = accountMgm;
        this.resourceMgm = resourceMgm;
        this.history = history;
        this.check = check;
    }
    
    // getter和setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public int getAppointment() {
        return appointment;
    }
    
    public void setAppointment(int appointment) {
        this.appointment = appointment;
    }
    
    public int getPublicInfo() {
        return publicInfo;
    }
    
    public void setPublicInfo(int publicInfo) {
        this.publicInfo = publicInfo;
    }
    
    public int getAccountMgm() {
        return accountMgm;
    }
    
    public void setAccountMgm(int accountMgm) {
        this.accountMgm = accountMgm;
    }
    
    public int getResourceMgm() {
        return resourceMgm;
    }
    
    public void setResourceMgm(int resourceMgm) {
        this.resourceMgm = resourceMgm;
    }
    
    public int getHistory() {
        return history;
    }
    
    public void setHistory(int history) {
        this.history = history;
    }
    
    public int getCheck() {
        return check;
    }
    
    public void setCheck(int check) {
        this.check = check;
    }
}