package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Un projet.")
public record ProjectResponse(
        @Schema(description = "Identifiant du projet.", example = "1")
        Long id,
        @Schema(description = "Nom du projet.", example = "Refonte du site")
        String name,
        @Schema(description = "Description du projet.", example = "Nouvelle version du site vitrine.")
        String description,
        @Schema(description = "Date de début.", example = "2026-12-01")
        LocalDate startDate,
        @Schema(description = "Identifiant du créateur du projet.", example = "1", nullable = true)
        Long createdByUserId
) {
    /** Construit la réponse à partir de l'entité. */
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
