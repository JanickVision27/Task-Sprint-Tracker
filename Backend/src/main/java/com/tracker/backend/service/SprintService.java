package com.tracker.backend.service;

import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.SprintResponse;
import com.tracker.backend.entity.Project;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.ProjectRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// * @Service marks this class as the business-logic layer for Sprints.
@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    // * Constructor Injection: Spring automatically provides the repositories when starting up.
    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository, TaskRepository taskRepository, CommentRepository commentRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
    }

    // 1. CREATE SPRINT
    public SprintResponse createSprint(CreateSprintRequest request) {
        // * Business Rule Check: Ensure the sprint's endDate is not earlier than its startDate
        validateSprintDates(request.getStartDate(), request.getEndDate());

        // * Verify the parent Project exists before linking a Sprint to it
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Project not found with id: " + request.getProjectId()));

        Sprint sprint = new Sprint();
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setProject(project);

        Sprint savedSprint = sprintRepository.save(sprint);
        return mapToResponse(savedSprint);
    }

    // 2. READ ALL SPRINTS FOR A PROJECT
    public List<SprintResponse> getSprintsByProject(Long projectId) {
        return sprintRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. READ ONE SPRINT BY ID
    public SprintResponse getSprintById(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sprint not found with id: " + id));

        return mapToResponse(sprint);
    }

    // 4. UPDATE SPRINT
    public SprintResponse updateSprint(Long id, CreateSprintRequest request) {
        // * Business Rule Check: Ensure updated dates are still logical
        validateSprintDates(request.getStartDate(), request.getEndDate());

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sprint not found with id: " + id));

        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        Sprint updatedSprint = sprintRepository.save(sprint);
        return mapToResponse(updatedSprint);
    }

    // 5. DELETE SPRINT (with Cascade Cleanup)
    // * @Transactional ensures that deleting comments, tasks, and the sprint happens as one unit:
    // * if anything fails midway, the database rolls back so no partial data is left behind.
    @Transactional
    public void deleteSprint(Long id) {
        if (!sprintRepository.existsById(id)) {
            throw new EntityNotFoundException("Sprint not found with id: " + id);
        }
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found with id: " + id));

        // First delete comments on each task, then the tasks, then the sprint itself
        var tasks = taskRepository.findBySprintId(id);
        tasks.forEach(task -> commentRepository.deleteAll(commentRepository.findByTaskId(task.getId())));
        taskRepository.deleteAll(tasks);
        sprintRepository.delete(sprint);
    }

    // --- BUSINESS RULE HELPER: Prevents endDate from being earlier than startDate ---
    private void validateSprintDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Sprint end date cannot be earlier than start date");
        }
    }

    // --- HELPER METHOD: Converts Entity to DTO ---
    private SprintResponse mapToResponse(Sprint sprint) {
        SprintResponse response = new SprintResponse();
        response.setId(sprint.getId());
        response.setName(sprint.getName());
        response.setStartDate(sprint.getStartDate());
        response.setEndDate(sprint.getEndDate());
        response.setProjectId(sprint.getProject().getId());
        response.setCreatedAt(sprint.getCreatedAt());
        response.setUpdatedAt(sprint.getUpdatedAt());
        return response;
    }
}
