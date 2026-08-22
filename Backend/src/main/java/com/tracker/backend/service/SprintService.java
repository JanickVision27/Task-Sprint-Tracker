package com.tracker.backend.service;

import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.SprintResponse;
import com.tracker.backend.entity.Project;
import com.tracker.backend.entity.Sprint;
import com.tracker.backend.repository.ProjectRepository;
import com.tracker.backend.repository.SprintRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// * @Service tags this as a worker class. Spring manages it for us.
@Service
public class SprintService {

    private final SprintRepository sprintRepository;

    // * We need two database tools here. We need the Project tool to verify the
    // Project exists before linking a Sprint to it.
    // ! If we don't check first, we might link a Sprint to a missing Project, which
    // breaks the database rules.
    private final ProjectRepository projectRepository;

    // * Spring automatically gives us our database tools when the app starts.
    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;

    }

    // 1. Create
    public SprintResponse createSprint(CreateSprintRequest request) {

        // * Step 1: Find the Project.
        // ! orElseThrow means: "If the Project is missing, stop immediately and send an
        // error." This stops the database from crashing.
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Project not found with id: " + request.getProjectId()));

        // * Step 2: Create the Sprint and set its basic fields
        Sprint sprint = new Sprint();
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        // * Step 3: Link the Sprint to the Project
        sprint.setProject(project);

        // * Step 4: Save the Sprint to the database
        Sprint savedSprint = sprintRepository.save(sprint);

        // * Step 5: Return the response
        return mapToResponse(savedSprint);
    }

    // 2. Read ALL Sprints For a Specific Project
    public List<SprintResponse> getSprintsByProject(Long projectId) {
        // * We use our custom search to find Sprints. Then we convert the raw database
        // items into simple, safe DTOs for the user.
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
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sprint not found with id: " + id));

        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        // ? Design choice: We don't let users move a Sprint to a different Project
        // here. If we wanted to allow it, we would find the new Project and attach it.

        Sprint updatedSprint = sprintRepository.save(sprint);
        return mapToResponse(updatedSprint);
    }

    // 5. DELETE
    public void deleteSprint(Long id) {
        // ! We check if it exists first. If we skip this, deleting a missing item
        // causes an ugly system error. This gives a clean "Not Found" message instead.
        if (!sprintRepository.existsById(id)) {
            throw new EntityNotFoundException("Sprint not found with id: " + id);
        }
        sprintRepository.deleteById(id);
    }

    // --- HELPER METHOD: Converts Entity to DTO ---
    private SprintResponse mapToResponse(Sprint sprint) {
        SprintResponse response = new SprintResponse();
        response.setId(sprint.getId());
        response.setName(sprint.getName());
        response.setStartDate(sprint.getStartDate());
        response.setEndDate(sprint.getEndDate());

        // * The raw database item holds the whole Project object. The user only needs
        // the Project's ID number, so we pull just the ID out.
        response.setProjectId(sprint.getProject().getId());

        response.setCreatedAt(sprint.getCreatedAt());
        response.setUpdatedAt(sprint.getUpdatedAt());

        return response;
    }
}