package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByProjectId(Long projectId);

    List<TaskAssignment> findByUserId(Long userId);

    Optional<TaskAssignment> findByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);
}
