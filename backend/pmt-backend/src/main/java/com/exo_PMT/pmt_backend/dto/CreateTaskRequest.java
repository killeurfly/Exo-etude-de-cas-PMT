package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.TaskPriority;

import java.time.LocalDate;

public record CreateTaskRequest(String name, String description, LocalDate dueDate, TaskPriority priority) {}
