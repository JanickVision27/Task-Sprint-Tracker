package com.tracker.backend.controller;

import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.SprintResponse;
import com.tracker.backend.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    // CREATE - Restricted to ADMIN and MANAGER roles
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SprintResponse> createSprint(@Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.createSprint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // READ (By Project) - Accessible to all authenticated users
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintResponse>> getSprintsByProject(@PathVariable Long projectId) {
        List<SprintResponse> responses = sprintService.getSprintsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    // READ (Single) - Accessible to all authenticated users
    @GetMapping("/{id}")
    public ResponseEntity<SprintResponse> getSprintById(@PathVariable Long id) {
        SprintResponse response = sprintService.getSprintById(id);
        return ResponseEntity.ok(response);
    }

    // UPDATE - Restricted to ADMIN and MANAGER roles
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SprintResponse> updateSprint(@PathVariable Long id,
            @Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.updateSprint(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE - Restricted to ADMIN and MANAGER roles
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.noContent().build();
    }
}
