package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.entity.Task;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.model.TaskPriority;
import com.exo_PMT.pmt_backend.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        if (projectMemberService.requireActiveMember(projectId, userId).getRole() == Role.OBSERVER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Un observateur ne peut pas créer de tâche."
            );
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de la tâche sont obligatoires.");
        }

        String name = request.name() == null ? "" : request.name().trim();
        String description = request.description() == null ? "" : request.description().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || description.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom et la description de la tâche sont obligatoires."
            );
        }

        Task task = new Task();
        task.setProjectId(projectId);
        task.setName(name);
        task.setDescription(description);
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setCreatedByUserId(userId);

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
}
