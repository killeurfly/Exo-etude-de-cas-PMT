package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesUserWithoutReturningPasswordAndLoginAcceptsTrimmedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Alex Doe  ",
                                  "email": "Alex@example.test",
                                  "password": "simple-pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alex Doe")))
                .andExpect(jsonPath("$.email", is("alex@example.test")))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " ALEX@EXAMPLE.TEST ",
                                  "password": "simple-pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("alex@example.test")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Another User",
                                  "email": "TEST@example.test",
                                  "password": "another-pass"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        registerUser();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.test",
                                  "password": "wrong-pass"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.test",
                                  "password": "simple-pass"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
