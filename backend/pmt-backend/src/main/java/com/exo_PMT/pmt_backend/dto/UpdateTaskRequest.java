package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;

import java.time.LocalDate;

public record UpdateTaskRequest(
        String name,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status
) {}
