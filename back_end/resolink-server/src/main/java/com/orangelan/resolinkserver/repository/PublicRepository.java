package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.Public;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicRepository extends JpaRepository<Public, Long> {
    // 可以在这里添加自定义查询方法
}