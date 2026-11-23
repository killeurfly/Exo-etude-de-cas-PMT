package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.entity.Project;
import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.ProjectRepository;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectMemberControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private User admin;
    private Project project;

    @BeforeEach
    void createProjectWithAdmin() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        admin = createUser("Admin", "admin@example.test");
        project = new Project();
        project.setName("PMT");
        project.setDescription("Gestion de projets");
        project.setStartDate(LocalDate.of(2026, 11, 18));
        project.setCreatedByUserId(admin.getId());
        project = projectRepository.save(project);
        addMember(admin, Role.ADMIN);
    }

    @Test
    void adminInvitesRegisteredUserWhoBecomesActiveMember() throws Exception {
        User bob = createUser("Bob", "bob@example.test");

        mockMvc.perform(invite(admin, "  BOB@example.test ", "MEMBER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("bob@example.test")))
                .andExpect(jsonPath("$.name", is("Bob")))
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mockMvc.perform(get("/api/projects").queryParam("userId", bob.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("PMT")));
    }

    @Test
    void invitationForUnknownEmailIsActivatedOnRegistration() throws Exception {
        mockMvc.perform(invite(admin, "carol@example.test", "OBSERVER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("INVITED")))
                .andExpect(jsonPath("$.userId", nullValue()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Carol", "email": "carol@example.test", "password": "carol123"}
                                """))
                .andExpect(status().isCreated());

        ProjectMember invitation = projectMemberRepository
                .findByProjectIdAndEmail(project.getId(), "carol@example.test")
                .orElseThrow();
        assertEquals(ProjectMemberStatus.ACTIVE, invitation.getStatus());
        assertEquals(Role.OBSERVER, invitation.getRole());
        assertEquals(
                userRepository.findByEmail("carol@example.test").orElseThrow().getId(),
                invitation.getUserId()
        );
    }

    @Test
    void membersListShowsRolesToProjectMembersOnly() throws Exception {
        User bob = createUser("Bob", "bob@example.test");
        addMember(bob, Role.MEMBER);
        User outsider = createUser("Outsider", "outsider@example.test");

        mockMvc.perform(get(membersUrl()).queryParam("userId", bob.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].name", is("Admin")))
                .andExpect(jsonPath("$[0].role", is("ADMIN")))
                .andExpect(jsonPath("$[1].role", is("MEMBER")));

        mockMvc.perform(get(membersUrl()).queryParam("userId", outsider.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projects/" + project.getId()).queryParam("userId", outsider.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminsCanInviteOrChangeRoles() throws Exception {
        User member = createUser("Member", "member@example.test");
        ProjectMember membership = addMember(member, Role.MEMBER);
        User observer = createUser("Observer", "observer@example.test");
        addMember(observer, Role.OBSERVER);

        mockMvc.perform(invite(member, "new@example.test", "MEMBER"))
                .andExpect(status().isForbidden());
        mockMvc.perform(invite(observer, "new@example.test", "MEMBER"))
                .andExpect(status().isForbidden());
        mockMvc.perform(changeRole(observer, membership.getId(), "ADMIN"))
                .andExpect(status().isForbidden());

        mockMvc.perform(changeRole(admin, membership.getId(), "OBSERVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("OBSERVER")));
    }

    @Test
    void invitationRejectsDuplicatesAndInvalidData() throws Exception {
        mockMvc.perform(invite(admin, "admin@example.test", "MEMBER"))
                .andExpect(status().isConflict());
        mockMvc.perform(invite(admin, "not-an-email", "MEMBER"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(invite(admin, "someone@example.test", "BOSS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        ProjectMember adminMembership = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), admin.getId())
                .orElseThrow();

        mockMvc.perform(changeRole(admin, adminMembership.getId(), "MEMBER"))
                .andExpect(status().isConflict());
    }

    private String membersUrl() {
        return "/api/projects/" + project.getId() + "/members";
    }

    private org.springframework.test.web.servlet.RequestBuilder invite(User user, String email, String role) {
        return post(membersUrl())
                .queryParam("userId", user.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"role\": \"" + role + "\"}");
    }

    private org.springframework.test.web.servlet.RequestBuilder changeRole(User user, Long memberId, String role) {
        return put(membersUrl() + "/" + memberId)
                .queryParam("userId", user.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"" + role + "\"}");
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("test-password");
        return userRepository.save(user);
    }

    private ProjectMember addMember(User user, Role role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getId());
        member.setUserId(user.getId());
        member.setEmail(user.getEmail());
        member.setRole(role);
        member.setStatus(ProjectMemberStatus.ACTIVE);
        member.setInvitedByUserId(admin.getId());
        return projectMemberRepository.save(member);
    }
}
