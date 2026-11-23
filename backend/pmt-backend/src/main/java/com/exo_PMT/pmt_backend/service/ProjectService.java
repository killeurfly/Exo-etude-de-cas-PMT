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

/**
 * Création et consultation des projets.
 * <p>
 * Les droits d'accès à un projet sont vérifiés par {@link ProjectMemberService#requireActiveMember}.
 */
@Service
public class ProjectService {
    private static final int MAX_NAME_LENGTH = 160;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMemberService projectMemberService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectMemberService projectMemberService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectMemberService = projectMemberService;
    }

    /**
     * Crée un projet et inscrit son créateur comme administrateur actif, dans la même transaction :
     * un projet n'existe jamais sans administrateur.
     *
     * @param userId  identifiant du créateur
     * @param request nom (160 caractères max), description et date de début
     * @return le projet créé
     * @throws ResponseStatusException 400 si une information est manquante ou le nom trop long ;
     *                                 404 si l'utilisateur n'existe pas
     */
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

    /**
     * Renvoie un projet à l'un de ses membres actifs, quel que soit son rôle.
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant de l'utilisateur qui consulte
     * @return le projet
     * @throws ResponseStatusException 404 si le projet n'existe pas ; 403 si l'utilisateur n'en est pas membre actif
     */
    @Transactional(readOnly = true)
    public ProjectResponse findProject(Long projectId, Long userId) {
        projectMemberService.requireActiveMember(projectId, userId);
        return projectRepository.findById(projectId)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable."));
    }

    /**
     * Liste les projets dont l'utilisateur est membre actif, du plus récent au plus ancien.
     * Les invitations en attente ne donnent pas accès au projet.
     *
     * @param userId identifiant de l'utilisateur connecté
     * @return les projets, éventuellement aucun
     * @throws ResponseStatusException 404 si l'utilisateur n'existe pas
     */
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
