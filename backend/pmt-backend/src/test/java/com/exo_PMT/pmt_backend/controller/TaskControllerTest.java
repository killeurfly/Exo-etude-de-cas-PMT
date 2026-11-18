package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.entity.Project;
import com.exo_PMT.pmt_backend.entity.ProjectMember;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.model.ProjectMemberStatus;
import com.exo_PMT.pmt_backend.model.Role;
import com.exo_PMT.pmt_backend.repository.ProjectMemberRepository;
import com.exo_PMT.pmt_backend.repository.ProjectRepository;
import com.exo_PMT.pmt_backend.repository.TaskRepository;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Project project;
    private User member;
    private User observer;

    @BeforeEach
    void createProjectWithMembers() {
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User admin = createUser("Admin", "admin@example.test");
        member = createUser("Member", "member@example.test");
        observer = createUser("Observer", "observer@example.test");

        project = new Project();
        project.setName("PMT");
        project.setDescription("Gestion de projets");
        project.setStartDate(LocalDate.of(2026, 11, 18));
        project.setCreatedByUserId(admin.getId());
        project = projectRepository.save(project);

        addMember(admin, Role.ADMIN);
        addMember(member, Role.MEMBER);
        addMember(observer, Role.OBSERVER);
    }

    @Test
    void memberCreatesTaskThatEveryMemberCanSee() throws Exception {
        mockMvc.perform(createTask(member, """
                        {
                          "name": "  Écrire les tests  ",
                          "description": "Couvrir la création de tâches",
                          "dueDate": "2026-12-01",
                          "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Écrire les tests")))
                .andExpect(jsonPath("$.dueDate", is("2026-12-01")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.projectId", is(project.getId().intValue())))
                .andExpect(jsonPath("$.createdByUserId", is(member.getId().intValue())));

        assertEquals(1, taskRepository.findByProjectId(project.getId()).size());

        mockMvc.perform(get(tasksUrl()).queryParam("userId", observer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Écrire les tests")));
    }

    @Test
    void priorityDefaultsToMediumAndDueDateIsOptional() throws Exception {
        mockMvc.perform(createTask(member, """
                        {"name": "Tâche simple", "description": "Sans échéance"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority", is("MEDIUM")))
                .andExpect(jsonPath("$.dueDate").doesNotExist());
    }

    @Test
    void observerCannotCreateTask() throws Exception {
        mockMvc.perform(createTask(observer, """
                        {"name": "Interdit", "description": "Un observateur ne crée pas de tâche"}
                        """))
                .andExpect(status().isForbidden());

        assertEquals(0, taskRepository.count());
    }

    @Test
    void taskRequiresNameAndDescription() throws Exception {
        mockMvc.perform(createTask(member, """
                        {"name": "   ", "description": ""}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outsiderCannotSeeOrCreateTasks() throws Exception {
        User outsider = createUser("Outsider", "outsider@example.test");

        mockMvc.perform(get(tasksUrl()).queryParam("userId", outsider.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(createTask(outsider, """
                        {"name": "Intrus", "description": "Pas membre du projet"}
                        """))
                .andExpect(status().isForbidden());
    }

    private String tasksUrl() {
        return "/api/projects/" + project.getId() + "/tasks";
    }

    private RequestBuilder createTask(User user, String json) {
        return post(tasksUrl())
                .queryParam("userId", user.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("test-password");
        return userRepository.save(user);
    }

    private void addMember(User user, Role role) {
        ProjectMember membership = new ProjectMember();
        membership.setProjectId(project.getId());
        membership.setUserId(user.getId());
        membership.setEmail(user.getEmail());
        membership.setRole(role);
        membership.setStatus(ProjectMemberStatus.ACTIVE);
        projectMemberRepository.save(membership);
    }
}
