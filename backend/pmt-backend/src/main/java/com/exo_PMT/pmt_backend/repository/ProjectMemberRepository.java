package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Accès à la table {@code project_members} (membres et invitations). */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    /** Membres et invitations d'un projet, sans ordre particulier. */
    List<ProjectMember> findByProjectId(Long projectId);

    /** Membres et invitations d'un projet, dans leur ordre d'arrivée. */
    List<ProjectMember> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    /** Adhésions d'un utilisateur ayant un statut donné, de la plus récente à la plus ancienne. */
    List<ProjectMember> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ProjectMemberStatus status);

    /**
     * Adhésions liées à un e-mail ; sert à retrouver les invitations en attente à l'inscription.
     */
    List<ProjectMember> findByEmailAndStatus(String email, ProjectMemberStatus status);

    /** Adhésion d'un utilisateur à un projet, quel que soit son statut. */
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    /** Adhésion ou invitation d'un e-mail dans un projet ; sert à refuser les doublons. */
    Optional<ProjectMember> findByProjectIdAndEmail(Long projectId, String email);

    /**
     * Compte les membres d'un projet ayant un rôle et un statut ; sert à protéger le dernier administrateur.
     */
    long countByProjectIdAndRoleAndStatus(Long projectId, Role role, ProjectMemberStatus status);
}
