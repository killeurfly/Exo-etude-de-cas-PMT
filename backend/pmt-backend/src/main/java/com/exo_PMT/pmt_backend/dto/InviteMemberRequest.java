package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Invitation d'une personne dans un projet.")
public record InviteMemberRequest(
        @Schema(description = "E-mail de la personne à inviter.", example = "zoe@example.test")
        String email,
        @Schema(description = "Rôle attribué dans le projet.", example = "MEMBER")
        Role role
) {}
