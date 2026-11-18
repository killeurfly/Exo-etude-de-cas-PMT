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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @BeforeEach
    void clearProjectData() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createProjectAddsCreatorAsAdminAndListsProjectOnlyForCreator() throws Exception {
        User creator = createUser("owner@example.test");
        User otherUser = createUser("other@example.test");

        mockMvc.perform(post("/api/projects")
                        .queryParam("userId", creator.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  PMT  ",
                                  "description": "  Gestion de projets  ",
                                  "startDate": "2026-11-18"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("PMT")))
                .andExpect(jsonPath("$.description", is("Gestion de projets")))
                .andExpect(jsonPath("$.startDate", is("2026-11-18")))
                .andExpect(jsonPath("$.createdByUserId", is(creator.getId().intValue())));

        Project project = projectRepository.findAll().get(0);
        ProjectMember membership = projectMemberRepository.findByProjectId(project.getId()).get(0);
        org.junit.jupiter.api.Assertions.assertEquals(creator.getId(), membership.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(Role.ADMIN, membership.getRole());
        org.junit.jupiter.api.Assertions.assertEquals(ProjectMemberStatus.ACTIVE, membership.getStatus());

        mockMvc.perform(get("/api/projects").queryParam("userId", creator.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("PMT")));

        mockMvc.perform(get("/api/projects").queryParam("userId", otherUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void createProjectRejectsMissingRequiredFields() throws Exception {
        User creator = createUser("owner@example.test");

        mockMvc.perform(post("/api/projects")
                        .queryParam("userId", creator.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "description": "",
                                  "startDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listingProjectsRejectsUnknownUser() throws Exception {
        mockMvc.perform(get("/api/projects").queryParam("userId", "9999"))
                .andExpect(status().isNotFound());
    }

    private User createUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("test-password");
        return userRepository.save(user);
    }
}
