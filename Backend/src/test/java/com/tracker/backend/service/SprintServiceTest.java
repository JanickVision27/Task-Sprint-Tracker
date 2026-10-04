package com.tracker.backend.service;

import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.SprintResponse;
import com.tracker.backend.entity.Project;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.ProjectRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// * Unit tests for SprintService verifying CRUD operations and sprint date business rules.
@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private SprintService sprintService;

    private Project testProject;
    private Sprint testSprint;
    private CreateSprintRequest validRequest;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Tracker Project");

        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 14, 0, 0);

        testSprint = new Sprint();
        testSprint.setId(5L);
        testSprint.setName("Sprint 1");
        testSprint.setStartDate(start);
        testSprint.setEndDate(end);
        testSprint.setProject(testProject);

        validRequest = new CreateSprintRequest();
        validRequest.setName("Sprint 1");
        validRequest.setProjectId(1L);
        validRequest.setStartDate(start);
        validRequest.setEndDate(end);
    }

    // Test 1: Creating a sprint with valid dates succeeds
    @Test
    void createSprint_Success() {
        // 1. ARRANGE
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);

        // 2. ACT
        SprintResponse response = sprintService.createSprint(validRequest);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Sprint 1", response.getName());
        assertEquals(1L, response.getProjectId());
        verify(sprintRepository, times(1)).save(any(Sprint.class));
    }

    // Test 2 (Business Rule): Creating a sprint where endDate is earlier than startDate fails
    @Test
    void createSprint_EndDateBeforeStartDate_ThrowsException() {
        // 1. ARRANGE: Set endDate 5 days BEFORE startDate
        validRequest.setStartDate(LocalDateTime.of(2026, 10, 10, 0, 0));
        validRequest.setEndDate(LocalDateTime.of(2026, 10, 5, 0, 0));

        // 2. ACT & 3. ASSERT
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> sprintService.createSprint(validRequest)
        );
        assertEquals("Sprint end date cannot be earlier than start date", ex.getMessage());
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    // Test 3: Creating a sprint for a non-existent project throws EntityNotFoundException
    @Test
    void createSprint_ProjectNotFound_ThrowsException() {
        // 1. ARRANGE
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> sprintService.createSprint(validRequest));
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    // Test 4: Fetching sprints by project returns mapped DTO list
    @Test
    void getSprintsByProject_ReturnsList() {
        // 1. ARRANGE
        when(sprintRepository.findByProjectId(1L)).thenReturn(List.of(testSprint));

        // 2. ACT
        List<SprintResponse> sprints = sprintService.getSprintsByProject(1L);

        // 3. ASSERT
        assertEquals(1, sprints.size());
        assertEquals("Sprint 1", sprints.get(0).getName());
    }

    // Test 5 (Business Rule): Updating a sprint with invalid dates also throws IllegalArgumentException
    @Test
    void updateSprint_EndDateBeforeStartDate_ThrowsException() {
        // 1. ARRANGE
        validRequest.setStartDate(LocalDateTime.of(2026, 11, 15, 0, 0));
        validRequest.setEndDate(LocalDateTime.of(2026, 11, 1, 0, 0));

        // 2. ACT & 3. ASSERT
        assertThrows(IllegalArgumentException.class, () -> sprintService.updateSprint(5L, validRequest));
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    // Test 6: Deleting a sprint cascades to delete its tasks and comments
    @Test
    void deleteSprint_CascadesTasksAndComments() {
        // 1. ARRANGE
        Task task = new Task();
        task.setId(100L);
        task.setSprint(testSprint);

        when(sprintRepository.existsById(5L)).thenReturn(true);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(testSprint));
        when(taskRepository.findBySprintId(5L)).thenReturn(List.of(task));
        when(commentRepository.findByTaskId(100L)).thenReturn(Collections.emptyList());

        // 2. ACT
        sprintService.deleteSprint(5L);

        // 3. ASSERT
        verify(commentRepository, times(1)).deleteAll(any());
        verify(taskRepository, times(1)).deleteAll(any());
        verify(sprintRepository, times(1)).delete(testSprint);
    }
}
