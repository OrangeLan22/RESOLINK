package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "physical_resource")
public class PhysicalResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "equipment_name", nullable = false, length = 255)
    private String equipmentName;
    
    @Column(name = "location", nullable = false, length = 255)
    private String location;
    
    @Column(name = "type", nullable = false, length = 255)
    private String type;
    
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
    
    @Column(name = "tag", nullable = true, length = 255)
    private String tag;
    
    // 构造函数
    public PhysicalResource() {
    }
    
    // getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
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
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
}