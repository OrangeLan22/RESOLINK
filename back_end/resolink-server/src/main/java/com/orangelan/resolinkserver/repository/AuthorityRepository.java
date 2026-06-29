package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    // 根据tag查询权限信息
    Authority findByTag(String tag);
}