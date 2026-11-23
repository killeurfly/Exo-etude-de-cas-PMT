package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès à la table {@code projects}. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
    /** Charge plusieurs projets en une requête, du plus récent au plus ancien. */
    List<Project> findByIdInOrderByCreatedAtDesc(List<Long> ids);
}
