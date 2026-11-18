package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    List<ProjectMember> findByUserId(Long userId);

    List<ProjectMember> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ProjectMemberStatus status);

    List<ProjectMember> findByEmailAndStatus(String email, ProjectMemberStatus status);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndEmail(Long projectId, String email);

    long countByProjectIdAndRoleAndStatus(Long projectId, Role role, ProjectMemberStatus status);
}
