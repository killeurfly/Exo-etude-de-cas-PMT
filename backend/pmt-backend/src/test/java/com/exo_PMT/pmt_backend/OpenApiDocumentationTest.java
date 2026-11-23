package com.exo_PMT.pmt_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocumentationDescribesEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("PMT API")))
                .andExpect(jsonPath("$.paths", hasKey("/api/projects/{projectId}/tasks/{taskId}")))
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/tasks'].post.summary", is("Créer une tâche")))
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/tasks'].post.responses", hasKey("403")))
                .andExpect(jsonPath("$.components.schemas.CreateTaskRequest.properties.name.example", is("Rédiger le README")))
                .andExpect(jsonPath("$.paths", not(hasKey("/hello"))));
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
