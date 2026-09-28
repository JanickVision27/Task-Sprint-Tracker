package com.tracker.backend.service;

import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.TaskResponse;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import com.tracker.backend.entity.TaskStatus;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private CommentRepository commentRepository; // ADDED: Mock for CommentRepository

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TaskService taskService;

    private Sprint testSprint;
    private Task testTask;
    private CreateTaskRequest createRequest;

    @BeforeEach
    void setUp() {
        testSprint = new Sprint();
        testSprint.setId(1L);
        testSprint.setName("Sprint 1");

        testTask = new Task();
        testTask.setId(10L);
        testTask.setTitle("Test Task");
        testTask.setSprint(testSprint);
        testTask.setStatus(TaskStatus.TODO);// Using String for status

        createRequest = new CreateTaskRequest();
        createRequest.setTitle("New Task");
        createRequest.setSprintId(1L);
    }

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

    @Test
    void deleteTask_Success() {
        // 1. ARRANGE
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).delete(any(Task.class));
        doNothing().when(commentRepository).deleteAll(any()); // Mock the comment deletion

        // 2. ACT
        taskService.deleteTask(10L);

        // 3. ASSERT
        verify(taskRepository, times(1)).delete(testTask);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/sprints/1/tasks"), any(Object.class));
    }

    @Test
    void createTask_SprintNotFound_ThrowsException() {
        // 1. ARRANGE
        when(sprintRepository.findById(anyLong())).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(RuntimeException.class, () -> {
            taskService.createTask(createRequest);
        });

        verify(taskRepository, never()).save(any(Task.class));
        // FIX: Ensure never() is explicitly included
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}