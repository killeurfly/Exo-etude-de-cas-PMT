package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.dto.UpdateTaskRequest;
import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.entity.TaskAssignment;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.TaskAssignmentRepository;
import com.exo_PMT.pmt_backend.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private static final int MAX_NAME_LENGTH = 180;

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;

    public TaskService(
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectMemberService projectMemberService
    ) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectMemberService = projectMemberService;
    }

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

    @Transactional
    public TaskResponse updateTask(Long projectId, Long taskId, Long userId, UpdateTaskRequest request) {
        requireEditor(projectId, userId, "Un observateur ne peut pas modifier de tâche.");
        Task task = taskRepository.findById(taskId)
                .filter(found -> found.getProjectId().equals(projectId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche introuvable."));
        if (request == null || request.priority() == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La priorité et le statut sont obligatoires.");
        }
        requireAssignableMember(projectId, request.assigneeUserId());

        task.setName(requireName(request.name()));
        task.setDescription(requireDescription(request.description()));
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
        return TaskResponse.from(task, request.assigneeUserId());
    }

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

    // Une tâche n'a qu'une personne assignée : on remplace l'éventuelle assignation précédente.
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
    }

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

    private void requireEditor(Long projectId, Long userId, String observerMessage) {
        if (projectMemberService.requireActiveMember(projectId, userId).getRole() == Role.OBSERVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, observerMessage);
        }
    }

    private String requireName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de la tâche est obligatoire.");
        }
        return trimmed;
    }

    private String requireDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description de la tâche est obligatoire.");
        }
        return trimmed;
    }
}
