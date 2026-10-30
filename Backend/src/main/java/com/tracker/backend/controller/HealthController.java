package com.tracker.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

// * Simple public Health-Check Controller for Render deployment monitoring.
// * Render (and you in your browser) can open GET /api/health without a JWT token
// * to verify that the Spring Boot server is awake and running.
// *
// * It also performs a real `SELECT 1` against the database, so "the app is up but every
// * request fails" is visible immediately instead of hidden behind a green status.
// * The HTTP status stays 200 on purpose: the container itself is healthy, and Render must
// * not restart it just because the database is temporarily unreachable.
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /** Longest database error message returned to the caller. */
    private static final int MAX_ERROR_LENGTH = 200;

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "Task & Sprint Tracker Backend");

        // * Round-trip to the database: catches wrong credentials, a suspended Neon project,
        // * a missing sslmode, or a bad host while the JVM itself is perfectly fine.
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            body.put("database", "UP");
        } catch (Exception ex) {
            body.put("database", "DOWN");
            body.put("databaseError", truncate(ex.getMessage()));
            log.warn("Health check: database is unreachable — {}", ex.getMessage());
        }

        return ResponseEntity.ok(body);
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown database error";
        }
        String singleLine = message.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= MAX_ERROR_LENGTH
                ? singleLine
                : singleLine.substring(0, MAX_ERROR_LENGTH) + "...";
    }
}
