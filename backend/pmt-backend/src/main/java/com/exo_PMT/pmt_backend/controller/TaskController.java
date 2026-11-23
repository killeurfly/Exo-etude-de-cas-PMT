package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskHistoryResponse;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.dto.UpdateTaskRequest;
import com.exo_PMT.pmt_backend.service.TaskService;
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

/**
 * Tâches d'un projet et leur historique ({@code /api/projects/{projectId}/tasks}).
 * <p>
 * Le détail de chaque endpoint (paramètres, réponses, codes d'erreur) est décrit par les annotations
 * OpenAPI et consultable sur {@code /swagger-ui.html}. Les règles métier sont dans le service.
 */
@Tag(name = "Tâches", description = "Création, consultation, modification, assignation et historique des tâches d'un projet.")
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Lister les tâches d'un projet",
            description = "Renvoie toutes les tâches du projet, de la plus récente à la plus ancienne. "
                    + "Accessible à tout membre actif, observateur compris."
    )
    @ApiResponse(responseCode = "200", description = "Liste des tâches (éventuellement vide).")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre actif du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable.")
    @GetMapping
    public List<TaskResponse> findTasks(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'utilisateur qui fait la demande.", example = "1") @RequestParam Long userId
    ) {
        return taskService.findTasks(projectId, userId);
    }

    @Operation(
            summary = "Créer une tâche",
            description = "Crée une tâche au statut TODO. La priorité vaut MEDIUM si elle est absente. "
                    + "Si `assigneeUserId` est renseigné, la personne doit être membre actif du projet "
                    + "et reçoit un e-mail de notification. Réservé aux rôles ADMIN et MEMBER."
    )
    @ApiResponse(responseCode = "201", description = "Tâche créée.")
    @ApiResponse(responseCode = "400", description = "Nom ou description manquant, ou personne assignée hors du projet.")
    @ApiResponse(responseCode = "403", description = "Utilisateur observateur ou hors du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'utilisateur qui crée la tâche.", example = "1") @RequestParam Long userId,
            @RequestBody CreateTaskRequest request
    ) {
        return taskService.createTask(projectId, userId, request);
    }

    @Operation(
            summary = "Modifier une tâche",
            description = "Remplace toutes les informations de la tâche. Passer le statut à DONE renseigne la date "
                    + "de fin ; repasser à un autre statut l'efface. Chaque champ modifié est ajouté à l'historique. "
                    + "Un changement de personne assignée envoie un e-mail. Réservé aux rôles ADMIN et MEMBER."
    )
    @ApiResponse(responseCode = "200", description = "Tâche modifiée.")
    @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant (nom, description, priorité, statut) ou personne assignée hors du projet.")
    @ApiResponse(responseCode = "403", description = "Utilisateur observateur ou hors du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable, ou tâche absente de ce projet.")
    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de la tâche.", example = "1") @PathVariable Long taskId,
            @Parameter(description = "Identifiant de l'utilisateur qui modifie.", example = "1") @RequestParam Long userId,
            @RequestBody UpdateTaskRequest request
    ) {
        return taskService.updateTask(projectId, taskId, userId, request);
    }

    @Operation(
            summary = "Consulter une tâche",
            description = "Renvoie toutes les informations d'une tâche. Accessible à tout membre actif, observateur compris."
    )
    @ApiResponse(responseCode = "200", description = "Tâche trouvée.")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre actif du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable, ou tâche absente de ce projet.")
    @GetMapping("/{taskId}")
    public TaskResponse findTask(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de la tâche.", example = "1") @PathVariable Long taskId,
            @Parameter(description = "Identifiant de l'utilisateur qui fait la demande.", example = "1") @RequestParam Long userId
    ) {
        return taskService.findTask(projectId, taskId, userId);
    }

    @Operation(
            summary = "Historique d'une tâche",
            description = "Liste des modifications, de la plus récente à la plus ancienne : une entrée par champ "
                    + "modifié (`name`, `description`, `dueDate`, `priority`, `status`, `assignee`) avec l'ancienne "
                    + "et la nouvelle valeur. Accessible à tout membre actif, observateur compris."
    )
    @ApiResponse(responseCode = "200", description = "Historique (éventuellement vide).")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre actif du projet.")
    @ApiResponse(responseCode = "404", description = "Projet introuvable, ou tâche absente de ce projet.")
    @GetMapping("/{taskId}/history")
    public List<TaskHistoryResponse> findHistory(
            @Parameter(description = "Identifiant du projet.", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de la tâche.", example = "1") @PathVariable Long taskId,
            @Parameter(description = "Identifiant de l'utilisateur qui fait la demande.", example = "1") @RequestParam Long userId
    ) {
        return taskService.findHistory(projectId, taskId, userId);
    }
}
