package com.tracker.backend.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;

//@RestControllerAdvice tells Spring: "Listen to ALL controllers. If an error happens, step in."
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
// ! This method specifically catches the EntityNotFoundException we throw in our Services
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        
        // Build a clean JSON response body
        Map<String, Object> body = Map.of(
            "timestamp", LocalDateTime.now(),
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        );

        // Return the JSON with a 404 status code
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
