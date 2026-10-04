package com.tracker.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// * Simple public Health-Check Controller for Render deployment monitoring.
// * Render (and you in your browser) can open GET /api/health without a JWT token
// * to verify that the Spring Boot server is awake and running.
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Task & Sprint Tracker Backend"
        ));
    }
}
