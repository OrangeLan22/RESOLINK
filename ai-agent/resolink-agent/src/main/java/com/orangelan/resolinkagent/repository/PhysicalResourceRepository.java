package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.PhysicalResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhysicalResourceRepository extends JpaRepository<PhysicalResource, Long> {
    
    /**
     * 查询可用的物理资源
     * @param type 资源类型
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != -1 AND p.publicFlag = 1 AND p.type = :type")
    List<PhysicalResource> findPublicResourcesByType(@Param("type") String type);
    
    /**
     * 查询部门可用的物理资源
     * @param type 资源类型
     * @param depId 部门ID
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != -1 AND p.publicFlag = 0 AND (p.depId = :depId OR p.depId LIKE CONCAT('%', :depId, '%')) AND p.type = :type")
    List<PhysicalResource> findDepartmentResourcesByType(@Param("type") String type, @Param("depId") String depId);
    
    /**
     * 查询所有可用的物理资源（管理员使用）
     * @param stage 资源状态
     * @param type 资源类型
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != :stage AND p.type = :type")
    List<PhysicalResource> findAllByStageNotAndType(@Param("stage") Integer stage, @Param("type") String type);
    
    /**
     * 查询所有可用的物理资源（管理员使用）
     * @param stage 资源状态
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != :stage")
    List<PhysicalResource> findAllByStageNot(@Param("stage") Integer stage);
    
    /**
     * 查询可用的物理资源
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != -1 AND p.publicFlag = 1")
    List<PhysicalResource> findPublicResources();
    
    /**
     * 查询部门可用的物理资源
     * @param depId 部门ID
     * @return 可用的物理资源列表
     */
    @Query("SELECT p FROM PhysicalResource p WHERE p.stage != -1 AND p.publicFlag = 0 AND (p.depId = :depId OR p.depId LIKE CONCAT('%', :depId, '%'))")
    List<PhysicalResource> findDepartmentResources(@Param("depId") String depId);
}