package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.InviteMemberRequest;
import com.exo_PMT.pmt_backend.dto.ProjectMemberResponse;
import com.exo_PMT.pmt_backend.dto.UpdateMemberRoleRequest;
import com.exo_PMT.pmt_backend.service.ProjectMemberService;
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
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    public List<ProjectMemberResponse> findMembers(@PathVariable Long projectId, @RequestParam Long userId) {
        return projectMemberService.findMembers(projectId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse inviteMember(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestBody InviteMemberRequest request
    ) {
        return projectMemberService.inviteMember(projectId, userId, request);
    }

    @PutMapping("/{memberId}")
    public ProjectMemberResponse updateRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestParam Long userId,
            @RequestBody UpdateMemberRoleRequest request
    ) {
        return projectMemberService.updateRole(projectId, memberId, userId, request);
    }
}
