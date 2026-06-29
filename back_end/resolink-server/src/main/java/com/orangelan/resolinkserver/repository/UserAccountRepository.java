package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    // 根据用户名查询用户账户
    UserAccount findByUseracc(String useracc);
    // 根据员工ID查询用户账户
    UserAccount findByEmpId(String empId);
    // 检查用户名是否存在
    boolean existsByUseracc(String useracc);
    // 检查员工ID是否存在
    boolean existsByEmpId(String empId);
}