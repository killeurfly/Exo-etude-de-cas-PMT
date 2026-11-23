package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Nouvelles informations d'une tâche : tous les champs sont remplacés.")
public record UpdateTaskRequest(
        @Schema(description = "Nom de la tâche (180 caractères max).", example = "Rédiger le README")
        String name,
        @Schema(description = "Description de la tâche.", example = "Décrire l'installation et le déploiement.")
        String description,
        @Schema(description = "Échéance, format AAAA-MM-JJ. null pour la retirer.", example = "2026-12-15", nullable = true)
        LocalDate dueDate,
        @Schema(description = "Priorité (obligatoire).", example = "HIGH")
        TaskPriority priority,
        @Schema(description = "Statut (obligatoire). DONE renseigne la date de fin, un autre statut l'efface.", example = "IN_PROGRESS")
        TaskStatus status,
        @Schema(description = "Identifiant de la personne assignée. null pour retirer l'assignation.", example = "2", nullable = true)
        Long assigneeUserId
) {}
