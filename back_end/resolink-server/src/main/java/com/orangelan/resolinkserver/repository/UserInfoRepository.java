package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    
    @Query("SELECT u FROM UserInfo u WHERE u.empId = :empId")
    UserInfo findByEmpId(@Param("empId") String empId);
    
    @Query("SELECT u FROM UserInfo u WHERE u.feishuUserId = :feishuUserId")
    UserInfo findByFeishuUserId(@Param("feishuUserId") String feishuUserId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserInfo u WHERE u.empId = :empId")
    boolean existsByEmpId(@Param("empId") String empId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserInfo u WHERE u.feishuUserId = :feishuUserId")
    boolean existsByFeishuUserId(@Param("feishuUserId") String feishuUserId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserInfo u WHERE u.staId = :staId")
    boolean existsByStaId(@Param("staId") Long staId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserInfo u WHERE u.authId = :authId")
    boolean existsByAuthId(@Param("authId") Long authId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserInfo u WHERE u.depId = :depId")
    boolean existsByDepId(@Param("depId") Long depId);
    
    @Modifying
    @Query("UPDATE UserInfo u SET u.feishuUserId = :feishuUserId WHERE u.id = :id")
    void updateFeishuUserIdById(@Param("feishuUserId") String feishuUserId, @Param("id") Long id);
}