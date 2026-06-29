package com.orangelan.resolinkagent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "department")
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "dep_name", nullable = false, length = 255)
    private String depName;
    
    // 与Status的一对多关系
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Status> statuses;
    
    // 构造函数
    public Department() {
    }
    
    public Department(String depName) {
        this.depName = depName;
    }
    
    public Department(Long d, String depName) {
        this.depName = depName;
    }
    
    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDepName() {
        return depName;
    }
    
    public void setDepName(String depName) {
        this.depName = depName;
    }
    
    public List<Status> getStatuses() {
        return statuses;
    }
    
    public void setStatuses(List<Status> statuses) {
        this.statuses = statuses;
    }
}