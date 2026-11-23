package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.CreateTaskRequest;
import com.exo_PMT.pmt_backend.dto.TaskHistoryResponse;
import com.exo_PMT.pmt_backend.dto.TaskResponse;
import com.exo_PMT.pmt_backend.dto.UpdateTaskRequest;
import com.exo_PMT.pmt_backend.service.TaskService;
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

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> findTasks(@PathVariable Long projectId, @RequestParam Long userId) {
        return taskService.findTasks(projectId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestBody CreateTaskRequest request
    ) {
        return taskService.createTask(projectId, userId, request);
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam Long userId,
            @RequestBody UpdateTaskRequest request
    ) {
        return taskService.updateTask(projectId, taskId, userId, request);
    }

    @GetMapping("/{taskId}")
    public TaskResponse findTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam Long userId
    ) {
        return taskService.findTask(projectId, taskId, userId);
    }

    @GetMapping("/{taskId}/history")
    public List<TaskHistoryResponse> findHistory(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam Long userId
    ) {
        return taskService.findHistory(projectId, taskId, userId);
    }
}
