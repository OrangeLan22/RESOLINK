package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    /**
     * 根据用户账号查询用户账户信息
     * @param useracc 用户账号
     * @return 用户账户信息
     */
    Optional<UserAccount> findByUseracc(String useracc);
    
    /**
     * 根据员工ID查询用户账户信息
     * @param empId 员工ID
     * @return 用户账户信息
     */
    Optional<UserAccount> findByEmpId(String empId);
}