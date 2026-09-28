package com.tracker.backend.service;

import com.tracker.backend.dto.CreateProjectRequest;
import com.tracker.backend.dto.ProjectResponse;
import com.tracker.backend.repository.ProjectRepository;
import com.tracker.backend.repository.SprintRepository;
import com.tracker.backend.repository.TaskRepository;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.entity.Project;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    public ProjectService(ProjectRepository projectRepository, SprintRepository sprintRepository, TaskRepository taskRepository, CommentRepository commentRepository) {
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
    }

    // 1. CREATE
    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project savedProject = projectRepository.save(project);

        return mapToResponse(savedProject);
    }

    // 2. READ ALL
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. READ ONE
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        return mapToResponse(project);
    }

    // 4. UPDATE
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updatedProject = projectRepository.save(project);

        return mapToResponse(updatedProject);
    }

    // 5. DELETE
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        var sprints = sprintRepository.findByProjectId(id);
        sprints.forEach(sprint -> {
            var tasks = taskRepository.findBySprintId(sprint.getId());
            tasks.forEach(task -> commentRepository.deleteAll(commentRepository.findByTaskId(task.getId())));
            taskRepository.deleteAll(tasks);
        });
        sprintRepository.deleteAll(sprints);
        projectRepository.delete(project);
    }

    // --- HELPER METHOD: Converts Entity to DTO ---
    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }

}
