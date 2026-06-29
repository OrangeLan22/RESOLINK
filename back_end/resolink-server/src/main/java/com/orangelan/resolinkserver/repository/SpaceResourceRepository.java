package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.SpaceResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 空间资源仓库接口
 * 用于空间资源的数据访问操作
 */
@Repository
public interface SpaceResourceRepository extends JpaRepository<SpaceResource, Long> {
}