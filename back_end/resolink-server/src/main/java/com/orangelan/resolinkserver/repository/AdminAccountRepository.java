package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {
    // 根据用户名查询管理员账户
    AdminAccount findByAdacc(String adacc);
    // 检查用户名是否存在
    boolean existsByAdacc(String adacc);
}