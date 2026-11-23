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

/**
 * Membres d'un projet : invitations, rôles (ADMIN, MEMBER, OBSERVER) et contrôle d'accès.
 * <p>
 * Fournit aussi {@link #requireActiveMember}, le point de contrôle commun utilisé par les autres
 * services pour vérifier qu'un utilisateur a accès à un projet.
 */
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

    /**
     * Liste les membres d'un projet dans leur ordre d'arrivée, invitations en attente comprises.
     * Le nom n'est renseigné que pour les personnes inscrites.
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant de l'utilisateur qui consulte
     * @return les membres et invitations du projet
     * @throws ResponseStatusException 404 si le projet n'existe pas ; 403 si l'utilisateur n'en est pas membre actif
     */
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

    /**
     * Invite une personne dans un projet par son adresse e-mail, avec un rôle. Réservé aux administrateurs.
     * <p>
     * Si un compte existe déjà pour cet e-mail, la personne devient membre actif immédiatement.
     * Sinon l'invitation reste en attente (INVITED) et sera activée à son inscription
     * ({@link UserService#register}).
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant de l'administrateur qui invite
     * @param request   e-mail et rôle de la personne invitée
     * @return le membre ou l'invitation créé
     * @throws ResponseStatusException 400 si l'e-mail est invalide ou le rôle absent ;
     *                                 403 si l'utilisateur n'est pas administrateur ;
     *                                 404 si le projet n'existe pas ;
     *                                 409 si la personne est déjà membre ou déjà invitée
     */
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

    /**
     * Change le rôle d'un membre ou d'une invitation. Réservé aux administrateurs.
     * <p>
     * Le dernier administrateur actif ne peut pas être rétrogradé : sans lui, plus personne
     * ne pourrait inviter ni gérer les rôles du projet.
     *
     * @param projectId identifiant du projet
     * @param memberId  identifiant de l'adhésion (champ {@code id} de la liste des membres)
     * @param userId    identifiant de l'administrateur qui modifie
     * @param request   nouveau rôle
     * @return le membre mis à jour
     * @throws ResponseStatusException 400 si le rôle est absent ; 403 si l'utilisateur n'est pas administrateur ;
     *                                 404 si le projet ou le membre n'existe pas ;
     *                                 409 s'il s'agit du dernier administrateur
     */
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

    /**
     * Vérifie qu'un utilisateur est membre actif d'un projet. Visibilité « package » :
     * utilisée par {@link ProjectService} et {@link TaskService}.
     *
     * @param projectId identifiant du projet
     * @param userId    identifiant de l'utilisateur
     * @return l'adhésion de l'utilisateur, qui porte son rôle
     * @throws ResponseStatusException 404 si le projet n'existe pas ;
     *                                 403 si l'utilisateur n'est pas membre actif (absent ou seulement invité)
     */
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

    /**
     * Vérifie que l'utilisateur est membre actif et administrateur du projet ; sinon erreur 403.
     */
    private void requireAdmin(Long projectId, Long userId) {
        if (requireActiveMember(projectId, userId).getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seul un administrateur peut gérer les membres."
            );
        }
    }
}
