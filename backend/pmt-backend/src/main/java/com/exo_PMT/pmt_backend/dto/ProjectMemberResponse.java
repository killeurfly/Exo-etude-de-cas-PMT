package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;

public record ProjectMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        Role role,
        ProjectMemberStatus status
) {
    public static ProjectMemberResponse from(ProjectMember member, String name) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getUserId(),
                name,
                member.getEmail(),
                member.getRole(),
                member.getStatus()
        );
    }
}
