package com.exo_PMT.pmt_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Données pour créer un projet.")
public record CreateProjectRequest(
        @Schema(description = "Nom du projet (180 caractères max).", example = "Refonte du site")
        String name,
        @Schema(description = "Description du projet.", example = "Nouvelle version du site vitrine.")
        String description,
        @Schema(description = "Date de début, format AAAA-MM-JJ.", example = "2026-12-01")
        LocalDate startDate
) {}
