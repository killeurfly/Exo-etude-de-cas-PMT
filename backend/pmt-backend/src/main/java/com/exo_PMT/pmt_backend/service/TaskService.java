package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskHistoryResponse;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.dto.UpdateTaskRequest;
import com.exo_PMT.pmt_backend.entity.Project;
import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.entity.TaskAssignment;
import com.exo_PMT.pmt_backend.entity.TaskHistory;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.ProjectRepository;
import com.exo_PMT.pmt_backend.repository.TaskAssignmentRepository;
import com.exo_PMT.pmt_backend.repository.TaskHistoryRepository;
import com.exo_PMT.pmt_backend.repository.TaskRepository;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tâches d'un projet : création, modification, assignation, historique et notification.
 * <p>
 * Règles de rôle : tout membre actif peut consulter les tâches ; seuls les rôles ADMIN et MEMBER
 * peuvent les créer, les modifier et les assigner (un OBSERVER reçoit une erreur 403).
 */
@Service
public class TaskService {
    private static final int MAX_NAME_LENGTH = 180;

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;
    private final EmailService emailService;

    public TaskService(
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            TaskHistoryRepository taskHistoryRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectMemberService projectMemberService,
            EmailService emailService
    ) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectMemberService = projectMemberService;
        this.emailService = emailService;
    }

    /**
     * Crée une tâche au statut TODO (priorité MEDIUM par défaut). Si une personne est assignée,
     * elle reçoit un e-mail de notification.
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant du créateur (ADMIN ou MEMBER)
     * @param request   nom, description, échéance facultative, priorité et personne assignée facultative
     * @return la tâche créée
     * @throws ResponseStatusException 400 si le nom ou la description manque, ou si la personne assignée
     *                                 n'est pas membre actif ; 403 si l'utilisateur est observateur ou hors du projet ;
     *                                 404 si le projet n'existe pas
     */
    @Transactional
    public TaskResponse createTask(Long projectId, Long userId, CreateTaskRequest request) {
        requireEditor(projectId, userId, "Un observateur ne peut pas créer de tâche.");
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de la tâche sont obligatoires.");
        }
        requireAssignableMember(projectId, request.assigneeUserId());

        Task task = new Task();
        task.setProjectId(projectId);
        task.setName(requireName(request.name()));
        task.setDescription(requireDescription(request.description()));
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setCreatedByUserId(userId);
        task = taskRepository.save(task);

        assign(task, request.assigneeUserId(), userId);
        return TaskResponse.from(task, request.assigneeUserId());
    }

    /**
     * Remplace toutes les informations d'une tâche.
     * <ul>
     *   <li>Chaque champ réellement modifié ajoute une ligne à l'historique.</li>
     *   <li>La date de fin est renseignée au passage au statut DONE, conservée si la tâche était déjà
     *       terminée et effacée si la tâche est rouverte.</li>
     *   <li>Un changement de personne assignée envoie un e-mail à la nouvelle personne.</li>
     * </ul>
     *
     * @param projectId identifiant du projet
     * @param taskId    identifiant de la tâche
     * @param userId    identifiant de l'auteur de la modification (ADMIN ou MEMBER)
     * @param request   nouvelles valeurs ; priorité et statut obligatoires
     * @return la tâche modifiée
     * @throws ResponseStatusException 400 si un champ obligatoire manque ou si la personne assignée n'est pas membre actif ;
     *                                 403 si l'utilisateur est observateur ou hors du projet ;
     *                                 404 si le projet n'existe pas ou si la tâche n'appartient pas à ce projet
     */
    @Transactional
    public TaskResponse updateTask(Long projectId, Long taskId, Long userId, UpdateTaskRequest request) {
        requireEditor(projectId, userId, "Un observateur ne peut pas modifier de tâche.");
        Task task = requireTask(projectId, taskId);
        if (request == null || request.priority() == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La priorité et le statut sont obligatoires.");
        }
        requireAssignableMember(projectId, request.assigneeUserId());
        String name = requireName(request.name());
        String description = requireDescription(request.description());

        // Une ligne d'historique par champ réellement modifié, comparé avant la mise à jour.
        List<TaskHistory> changes = new ArrayList<>();
        track(changes, task, userId, "name", task.getName(), name);
        track(changes, task, userId, "description", task.getDescription(), description);
        track(changes, task, userId, "dueDate", task.getDueDate(), request.dueDate());
        track(changes, task, userId, "priority", task.getPriority(), request.priority());
        track(changes, task, userId, "status", task.getStatus(), request.status());
        track(changes, task, userId, "assignee", currentAssignee(taskId), request.assigneeUserId());

        task.setName(name);
        task.setDescription(description);
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());

        // La date de fin suit le statut : posée au passage à DONE, effacée si la tâche est rouverte.
        if (request.status() == TaskStatus.DONE && task.getStatus() != TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (request.status() != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }
        task.setStatus(request.status());
        task = taskRepository.save(task);

        assign(task, request.assigneeUserId(), userId);
        taskHistoryRepository.saveAll(changes);
        return TaskResponse.from(task, request.assigneeUserId());
    }

    /**
     * Renvoie l'historique d'une tâche, de la modification la plus récente à la plus ancienne,
     * avec le nom de l'auteur de chaque modification. Accessible à tout membre actif.
     *
     * @param projectId identifiant du projet
     * @param taskId    identifiant de la tâche
     * @param userId    identifiant de l'utilisateur qui consulte
     * @return les modifications, une par champ modifié
     * @throws ResponseStatusException 403 si l'utilisateur n'est pas membre actif ;
     *                                 404 si le projet n'existe pas ou si la tâche n'appartient pas à ce projet
     */
    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> findHistory(Long projectId, Long taskId, Long userId) {
        projectMemberService.requireActiveMember(projectId, userId);
        requireTask(projectId, taskId);

        List<TaskHistory> history = taskHistoryRepository.findByTaskIdOrderByChangedAtDescIdDesc(taskId);
        List<Long> authorIds = history.stream()
                .map(TaskHistory::getChangedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> namesByUserId = userRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return history.stream()
                .map(entry -> TaskHistoryResponse.from(entry, namesByUserId.get(entry.getChangedByUserId())))
                .toList();
    }

    /**
     * Renvoie une tâche et la personne qui lui est assignée. Accessible à tout membre actif.
     *
     * @param projectId identifiant du projet
     * @param taskId    identifiant de la tâche
     * @param userId    identifiant de l'utilisateur qui consulte
     * @return la tâche
     * @throws ResponseStatusException 403 si l'utilisateur n'est pas membre actif ;
     *                                 404 si le projet n'existe pas ou si la tâche n'appartient pas à ce projet
     */
    @Transactional(readOnly = true)
    public TaskResponse findTask(Long projectId, Long taskId, Long userId) {
        projectMemberService.requireActiveMember(projectId, userId);

        Task task = requireTask(projectId, taskId);

        return TaskResponse.from(task, currentAssignee(taskId));
    }

    /**
     * Liste les tâches d'un projet, de la plus récente à la plus ancienne, avec leur personne assignée.
     * Les assignations sont chargées en une seule requête pour tout le projet.
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant de l'utilisateur qui consulte
     * @return les tâches du projet
     * @throws ResponseStatusException 404 si le projet n'existe pas ; 403 si l'utilisateur n'en est pas membre actif
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> findTasks(Long projectId, Long userId) {
        projectMemberService.requireActiveMember(projectId, userId);

        Map<Long, Long> assigneeByTaskId = taskAssignmentRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(
                        TaskAssignment::getTaskId,
                        TaskAssignment::getUserId,
                        (first, ignored) -> first
                ));

        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(task -> TaskResponse.from(task, assigneeByTaskId.get(task.getId())))
                .toList();
    }

    /**
     * Charge une tâche en vérifiant qu'elle appartient bien au projet de l'URL : sans ce contrôle,
     * un membre d'un projet pourrait lire ou modifier la tâche d'un autre projet en changeant l'identifiant.
     * Lève une erreur 404 sinon.
     */
    private Task requireTask(Long projectId, Long taskId) {
        return taskRepository.findById(taskId)
                .filter(found -> found.getProjectId().equals(projectId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche introuvable."));
    }

    /** Renvoie l'identifiant de la personne actuellement assignée à la tâche, ou {@code null}. */
    private Long currentAssignee(Long taskId) {
        return taskAssignmentRepository.findByTaskId(taskId)
                .stream()
                .map(TaskAssignment::getUserId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Ajoute une entrée d'historique si la valeur d'un champ change. Les valeurs sont stockées sous forme
     * de texte brut (code d'énumération, date ISO, identifiant) ; le frontend les traduit pour l'affichage.
     */
    private void track(List<TaskHistory> changes, Task task, Long userId, String field, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        TaskHistory entry = new TaskHistory();
        entry.setTaskId(task.getId());
        entry.setChangedByUserId(userId);
        entry.setFieldName(field);
        entry.setOldValue(oldValue == null ? null : oldValue.toString());
        entry.setNewValue(newValue == null ? null : newValue.toString());
        changes.add(entry);
    }

    /**
     * Une tâche n'a qu'une personne assignée : l'éventuelle assignation précédente est remplacée,
     * puis la nouvelle personne est notifiée. Rien n'est fait si la personne ne change pas,
     * ce qui évite un e-mail en double. {@code null} retire l'assignation.
     */
    private void assign(Task task, Long assigneeUserId, Long assignedByUserId) {
        List<TaskAssignment> current = taskAssignmentRepository.findByTaskId(task.getId());
        if (assigneeUserId != null
                && current.size() == 1
                && current.get(0).getUserId().equals(assigneeUserId)) {
            return;
        }

        taskAssignmentRepository.deleteAll(current);
        taskAssignmentRepository.flush();
        if (assigneeUserId == null) {
            return;
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setProjectId(task.getProjectId());
        assignment.setTaskId(task.getId());
        assignment.setUserId(assigneeUserId);
        assignment.setAssignedByUserId(assignedByUserId);
        taskAssignmentRepository.save(assignment);
        notifyAssignee(task, assigneeUserId, assignedByUserId);
    }

    /**
     * Envoie l'e-mail d'assignation avec le nom de la tâche, du projet et de la personne qui assigne.
     */
    private void notifyAssignee(Task task, Long assigneeUserId, Long assignedByUserId) {
        User assignee = userRepository.findById(assigneeUserId).orElse(null);
        if (assignee == null) {
            return;
        }
        String assignedByName = userRepository.findById(assignedByUserId)
                .map(User::getName)
                .orElse("Un membre du projet");
        String projectName = projectRepository.findById(task.getProjectId())
                .map(Project::getName)
                .orElse("PMT");

        emailService.sendTaskAssigned(assignee.getEmail(), task.getName(), projectName, assignedByName);
    }

    /**
     * Lève une erreur 400 si la personne à assigner n'est pas membre actif du projet ({@code null} est accepté).
     */
    private void requireAssignableMember(Long projectId, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return;
        }
        boolean activeMember = projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeUserId)
                .filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
                .isPresent();
        if (!activeMember) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La tâche ne peut être assignée qu'à un membre actif du projet."
            );
        }
    }

    /**
     * Vérifie que l'utilisateur est membre actif avec le rôle ADMIN ou MEMBER ; un observateur reçoit une erreur 403.
     */
    private void requireEditor(Long projectId, Long userId, String observerMessage) {
        if (projectMemberService.requireActiveMember(projectId, userId).getRole() == Role.OBSERVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, observerMessage);
        }
    }

    /**
     * Renvoie le nom sans espaces superflus ; erreur 400 s'il est vide ou dépasse 180 caractères.
     */
    private String requireName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de la tâche est obligatoire.");
        }
        return trimmed;
    }

    /** Renvoie la description sans espaces superflus ; erreur 400 si elle est vide. */
    private String requireDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description de la tâche est obligatoire.");
        }
        return trimmed;
    }
}
