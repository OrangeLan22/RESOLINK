package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.PhysicalResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalResourceRepository extends JpaRepository<PhysicalResource, Long> {
}