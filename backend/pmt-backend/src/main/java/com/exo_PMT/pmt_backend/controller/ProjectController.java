package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.CreateProjectRequest;
import com.exo_PMT.pmt_backend.dto.ProjectResponse;
import com.exo_PMT.pmt_backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Création et consultation des projets ({@code /api/projects}).
 * <p>
 * Le détail de chaque endpoint (paramètres, réponses, codes d'erreur) est décrit par les annotations
 * OpenAPI et consultable sur {@code /swagger-ui.html}. Les règles métier sont dans le service.
 */
@Tag(name = "Projets", description = "Création et consultation des projets.")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(
            summary = "Lister mes projets",
            description = "Renvoie les projets dont l'utilisateur est membre actif, du plus récent au plus ancien."
    )
    @ApiResponse(responseCode = "200", description = "Liste des projets (éventuellement vide).")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable.")
    @GetMapping
    public List<ProjectResponse> findProjects(
            @Parameter(description = "Identifiant de l'utilisateur connecté.", example = "1") @RequestParam Long userId
    ) {
        return projectService.findProjectsForUser(userId);
    }

    @Operation(
            summary = "Consulter un projet",
            description = "Renvoie un projet. Réservé à ses membres actifs, observateurs compris."
    )
    @ApiResponse(responseCode = "200", description = "Projet trouvé.")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre actif du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable.")
    @GetMapping("/{projectId}")
    public ProjectResponse findProject(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'utilisateur qui fait la demande.", example = "1") @RequestParam Long userId
    ) {
        return projectService.findProject(projectId, userId);
    }

    @Operation(
            summary = "Créer un projet",
            description = "Crée un projet. Son créateur en devient automatiquement administrateur (rôle ADMIN)."
    )
    @ApiResponse(responseCode = "201", description = "Projet créé.")
    @ApiResponse(responseCode = "400", description = "Nom (180 caractères max), description ou date de début manquant.")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @Parameter(description = "Identifiant de l'utilisateur qui crée le projet.", example = "1") @RequestParam Long userId,
            @RequestBody CreateProjectRequest request
    ) {
        return projectService.createProject(userId, request);
    }
}
