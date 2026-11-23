package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès à la table {@code task_history}. */
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    /**
     * Historique d'une tâche, du plus récent au plus ancien. Le tri secondaire par {@code id} départage
     * les champs modifiés au même instant, lors d'un même enregistrement.
     */
    List<TaskHistory> findByTaskIdOrderByChangedAtDescIdDesc(Long taskId);
}
