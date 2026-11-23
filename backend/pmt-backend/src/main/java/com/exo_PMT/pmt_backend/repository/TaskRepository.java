package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès à la table {@code tasks}. */
public interface TaskRepository extends JpaRepository<Task, Long> {
    /** Tâches d'un projet, sans ordre particulier. */
    List<Task> findByProjectId(Long projectId);

    /** Tâches d'un projet, de la plus récente à la plus ancienne. */
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
