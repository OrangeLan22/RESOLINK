package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // 根据部门名称查询部门
    Department findByDepName(String depName);
}