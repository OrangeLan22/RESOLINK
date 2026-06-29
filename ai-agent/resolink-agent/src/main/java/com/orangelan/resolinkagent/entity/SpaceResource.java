package com.orangelan.resolinkagent.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "space_resource")
public class SpaceResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "space_name", nullable = false, length = 255)
    private String spaceName;
    
    @Column(name = "location", nullable = false, length = 255)
    private String location;
    
    @Column(name = "type", nullable = false, length = 255)
    private String type;
    
    @Column(name = "capacity", nullable = false)
    private Integer capacity;
    
    @Column(name = "`public`", nullable = false)
    private Integer publicFlag;
    
    @Column(name = "dep_id", nullable = true, length = 255)
    private String depId;
    
    @Column(name = "`check`", nullable = false)
    private Integer checkFlag;
    
    @Column(name = "note", nullable = false, columnDefinition = "text")
    private String note;
    
    @Column(name = "stage", nullable = false)
    private Integer stage = 0;
    
    // 构造函数
    public SpaceResource() {
    }
    
    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getPublicFlag() {
        return publicFlag;
    }

    public void setPublicFlag(Integer publicFlag) {
        this.publicFlag = publicFlag;
    }

    public String getDepId() {
        return depId;
    }

    public void setDepId(String depId) {
        this.depId = depId;
    }

    public Integer getCheckFlag() {
        return checkFlag;
    }

    public void setCheckFlag(Integer checkFlag) {
        this.checkFlag = checkFlag;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }
}