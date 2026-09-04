package com.tracker.backend.service;

import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.TaskResponse;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate; // For sending messages to WebSocket clients

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository,SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
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

    public void deleteTask(Long id) {
        Task task = findTask(id);
        Long sprintId = task.getSprint().getId();
        taskRepository.delete(task);
        
        // BROADCAST: Tell all users a task was deleted
        // We send a simple map with the ID so the frontend knows which one to remove from the screen
        Object deletionMessage = Map.of("deletedId", id);
        messagingTemplate.convertAndSend(taskTopic(sprintId), deletionMessage);
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
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
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
