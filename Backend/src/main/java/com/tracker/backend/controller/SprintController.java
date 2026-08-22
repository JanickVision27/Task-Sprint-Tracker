package com.tracker.backend.controller;

import com.tracker.backend.dto.CreateSprintRequest;
import com.tracker.backend.dto.SprintResponse;
import com.tracker.backend.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// * @RestController tells Spring this class handles web traffic and returns data (not HTML pages).
// * @RequestMapping sets the base web address for all routes in this file to "/api/sprints".

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintService sprintService;

    // * Spring automatically gives us the Service worker when the app starts.
    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    // CREATE
    // * @PostMapping means this route handles creating new items.
    // * @Valid checks the user's data for mistakes (like missing required fields)
    // before we process it.
    // * @RequestBody grabs the user's data from the incoming message body.

    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(@Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.createSprint(request);

        // ! HttpStatus.CREATED sends a 201 status. This is the correct way to say "New
        // * item successfully created", instead of just a basic 200 OK.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // READ (By Project)
    // * @GetMapping means this route handles reading/fetching data.
    // * @PathVariable grabs the variable straight from the web address URL
    // (e.g.,api/sprints/project/5).
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintResponse>> getSprintsByProject(@PathVariable Long projectId) {
        List<SprintResponse> responses = sprintService.getSprintsByProject(projectId);

        // * ResponseEntity.ok() sends a 200 status, meaning "Success, here is your
        // data".
        return ResponseEntity.ok(responses);
    }

    // READ (Single)
    @GetMapping("/{id}")
    public ResponseEntity<SprintResponse> getSprintById(@PathVariable Long id) {
        SprintResponse response = sprintService.getSprintById(id);
        return ResponseEntity.ok(response);
    }

    // UPDATE
    // * @PutMapping means this route handles changing existing items.
    // * It needs both the ID from the URL to find the item, and the new details
    // from the message body.
    @PutMapping("/{id}")
    public ResponseEntity<SprintResponse> updateSprint(@PathVariable Long id,
            @Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.updateSprint(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE
    // * @DeleteMapping means this route handles removing items.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);

        // ! noContent().build() sends a 204 status. It means "Action successful, but there is no data left to show you".
        return ResponseEntity.noContent().build();
    }

}
