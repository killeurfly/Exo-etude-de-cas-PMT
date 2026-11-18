package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.CreateProjectRequest;
import com.exo_PMT.pmt_backend.dto.ProjectResponse;
import com.exo_PMT.pmt_backend.service.ProjectService;
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

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> findProjects(@RequestParam Long userId) {
        return projectService.findProjectsForUser(userId);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse findProject(@PathVariable Long projectId, @RequestParam Long userId) {
        return projectService.findProject(projectId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @RequestParam Long userId,
            @RequestBody CreateProjectRequest request
    ) {
        return projectService.createProject(userId, request);
    }
}
