package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un membre d'un projet, ou une invitation en attente.")
public record ProjectMemberResponse(
        @Schema(description = "Identifiant de l'adhésion (à utiliser comme memberId pour changer le rôle).", example = "2")
        Long id,
        @Schema(description = "Identifiant de l'utilisateur ; null tant que l'invitation n'est pas acceptée.", example = "3", nullable = true)
        Long userId,
        @Schema(description = "Nom de l'utilisateur ; null pour une invitation en attente.", example = "Marc", nullable = true)
        String name,
        @Schema(description = "E-mail du membre ou de la personne invitée.", example = "marc@example.test")
        String email,
        @Schema(description = "Rôle dans le projet.", example = "MEMBER")
        Role role,
        @Schema(description = "ACTIVE pour un membre, INVITED pour une invitation en attente.", example = "ACTIVE")
        ProjectMemberStatus status
) {
    /**
     * Construit la réponse à partir de l'adhésion et du nom de l'utilisateur ({@code null} pour une invitation en attente).
     */
    public static ProjectMemberResponse from(ProjectMember member, String name) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getUserId(),
                name,
                member.getEmail(),
                member.getRole(),
                member.getStatus()
        );
    }
}
