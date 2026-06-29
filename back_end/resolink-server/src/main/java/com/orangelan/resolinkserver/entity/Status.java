package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "status")
public class Status {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "sta_name", nullable = false, length = 255)
    private String staName;
    
    // 与Department的多对一关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @JsonIgnore
    private Department department;
    
    // 构造函数
    public Status() {
    }
    
    public Status(String staName, Department department) {
        this.staName = staName;
        this.department = department;
    }
    
    public Status(Long id, String staName, Department department) {
        this.id = id;
        this.staName = staName;
        this.department = department;
    }
    
    // getter和setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStaName() {
        return staName;
    }
    
    public void setStaName(String staName) {
        this.staName = staName;
    }
    
    public Department getDepartment() {
        return department;
    }
    
    public void setDepartment(Department department) {
        this.department = department;
    }
}