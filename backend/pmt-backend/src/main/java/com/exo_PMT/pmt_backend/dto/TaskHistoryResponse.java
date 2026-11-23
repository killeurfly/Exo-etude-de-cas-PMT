package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.TaskHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Une modification d'un champ d'une tâche.")
public record TaskHistoryResponse(
        @Schema(description = "Identifiant de l'entrée d'historique.", example = "12")
        Long id,
        @Schema(description = "Champ modifié : name, description, dueDate, priority, status ou assignee.", example = "status")
        String fieldName,
        @Schema(description = "Ancienne valeur brute (code ou identifiant pour assignee), ou null.", example = "TODO", nullable = true)
        String oldValue,
        @Schema(description = "Nouvelle valeur brute (code ou identifiant pour assignee), ou null.", example = "IN_PROGRESS", nullable = true)
        String newValue,
        @Schema(description = "Identifiant de l'auteur de la modification.", example = "2", nullable = true)
        Long changedByUserId,
        @Schema(description = "Nom de l'auteur de la modification.", example = "Marc", nullable = true)
        String changedByName,
        @Schema(description = "Date et heure de la modification.", example = "2026-11-18T14:05:00")
        LocalDateTime changedAt
) {
    public static TaskHistoryResponse from(TaskHistory history, String changedByName) {
        return new TaskHistoryResponse(
                history.getId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getChangedByUserId(),
                changedByName,
                history.getChangedAt()
        );
    }
}
