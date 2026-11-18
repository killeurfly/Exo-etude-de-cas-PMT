package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.Project;

import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        Long createdByUserId
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getCreatedByUserId()
        );
    }
}
