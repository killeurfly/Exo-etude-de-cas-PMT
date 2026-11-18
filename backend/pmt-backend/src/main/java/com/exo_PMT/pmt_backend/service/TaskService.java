package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.dto.UpdateTaskRequest;
import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.model.TaskStatus;
import com.exo_PMT.pmt_backend.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private static final int MAX_NAME_LENGTH = 180;

    private final TaskRepository taskRepository;
    private final ProjectMemberService projectMemberService;

    public TaskService(TaskRepository taskRepository, ProjectMemberService projectMemberService) {
        this.taskRepository = taskRepository;
        this.projectMemberService = projectMemberService;
    }

    @Transactional
    public TaskResponse createTask(Long projectId, Long userId, CreateTaskRequest request) {
        requireEditor(projectId, userId, "Un observateur ne peut pas créer de tâche.");
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de la tâche sont obligatoires.");
        }

        Task task = new Task();
        task.setProjectId(projectId);
        task.setName(requireName(request.name()));
        task.setDescription(requireDescription(request.description()));
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setCreatedByUserId(userId);

        return TaskResponse.from(taskRepository.save(task));
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

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findTasks(Long projectId, Long userId) {
        projectMemberService.requireActiveMember(projectId, userId);

        return taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(TaskResponse::from)
                .toList();
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
