package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedByUserId(Long createdByUserId);

    List<Project> findByIdInOrderByCreatedAtDesc(List<Long> ids);
}
