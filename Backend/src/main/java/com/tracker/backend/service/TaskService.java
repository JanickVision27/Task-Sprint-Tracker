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
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository, SimpMessagingTemplate messagingTemplate, CommentRepository commentRepository) {
        this(taskRepository, sprintRepository, messagingTemplate, commentRepository, null);
    }

    @Autowired
    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository, SimpMessagingTemplate messagingTemplate, CommentRepository commentRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
        this.messagingTemplate = messagingTemplate;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
        validateTaskBusinessRules(request);
        Sprint sprint = findSprint(request.getSprintId());

        Task task = new Task();
        applyRequest(task, request);
        task.setSprint(sprint);

        TaskResponse response = mapToResponse(taskRepository.save(task));

        // BROADCAST: Tell all users a new task was created
        broadcastTaskUpdate(response);

        return response;
    }

    public List<TaskResponse> getTasksBySprint(Long sprintId) {
        return taskRepository.findBySprintId(sprintId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        return mapToResponse(findTask(id));
    }

    public TaskResponse updateTask(Long id, CreateTaskRequest request) {
        Task task = findTask(id);
        enforceMemberTaskOwnership(task);
        validateTaskBusinessRules(request);

        Long previousSprintId = task.getSprint().getId();
        applyRequest(task, request);
        task.setSprint(findSprint(request.getSprintId()));

        TaskResponse response = mapToResponse(taskRepository.save(task));

        // BROADCAST: Tell all users a task was updated (e.g., moved on the Kanban board)
        broadcastTaskUpdate(response);
        // If an update ever moves a task to another sprint, refresh viewers of both boards.
        if (!previousSprintId.equals(response.getSprintId())) {
            broadcastTaskUpdate(previousSprintId, response);
        }

        return response;
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTask(id);
        enforceMemberTaskOwnership(task);

        Long sprintId = task.getSprint().getId();
        commentRepository.deleteAll(commentRepository.findByTaskId(id));
        taskRepository.delete(task);

        // BROADCAST: Tell all users a task was deleted
        Object deletionMessage = Map.of("deletedId", id);
        messagingTemplate.convertAndSend(taskTopic(sprintId), deletionMessage);
    }

    private void validateTaskBusinessRules(CreateTaskRequest request) {
        if (request.getStatus() == TaskStatus.DONE && request.getAssigneeId() == null) {
            throw new IllegalArgumentException("Cannot move task to DONE without an assignee");
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            String normalizedPriority = request.getPriority().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_PRIORITIES.contains(normalizedPriority)) {
                throw new IllegalArgumentException("Task priority must be LOW, MEDIUM, or HIGH");
            }
            request.setPriority(normalizedPriority);
        }
    }

    private void enforceMemberTaskOwnership(Task existingTask) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        boolean isAdminOrManager = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));
        if (isAdminOrManager) {
            return;
        }
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
