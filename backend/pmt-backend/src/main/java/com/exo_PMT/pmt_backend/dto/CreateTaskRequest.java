package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Données pour créer une tâche.")
public record CreateTaskRequest(
        @Schema(description = "Nom de la tâche (180 caractères max).", example = "Rédiger le README")
        String name,
        @Schema(description = "Description de la tâche.", example = "Décrire l'installation et le déploiement.")
        String description,
        @Schema(description = "Échéance (facultative), format AAAA-MM-JJ.", example = "2026-12-15", nullable = true)
        LocalDate dueDate,
        @Schema(description = "Priorité. MEDIUM si absente.", example = "HIGH")
        TaskPriority priority,
        @Schema(description = "Identifiant de la personne assignée (facultatif). Doit être membre actif du projet.", example = "2", nullable = true)
        Long assigneeUserId
) {}
