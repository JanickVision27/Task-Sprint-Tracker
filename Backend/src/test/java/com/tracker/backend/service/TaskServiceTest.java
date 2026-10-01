package com.tracker.backend.service;

import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.TaskResponse;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.entity.TaskStatus;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import com.tracker.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// * @ExtendWith(MockitoExtension.class) tells JUnit 5 to create fake ("mock") repositories
// * so we can test TaskService logic in isolation without needing a real database.
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TaskService taskService;

    private Sprint testSprint;
    private Task testTask;
    private CreateTaskRequest createRequest;

    @BeforeEach
    void setUp() {
        // Initialize TaskService with all mocked dependencies
        taskService = new TaskService(
                taskRepository,
                sprintRepository,
                messagingTemplate,
                commentRepository,
                userRepository
        );

        testSprint = new Sprint();
        testSprint.setId(1L);
        testSprint.setName("Sprint 1");

        testTask = new Task();
        testTask.setId(10L);
        testTask.setTitle("Test Task");
        testTask.setSprint(testSprint);
        testTask.setStatus(TaskStatus.TODO);
        testTask.setPriority("MEDIUM");

        createRequest = new CreateTaskRequest();
        createRequest.setTitle("New Task");
        createRequest.setSprintId(1L);
        createRequest.setStatus(TaskStatus.TODO);
        createRequest.setPriority("MEDIUM");
    }

    @AfterEach
    void tearDown() {
        // Clear any fake logged-in user after each test so tests never affect each other
        SecurityContextHolder.clearContext();
    }

    // Test 1: Creating a valid task saves to DB and broadcasts to WebSocket
    @Test
    void createTask_Success() {
        // 1. ARRANGE
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // 2. ACT
        TaskResponse response = taskService.createTask(createRequest);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Test Task", response.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/sprints/1/tasks"), any(Object.class));
    }

    // Test 2: Creating a task for a missing sprint throws EntityNotFoundException
    @Test
    void createTask_SprintNotFound_ThrowsException() {
        // 1. ARRANGE
        when(sprintRepository.findById(anyLong())).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> taskService.createTask(createRequest));
        verify(taskRepository, never()).save(any(Task.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // Test 3 (Business Rule): Cannot create a task with status DONE if assigneeId is null
    @Test
    void createTask_DoneWithoutAssignee_ThrowsException() {
        // 1. ARRANGE
        createRequest.setStatus(TaskStatus.DONE);
        createRequest.setAssigneeId(null);

        // 2. ACT & 3. ASSERT
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(createRequest)
        );
        assertEquals("Cannot move task to DONE without an assignee", ex.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Test 4 (Business Rule): Invalid priority string is rejected
    @Test
    void createTask_InvalidPriority_ThrowsException() {
        // 1. ARRANGE
        createRequest.setPriority("URGENT_INVALID");

        // 2. ACT & 3. ASSERT
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(createRequest)
        );
        assertEquals("Task priority must be LOW, MEDIUM, or HIGH", ex.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Test 5 (Business Rule): Moving a task to DONE succeeds when assigneeId is provided
    @Test
    void updateTask_MoveToDoneWithAssignee_Success() {
        // 1. ARRANGE
        createRequest.setStatus(TaskStatus.DONE);
        createRequest.setAssigneeId(5L);

        Task updatedEntity = new Task();
        updatedEntity.setId(10L);
        updatedEntity.setTitle("New Task");
        updatedEntity.setStatus(TaskStatus.DONE);
        updatedEntity.setPriority("MEDIUM");
        updatedEntity.setAssigneeId(5L);
        updatedEntity.setSprint(testSprint);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedEntity);

        // 2. ACT
        TaskResponse response = taskService.updateTask(10L, createRequest);

        // 3. ASSERT
        assertEquals(TaskStatus.DONE, response.getStatus());
        assertEquals(5L, response.getAssigneeId());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/sprints/1/tasks"), any(Object.class));
    }

    // Test 6 (Business Rule): Moving an existing task to DONE without an assignee fails
    @Test
    void updateTask_MoveToDoneWithoutAssignee_ThrowsException() {
        // 1. ARRANGE
        createRequest.setStatus(TaskStatus.DONE);
        createRequest.setAssigneeId(null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));

        // 2. ACT & 3. ASSERT
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(10L, createRequest)
        );
        assertEquals("Cannot move task to DONE without an assignee", ex.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Test 7 (RBAC): A MEMBER cannot move or update a task assigned to another teammate
    @Test
    void updateTask_MemberUpdatingSomeoneElsesTask_ThrowsAccessDenied() {
        // 1. ARRANGE: Task is assigned to user #99, but logged-in MEMBER is user #2
        testTask.setAssigneeId(99L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));

        User loggedInMember = new User(2L, "Member User", "member@test.com", "hashed", Role.MEMBER);
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(loggedInMember));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "member@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                )
        );

        // 2. ACT & 3. ASSERT
        assertThrows(AccessDeniedException.class, () -> taskService.updateTask(10L, createRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Test 8 (RBAC): A MEMBER can move their own assigned task
    @Test
    void updateTask_MemberUpdatingOwnTask_Success() {
        // 1. ARRANGE: Both task assigneeId and logged-in MEMBER have ID #2
        testTask.setAssigneeId(2L);
        createRequest.setAssigneeId(2L);
        createRequest.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        User loggedInMember = new User(2L, "Member User", "member@test.com", "hashed", Role.MEMBER);
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(loggedInMember));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "member@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                )
        );

        // 2. ACT
        TaskResponse response = taskService.updateTask(10L, createRequest);

        // 3. ASSERT
        assertNotNull(response);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    // Test 9: Deleting a task deletes its comments and broadcasts deletion to WebSocket
    @Test
    void deleteTask_Success() {
        // 1. ARRANGE
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(commentRepository.findByTaskId(10L)).thenReturn(Collections.emptyList());
        doNothing().when(commentRepository).deleteAll(any());
        doNothing().when(taskRepository).delete(any(Task.class));

        // 2. ACT
        taskService.deleteTask(10L);

        // 3. ASSERT
        verify(taskRepository, times(1)).delete(testTask);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/sprints/1/tasks"), any(Object.class));
    }
}
