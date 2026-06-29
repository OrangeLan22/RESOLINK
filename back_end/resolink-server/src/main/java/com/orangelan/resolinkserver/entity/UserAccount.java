package com.orangelan.resolinkserver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "useraccount")
public class UserAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Column(name = "emp_id", nullable = false, length = 255)
    private String empId;
    
    @Column(name = "useracc", nullable = false, length = 255)
    private String useracc;
    
    @Column(name = "userpass", nullable = false, length = 255)
    private String userpass;
    
    @Column(name = "salt", nullable = false, length = 8)
    private String salt;
    
    @Column(name = "isinitial", nullable = false)
    private int isinitial;
    
    // 构造函数
    public UserAccount() {
    }
    
    public UserAccount(String empId, String useracc, String userpass, String salt, int isinitial) {
        this.empId = empId;
        this.useracc = useracc;
        this.userpass = userpass;
        this.salt = salt;
        this.isinitial = isinitial;
    }
    
    public UserAccount(Long id, String empId, String useracc, String userpass, String salt, int isinitial) {
        this.id = id;
        this.empId = empId;
        this.useracc = useracc;
        this.userpass = userpass;
        this.salt = salt;
        this.isinitial = isinitial;
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
    
    public String getUseracc() {
        return useracc;
    }
    
    public void setUseracc(String useracc) {
        this.useracc = useracc;
    }
    
    public String getUserpass() {
        return userpass;
    }
    
    public void setUserpass(String userpass) {
        this.userpass = userpass;
    }
    
    public String getSalt() {
        return salt;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
    
    public int getIsinitial() {
        return isinitial;
    }
    
    public void setIsinitial(int isinitial) {
        this.isinitial = isinitial;
    }
}