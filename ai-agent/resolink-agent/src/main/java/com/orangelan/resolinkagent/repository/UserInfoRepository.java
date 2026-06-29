package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    
    /**
     * 根据用户账号查询用户信息
     * @param useracc 用户账号
     * @return 用户信息
     */
    @Query("SELECT u FROM UserInfo u JOIN UserAccount a ON u.empId = a.empId WHERE a.useracc = :useracc")
    Optional<UserInfo> findByUseracc(@Param("useracc") String useracc);
    
    /**
     * 根据员工ID查询用户信息
     * @param empId 员工ID
     * @return 用户信息
     */
    Optional<UserInfo> findByEmpId(String empId);
}