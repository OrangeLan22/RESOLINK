package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    // 根据部门ID和身份名称查询身份
    Status findByDepartmentIdAndStaName(Long departmentId, String staName);
    // 根据状态名称查询状态
    Status findByStaName(String staName);
}