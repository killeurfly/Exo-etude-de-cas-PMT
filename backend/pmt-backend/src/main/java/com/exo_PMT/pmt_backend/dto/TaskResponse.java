package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Une tâche d'un projet.")
public record TaskResponse(
        @Schema(description = "Identifiant de la tâche.", example = "1")
        Long id,
        @Schema(description = "Identifiant du projet de la tâche.", example = "1")
        Long projectId,
        @Schema(description = "Nom de la tâche.", example = "Rédiger le README")
        String name,
        @Schema(description = "Description de la tâche.", example = "Décrire l'installation et le déploiement.")
        String description,
        @Schema(description = "Échéance, ou null.", example = "2026-12-15", nullable = true)
        LocalDate dueDate,
        @Schema(description = "Priorité.", example = "HIGH")
        TaskPriority priority,
        @Schema(description = "Statut.", example = "DONE")
        TaskStatus status,
        @Schema(description = "Date et heure de fin, renseignée automatiquement au passage à DONE ; null sinon.", example = "2026-12-10T14:30:00", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "Identifiant de l'utilisateur qui a créé la tâche.", example = "1", nullable = true)
        Long createdByUserId,
        @Schema(description = "Identifiant de la personne assignée, ou null si personne n'est assigné.", example = "2", nullable = true)
        Long assigneeUserId
) {
    /**
     * Construit la réponse à partir de la tâche et de la personne assignée, stockée à part ({@code null} si aucune).
     */
    public static TaskResponse from(Task task, Long assigneeUserId) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getName(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCompletedAt(),
                task.getCreatedByUserId(),
                assigneeUserId
        );
    }
}
