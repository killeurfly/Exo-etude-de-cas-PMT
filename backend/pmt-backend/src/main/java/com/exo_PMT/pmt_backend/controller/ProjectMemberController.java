package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.InviteMemberRequest;
import com.exo_PMT.pmt_backend.dto.ProjectMemberResponse;
import com.exo_PMT.pmt_backend.dto.UpdateMemberRoleRequest;
import com.exo_PMT.pmt_backend.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Membres", description = "Invitation des membres et gestion de leurs rôles dans un projet.")
@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @Operation(
            summary = "Lister les membres",
            description = "Renvoie les membres du projet, y compris les invitations en attente (statut INVITED). "
                    + "Accessible à tout membre actif."
    )
    @ApiResponse(responseCode = "200", description = "Liste des membres.")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre actif du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable.")
    @GetMapping
    public List<ProjectMemberResponse> findMembers(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'utilisateur qui fait la demande.", example = "1") @RequestParam Long userId
    ) {
        return projectMemberService.findMembers(projectId, userId);
    }

    @Operation(
            summary = "Inviter un membre",
            description = "Ajoute une personne au projet par son e-mail, avec un rôle. Si un compte existe pour cet "
                    + "e-mail, la personne devient membre actif tout de suite ; sinon l'invitation reste en attente "
                    + "(INVITED) jusqu'à son inscription. Réservé aux administrateurs."
    )
    @ApiResponse(responseCode = "201", description = "Membre ajouté ou invitation enregistrée.")
    @ApiResponse(responseCode = "400", description = "E-mail invalide ou rôle manquant.")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas administrateur du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable.")
    @ApiResponse(responseCode = "409", description = "Cette personne fait déjà partie du projet ou est déjà invitée.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse inviteMember(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'administrateur qui invite.", example = "1") @RequestParam Long userId,
            @RequestBody InviteMemberRequest request
    ) {
        return projectMemberService.inviteMember(projectId, userId, request);
    }

    @Operation(
            summary = "Changer le rôle d'un membre",
            description = "Modifie le rôle d'un membre ou d'une invitation. Le dernier administrateur actif "
                    + "ne peut pas être rétrogradé. Réservé aux administrateurs."
    )
    @ApiResponse(responseCode = "200", description = "Rôle modifié.")
    @ApiResponse(responseCode = "400", description = "Rôle manquant.")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas administrateur du projet.")
    @ApiResponse(responseCode = "404", description = "Projet ou membre introuvable.")
    @ApiResponse(responseCode = "409", description = "Le projet doit garder au moins un administrateur.")
    @PutMapping("/{memberId}")
    public ProjectMemberResponse updateRole(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'adhésion (champ `id` renvoyé par la liste des membres).", example = "2") @PathVariable Long memberId,
            @Parameter(description = "Identifiant de l'administrateur qui modifie.", example = "1") @RequestParam Long userId,
            @RequestBody UpdateMemberRoleRequest request
    ) {
        return projectMemberService.updateRole(projectId, memberId, userId, request);
    }
}
