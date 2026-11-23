package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nouveau rôle d'un membre.")
public record UpdateMemberRoleRequest(
        @Schema(description = "Nouveau rôle.", example = "OBSERVER")
        Role role
) {}
