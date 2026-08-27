package com.tracker.backend.service;

import com.tracker.backend.dto.CreateTaskRequest;
import com.tracker.backend.dto.TaskResponse;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    public TaskService(TaskRepository taskRepository, SprintRepository sprintRepository) {
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
        Sprint sprint = findSprint(request.getSprintId());

        Task task = new Task();
        applyRequest(task, request);
        task.setSprint(sprint);
        return mapToResponse(taskRepository.save(task));
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
        applyRequest(task, request);
        task.setSprint(findSprint(request.getSprintId()));
        return mapToResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        taskRepository.delete(findTask(id));
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
}
