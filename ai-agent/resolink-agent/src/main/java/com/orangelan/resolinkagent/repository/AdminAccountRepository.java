package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {
    
    /**
     * 根据管理员账号查询管理员账户
     * @param adacc 管理员账号
     * @return 管理员账户
     */
    Optional<AdminAccount> findByAdacc(String adacc);
    
    /**
     * 根据员工ID查询管理员账户
     * @param empId 员工ID
     * @return 管理员账户
     */
    Optional<AdminAccount> findByEmpId(String empId);
}