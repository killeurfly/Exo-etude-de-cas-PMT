package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}
