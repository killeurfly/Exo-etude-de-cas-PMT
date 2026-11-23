package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès à la table {@code task_assignments}. */
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    /** Assignation(s) d'une tâche. */
    List<TaskAssignment> findByTaskId(Long taskId);

    /** Toutes les assignations d'un projet, chargées en une requête pour la liste des tâches. */
    List<TaskAssignment> findByProjectId(Long projectId);
}
