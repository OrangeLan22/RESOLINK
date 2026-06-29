package com.orangelan.resolinkagent.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "adminaccount")
public class AdminAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    
    @Column(name = "adacc", nullable = false, unique = true)
    private String adacc;
    
    @Column(name = "adpass", nullable = false)
    private String adpass;
    
    @Column(name = "salt", nullable = false, length = 8)
    private String salt;
    
    @Column(name = "emp_id", nullable = false, length = 255)
    private String empId;
    
    // 构造函数、getter和setter
    public AdminAccount() {
    }
    
    public AdminAccount(String adacc, String adpass, String salt, String empId) {
        this.adacc = adacc;
        this.adpass = adpass;
        this.salt = salt;
        this.empId = empId;
    }
    
    public AdminAccount(int id, String adacc, String adpass, String salt, String empId) {
        this.id = id;
        this.adacc = adacc;
        this.adpass = adpass;
        this.salt = salt;
        this.empId = empId;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getEmpId() {
        return empId;
    }
    
    public void setEmpId(String empId) {
        this.empId = empId;
    }
    
    public String getAdacc() {
        return adacc;
    }
    
    public void setAdacc(String adacc) {
        this.adacc = adacc;
    }
    
    public String getAdpass() {
        return adpass;
    }
    
    public void setAdpass(String adpass) {
        this.adpass = adpass;
    }
    
    public String getSalt() {
        return salt;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
}