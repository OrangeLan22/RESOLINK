package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "public")
public class Public {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "companyname", nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String companyName;
    
    // 构造函数、getter和setter
    public Public() {
    }
    
    public Public(String companyName) {
        this.companyName = companyName;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}