package com.tracker.backend.service;

import com.tracker.backend.dto.CreateProjectRequest;
import com.tracker.backend.dto.ProjectResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// * Unit tests for ProjectService covering project CRUD and cascading deletion.
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private CreateProjectRequest request;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Kanban Platform");
        testProject.setDescription("Real-time sprint tracker");

        request = new CreateProjectRequest();
        request.setName("Kanban Platform");
        request.setDescription("Real-time sprint tracker");
    }

    @Test
    void createProject_Success() {
        // 1. ARRANGE
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // 2. ACT
        ProjectResponse response = projectService.createProject(request);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Kanban Platform", response.getName());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void getAllProjects_ReturnsList() {
        // 1. ARRANGE
        when(projectRepository.findAll()).thenReturn(List.of(testProject));

        // 2. ACT
        List<ProjectResponse> projects = projectService.getAllProjects();

        // 3. ASSERT
        assertEquals(1, projects.size());
        assertEquals("Kanban Platform", projects.get(0).getName());
    }

    @Test
    void getProjectById_NotFound_ThrowsException() {
        // 1. ARRANGE
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> projectService.getProjectById(99L));
    }

    @Test
    void updateProject_Success() {
        // 1. ARRANGE
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // 2. ACT
        ProjectResponse response = projectService.updateProject(1L, request);

        // 3. ASSERT
        assertEquals("Kanban Platform", response.getName());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void deleteProject_CascadesSprintsTasksAndComments() {
        // 1. ARRANGE
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProject(testProject);

        Task task = new Task();
        task.setId(100L);
        task.setSprint(sprint);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(sprintRepository.findByProjectId(1L)).thenReturn(List.of(sprint));
        when(taskRepository.findBySprintId(10L)).thenReturn(List.of(task));
        when(commentRepository.findByTaskId(100L)).thenReturn(Collections.emptyList());

        // 2. ACT
        projectService.deleteProject(1L);

        // 3. ASSERT
        verify(commentRepository, times(1)).deleteAll(any());
        verify(taskRepository, times(1)).deleteAll(any());
        verify(sprintRepository, times(1)).deleteAll(any());
        verify(projectRepository, times(1)).delete(testProject);
    }
}
