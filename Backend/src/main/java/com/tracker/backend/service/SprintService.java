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

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository, TaskRepository taskRepository, CommentRepository commentRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
    }

    // 1. Create
    public SprintResponse createSprint(CreateSprintRequest request) {
        validateSprintDates(request.getStartDate(), request.getEndDate());

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

    // 2. Read ALL Sprints For a Specific Project
    public List<SprintResponse> getSprintsByProject(Long projectId) {
        return sprintRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. READ ONE SPRINT
    public SprintResponse getSprintById(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sprint not found with id: " + id));

        return mapToResponse(sprint);
    }

    // 4. UPDATE
    public SprintResponse updateSprint(Long id, CreateSprintRequest request) {
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

    // 5. DELETE
    @Transactional
    public void deleteSprint(Long id) {
        if (!sprintRepository.existsById(id)) {
            throw new EntityNotFoundException("Sprint not found with id: " + id);
        }
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found with id: " + id));
        var tasks = taskRepository.findBySprintId(id);
        tasks.forEach(task -> commentRepository.deleteAll(commentRepository.findByTaskId(task.getId())));
        taskRepository.deleteAll(tasks);
        sprintRepository.delete(sprint);
    }

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
