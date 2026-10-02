package com.tracker.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.backend.dto.CreateProjectRequest;
import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.RegisterRequest;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// * Integration tests that boot the full Spring context with an H2 in-memory database
// * and send simulated HTTP requests through SecurityConfig, Controllers, Services, and GlobalExceptionHandler.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Test 0: Public /api/health endpoint returns 200 OK with status UP (used by Render health check)
    @Test
    void healthCheck_PublicEndpoint_ReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // Test 1: Public /api/auth/register endpoint creates a new user and returns 200 OK
    @Test
    void registerUser_PublicEndpoint_ReturnsOk() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Integration User");
        request.setEmail("integration@test.com");
        request.setPassword("secret123");
        request.setRole(Role.MANAGER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    // Test 2: Protected endpoint (/api/projects) rejects unauthenticated requests with 403
    @Test
    void getProjects_WithoutJwt_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isForbidden());
    }

    // Test 3: Role-Based Access Control — A MEMBER cannot create a Sprint (403 Forbidden)
    @Test
    @WithMockUser(username = "member@test.com", roles = {"MEMBER"})
    void createSprint_AsMember_ReturnsForbidden() throws Exception {
        CreateSprintRequest sprintRequest = new CreateSprintRequest();
        sprintRequest.setName("Member Sprint");
        sprintRequest.setProjectId(1L);
        sprintRequest.setStartDate(LocalDateTime.of(2026, 10, 1, 0, 0));
        sprintRequest.setEndDate(LocalDateTime.of(2026, 10, 14, 0, 0));

        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sprintRequest)))
                .andExpect(status().isForbidden());
    }

    // Test 4: Business Rule + GlobalExceptionHandler — Creating DONE task without assignee returns 400 Bad Request
    @Test
    @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
    void createTask_DoneWithoutAssignee_ReturnsBadRequestWithMessage() throws Exception {
        // First create a project and sprint as MANAGER
        CreateProjectRequest projectReq = new CreateProjectRequest();
        projectReq.setName("Demo Project");
        projectReq.setDescription("Testing business rules");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isCreated());

        // Now try to create a task with status DONE and assigneeId = null
        CreateTaskRequest taskReq = new CreateTaskRequest();
        taskReq.setTitle("Unassigned Done Task");
        taskReq.setStatus(TaskStatus.DONE);
        taskReq.setPriority("HIGH");
        taskReq.setSprintId(1L);
        taskReq.setAssigneeId(null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot move task to DONE without an assignee"));
    }
}
