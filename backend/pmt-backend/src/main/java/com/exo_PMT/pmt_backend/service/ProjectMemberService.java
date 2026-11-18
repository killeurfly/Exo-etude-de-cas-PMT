package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.dto.InviteMemberRequest;
import com.exo_PMT.pmt_backend.dto.ProjectMemberResponse;
import com.exo_PMT.pmt_backend.dto.UpdateMemberRoleRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectMemberService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectMemberService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findMembers(Long projectId, Long userId) {
        requireActiveMember(projectId, userId);

        List<ProjectMember> members = projectMemberRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<Long> userIds = members.stream()
                .map(ProjectMember::getUserId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> namesByUserId = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return members.stream()
                .map(member -> ProjectMemberResponse.from(member, namesByUserId.get(member.getUserId())))
                .toList();
    }

    @Transactional
    public ProjectMemberResponse inviteMember(Long projectId, Long userId, InviteMemberRequest request) {
        requireAdmin(projectId, userId);

        String email = UserService.normalizeEmail(request == null ? null : request.email());
        if (!UserService.isValidEmail(email) || request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'e-mail et le rôle sont obligatoires.");
        }
        if (projectMemberRepository.findByProjectIdAndEmail(projectId, email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette personne fait déjà partie du projet.");
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setEmail(email);
        member.setRole(request.role());
        member.setInvitedByUserId(userId);

        // Un utilisateur déjà inscrit rejoint directement le projet ;
        // sinon l'invitation reste en attente jusqu'à son inscription.
        User invitedUser = userRepository.findByEmail(email).orElse(null);
        if (invitedUser != null) {
            member.setUserId(invitedUser.getId());
            member.setStatus(ProjectMemberStatus.ACTIVE);
            member.setJoinedAt(LocalDateTime.now());
        } else {
            member.setStatus(ProjectMemberStatus.INVITED);
        }

        ProjectMember savedMember = projectMemberRepository.save(member);
        return ProjectMemberResponse.from(savedMember, invitedUser == null ? null : invitedUser.getName());
    }

    @Transactional
    public ProjectMemberResponse updateRole(
            Long projectId,
            Long memberId,
            Long userId,
            UpdateMemberRoleRequest request
    ) {
        requireAdmin(projectId, userId);
        if (request == null || request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle est obligatoire.");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .filter(found -> found.getProjectId().equals(projectId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable."));

        boolean removesAdmin = member.getRole() == Role.ADMIN && request.role() != Role.ADMIN;
        if (removesAdmin && member.getStatus() == ProjectMemberStatus.ACTIVE
                && projectMemberRepository.countByProjectIdAndRoleAndStatus(
                        projectId, Role.ADMIN, ProjectMemberStatus.ACTIVE) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le projet doit garder au moins un administrateur."
            );
        }

        member.setRole(request.role());
        String name = member.getUserId() == null
                ? null
                : userRepository.findById(member.getUserId()).map(User::getName).orElse(null);
        return ProjectMemberResponse.from(member, name);
    }

    ProjectMember requireActiveMember(Long projectId, Long userId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable.");
        }

        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tu ne fais pas partie de ce projet."
                ));
    }

    private void requireAdmin(Long projectId, Long userId) {
        if (requireActiveMember(projectId, userId).getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seul un administrateur peut gérer les membres."
            );
        }
    }
}
