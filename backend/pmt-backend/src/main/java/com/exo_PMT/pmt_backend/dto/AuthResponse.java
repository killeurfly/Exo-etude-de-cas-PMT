package com.exo_PMT.pmt_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Utilisateur connecté ou inscrit.")
public record AuthResponse(
        @Schema(description = "Identifiant de l'utilisateur, à passer comme userId aux autres endpoints.", example = "1")
        Long id,
        @Schema(description = "Nom de l'utilisateur.", example = "Alice")
        String name,
        @Schema(description = "E-mail de l'utilisateur.", example = "alice@example.test")
        String email
) {}
