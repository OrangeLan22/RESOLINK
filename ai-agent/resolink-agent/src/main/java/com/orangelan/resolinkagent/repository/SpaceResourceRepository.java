package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.SpaceResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceResourceRepository extends JpaRepository<SpaceResource, Long> {
    
    /**
     * 查询可用的空间资源
     * @param type 资源类型
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != -1 AND s.publicFlag = 1 AND s.type = :type")
    List<SpaceResource> findPublicResourcesByType(@Param("type") String type);
    
    /**
     * 查询部门可用的空间资源
     * @param type 资源类型
     * @param depId 部门ID
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != -1 AND s.publicFlag = 0 AND (s.depId = :depId OR s.depId LIKE CONCAT('%', :depId, '%')) AND s.type = :type")
    List<SpaceResource> findDepartmentResourcesByType(@Param("type") String type, @Param("depId") String depId);
    
    /**
     * 查询所有可用的空间资源（管理员使用）
     * @param stage 资源状态
     * @param type 资源类型
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != :stage AND s.type = :type")
    List<SpaceResource> findAllByStageNotAndType(@Param("stage") Integer stage, @Param("type") String type);
    
    /**
     * 查询所有可用的空间资源（管理员使用）
     * @param stage 资源状态
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != :stage")
    List<SpaceResource> findAllByStageNot(@Param("stage") Integer stage);
    
    /**
     * 查询可用的空间资源
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != -1 AND s.publicFlag = 1")
    List<SpaceResource> findPublicResources();
    
    /**
     * 查询部门可用的空间资源
     * @param depId 部门ID
     * @return 可用的空间资源列表
     */
    @Query("SELECT s FROM SpaceResource s WHERE s.stage != -1 AND s.publicFlag = 0 AND (s.depId = :depId OR s.depId LIKE CONCAT('%', :depId, '%'))")
    List<SpaceResource> findDepartmentResources(@Param("depId") String depId);
}