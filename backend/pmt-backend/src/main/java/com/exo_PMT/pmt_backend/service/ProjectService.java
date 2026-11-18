package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.CreateProjectRequest;
import com.exo_PMT.pmt_backend.dto.ProjectResponse;
import com.exo_PMT.pmt_backend.entity.Project;
import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.ProjectRepository;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProjectService {
    private static final int MAX_NAME_LENGTH = 160;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse createProject(Long userId, CreateProjectRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations du projet sont obligatoires.");
        }

        String name = request.name() == null ? "" : request.name().trim();
        String description = request.description() == null ? "" : request.description().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || description.isEmpty() || request.startDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom, la description et la date de début sont obligatoires."
            );
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setStartDate(request.startDate());
        project.setCreatedByUserId(creator.getId());
        Project savedProject = projectRepository.save(project);

        ProjectMember adminMembership = new ProjectMember();
        adminMembership.setProjectId(savedProject.getId());
        adminMembership.setUserId(creator.getId());
        adminMembership.setEmail(creator.getEmail());
        adminMembership.setRole(Role.ADMIN);
        adminMembership.setStatus(ProjectMemberStatus.ACTIVE);
        adminMembership.setInvitedByUserId(creator.getId());
        projectMemberRepository.save(adminMembership);

        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findProjectsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.");
        }

        List<Long> projectIds = projectMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, ProjectMemberStatus.ACTIVE)
                .stream()
                .map(ProjectMember::getProjectId)
                .toList();

        return projectRepository.findByIdInOrderByCreatedAtDesc(projectIds)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }
}
