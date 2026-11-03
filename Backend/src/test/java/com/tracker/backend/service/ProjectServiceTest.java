package com.tracker.backend.service;

import com.tracker.backend.dto.CreateProjectRequest;
import com.tracker.backend.dto.ProjectResponse;
import com.tracker.backend.entity.Project;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.ProjectRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import com.tracker.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private CreateProjectRequest request;
    private User managerUser;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Kanban Platform");
        testProject.setDescription("Real-time sprint tracker");

        request = new CreateProjectRequest();
        request.setName("Kanban Platform");
        request.setDescription("Real-time sprint tracker");

        managerUser = new User(5L, "Maya Manager", "maya@test.com", "hashed_pw", Role.MANAGER);
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

    // * The Admin picks a Manager while creating the project: the choice is stored on the
    // * entity and echoed back in the response so the UI can render it without a second call.
    @Test
    void createProject_WithManager_StoresAndReturnsManagerDetails() {
        // 1. ARRANGE
        request.setManagerId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(managerUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // 2. ACT
        ProjectResponse response = projectService.createProject(request);

        // 3. ASSERT
        assertEquals(5L, response.getManagerId());
        assertEquals("Maya Manager", response.getManagerName());
        assertEquals("maya@test.com", response.getManagerEmail());
        assertEquals(Role.MANAGER, response.getManagerRole());

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertEquals(managerUser, captor.getValue().getManager());
    }

    // * A project can be created without a manager, and no user lookup should happen.
    @Test
    void createProject_WithoutManager_LeavesManagerEmpty() {
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.createProject(request);

        assertNull(response.getManagerId());
        assertNull(response.getManagerName());
        verify(userRepository, never()).findById(any());
    }

    // * A project belongs to a Manager, so assigning a Member must fail with a clear message.
    @Test
    void createProject_ManagerIsNotManagerRole_ThrowsIllegalArgument() {
        request.setManagerId(9L);
        User member = new User(9L, "Mo Member", "mo@test.com", "hashed_pw", Role.MEMBER);
        when(userRepository.findById(9L)).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.createProject(request));

        assertTrue(ex.getMessage().contains("MEMBER"));
        assertTrue(ex.getMessage().contains("MANAGER"));
        verify(projectRepository, never()).save(any(Project.class));
    }

    // * An unknown manager id is a 404-style error, not a project with a dangling reference.
    @Test
    void createProject_UnknownManager_ThrowsEntityNotFound() {
        request.setManagerId(404L);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> projectService.createProject(request));
        verify(projectRepository, never()).save(any(Project.class));
    }

    // * Updating with managerId = null unassigns the current manager.
    @Test
    void updateProject_NullManagerId_ClearsAssignment() {
        testProject.setManager(managerUser);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.updateProject(1L, request);

        assertNull(testProject.getManager());
        assertNull(response.getManagerId());
    }

    // * Reassigning an existing project to a different Manager replaces the old one.
    @Test
    void updateProject_NewManagerId_ReplacesManager() {
        testProject.setManager(managerUser);
        User otherManager = new User(6L, "Ravi Lead", "ravi@test.com", "hashed_pw", Role.MANAGER);
        request.setManagerId(6L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(6L)).thenReturn(Optional.of(otherManager));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.updateProject(1L, request);

        assertEquals(otherManager, testProject.getManager());
        assertEquals("Ravi Lead", response.getManagerName());
    }
}
