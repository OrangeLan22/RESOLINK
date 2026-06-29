package com.orangelan.resolinkagent.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "appointment")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "emp_id", nullable = false, length = 255)
    private String empId;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "res_id", nullable = false)
    private Long resId;
    
    @Column(name = "res_name", nullable = false, length = 255)
    private String resName;
    
    @Column(name = "type", nullable = false, length = 255)
    private String type;
    
    @Column(name = "appointment_date", nullable = false)
    private Date appointmentDate;
    
    @Column(name = "start_time", nullable = false, length = 255)
    private String startTime;
    
    @Column(name = "end_time", nullable = false, length = 255)
    private String endTime;
    
    @Column(name = "status", nullable = false, length = 255)
    private String status;
    
    @Column(name = "approval")
    private Integer approval;
    
    @Column(name = "`check`")
    private Integer check;
    
    // 非数据库字段，用于返回给前端
    @Transient
    private String depName;
    
    @Transient
    private String staName;
    
    // 构造函数
    public Appointment() {
        this.approval = 0;
        this.check = 0;
    }
    
    public Appointment(String empId, String name, Long resId, String resName, String type,
                       Date appointmentDate, String startTime, String endTime, String status) {
        this.empId = empId;
        this.name = name;
        this.resId = resId;
        this.resName = resName;
        this.type = type;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.approval = 0;
        this.check = 0;
    }
    
    public Appointment(Long id, String empId, String name, Long resId, String resName, String type,
                       Date appointmentDate, String startTime, String endTime, String status, String depName, String staName) {
        this.id = id;
        this.empId = empId;
        this.name = name;
        this.resId = resId;
        this.resName = resName;
        this.type = type;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.depName = depName;
        this.staName = staName;
        this.approval = 0;
        this.check = 0;
    }
    
    // getter和setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    

    
    public Long getResId() {
        return resId;
    }
    
    public void setResId(Long resId) {
        this.resId = resId;
    }
    
    public String getResName() {
        return resName;
    }
    
    public void setResName(String resName) {
        this.resName = resName;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Date getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public Integer getApproval() {
        return approval;
    }
    
    public void setApproval(Integer approval) {
        this.approval = approval;
    }
    
    public Integer getCheck() {
        return check;
    }
    
    public void setCheck(Integer check) {
        this.check = check;
    }
}