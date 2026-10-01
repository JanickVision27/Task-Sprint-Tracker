package com.tracker.backend.service;

import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.TaskResponse;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.entity.TaskStatus;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import com.tracker.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {
    // * Only these 3 priority levels are allowed in our system
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final SimpMessagingTemplate messagingTemplate; // Sends live WebSocket messages to connected browsers
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // Constructor used by simple unit tests that don't need UserRepository
    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository, SimpMessagingTemplate messagingTemplate, CommentRepository commentRepository) {
        this(taskRepository, sprintRepository, messagingTemplate, commentRepository, null);
    }

    // * @Autowired tells Spring Boot to use this full constructor when running the application
    @Autowired
    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository, SimpMessagingTemplate messagingTemplate, CommentRepository commentRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
        this.messagingTemplate = messagingTemplate;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    // 1. CREATE TASK
    public TaskResponse createTask(CreateTaskRequest request) {
        // * Step 1: Check business rules (e.g., cannot create as DONE without assignee, valid priority)
        validateTaskBusinessRules(request);

        // * Step 2: Ensure the target Sprint exists
        Sprint sprint = findSprint(request.getSprintId());

        // * Step 3: Build and save the Task entity
        Task task = new Task();
        applyRequest(task, request);
        task.setSprint(sprint);

        TaskResponse response = mapToResponse(taskRepository.save(task));

        // * Step 4: Broadcast the new task over WebSockets so teammates see it appear live!
        broadcastTaskUpdate(response);

        return response;
    }

    // 2. READ ALL TASKS IN A SPRINT
    public List<TaskResponse> getTasksBySprint(Long sprintId) {
        return taskRepository.findBySprintId(sprintId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. READ SINGLE TASK
    public TaskResponse getTaskById(Long id) {
        return mapToResponse(findTask(id));
    }

    // 4. UPDATE TASK (Used when dragging a card across columns or assigning a user)
    public TaskResponse updateTask(Long id, CreateTaskRequest request) {
        Task task = findTask(id);

        // * Step 1: Role-Based Check — If logged-in user is a MEMBER, they can only move their own tasks
        enforceMemberTaskOwnership(task);

        // * Step 2: Business Rule Check — Cannot move a task to DONE unless assigneeId is present
        validateTaskBusinessRules(request);

        Long previousSprintId = task.getSprint().getId();
        applyRequest(task, request);
        task.setSprint(findSprint(request.getSprintId()));

        TaskResponse response = mapToResponse(taskRepository.save(task));

        // * Step 3: Broadcast the updated task over WebSockets so all open boards move the card live
        broadcastTaskUpdate(response);
        if (!previousSprintId.equals(response.getSprintId())) {
            broadcastTaskUpdate(previousSprintId, response);
        }

        return response;
    }

    // 5. DELETE TASK
    @Transactional
    public void deleteTask(Long id) {
        Task task = findTask(id);
        enforceMemberTaskOwnership(task);

        Long sprintId = task.getSprint().getId();
        commentRepository.deleteAll(commentRepository.findByTaskId(id));
        taskRepository.delete(task);

        // * Broadcast task deletion to all connected clients on this sprint board
        Object deletionMessage = Map.of("deletedId", id);
        messagingTemplate.convertAndSend(taskTopic(sprintId), deletionMessage);
    }

    // --- BUSINESS RULE 1 & 2: Assignee required for DONE + Priority validation ---
    private void validateTaskBusinessRules(CreateTaskRequest request) {
        // Rule 1: A task cannot be marked DONE unless someone is assigned to it
        if (request.getStatus() == TaskStatus.DONE && request.getAssigneeId() == null) {
            throw new IllegalArgumentException("Cannot move task to DONE without an assignee");
        }

        // Rule 2: If priority is provided, it must be LOW, MEDIUM, or HIGH
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            String normalizedPriority = request.getPriority().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_PRIORITIES.contains(normalizedPriority)) {
                throw new IllegalArgumentException("Task priority must be LOW, MEDIUM, or HIGH");
            }
            request.setPriority(normalizedPriority);
        }
    }

    // --- ROLE-BASED RULE: MEMBER can only modify/move their own tasks (ADMIN & MANAGER can modify any) ---
    private void enforceMemberTaskOwnership(Task existingTask) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        // Check if the user has ROLE_ADMIN or ROLE_MANAGER
        boolean isAdminOrManager = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));
        if (isAdminOrManager) {
            return; // Admins and Managers can move/edit any task
        }

        // Otherwise, the user is a MEMBER: check if the task is already assigned to someone else
        if (userRepository != null && auth.getName() != null) {
            userRepository.findByEmail(auth.getName()).ifPresent(currentUser -> {
                if (existingTask.getAssigneeId() != null && !existingTask.getAssigneeId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Members can only move or modify tasks assigned to them");
                }
            });
        }
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }

    private Sprint findSprint(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found with id: " + id));
    }

    private void applyRequest(Task task, CreateTaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null && !request.getPriority().isBlank()
                ? request.getPriority().trim().toUpperCase(Locale.ROOT)
                : "MEDIUM");
        task.setAssigneeId(request.getAssigneeId());
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setSprintId(task.getSprint().getId());
        response.setAssigneeId(task.getAssigneeId());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    private void broadcastTaskUpdate(TaskResponse taskResponse) {
        broadcastTaskUpdate(taskResponse.getSprintId(), taskResponse);
    }

    private void broadcastTaskUpdate(Long sprintId, TaskResponse taskResponse) {
        messagingTemplate.convertAndSend(taskTopic(sprintId), taskResponse);
    }

    private String taskTopic(Long sprintId) {
        return "/topic/sprints/" + sprintId + "/tasks";
    }
}
