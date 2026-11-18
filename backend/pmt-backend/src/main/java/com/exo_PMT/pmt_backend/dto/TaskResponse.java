package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        LocalDateTime completedAt,
        Long createdByUserId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getName(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCompletedAt(),
                task.getCreatedByUserId()
        );
    }
}
