package com.tracker.backend.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

// * @RestControllerAdvice acts like a global "safety net" (try-catch) across ALL controllers.
// * Whenever any Controller or Service throws an exception, this class catches it and returns
// * a clean, structured JSON error response instead of crashing with a raw 500 Server Error.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ? 1. Handles @Valid annotation failures (e.g., blank title, missing sprintId) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", message);
        return ResponseEntity.badRequest().body(body);
    }

    // ? 2. Handles our custom business-rule errors from Services -> 400 Bad Request
    // ! Examples:
    // ! - Moving a task to DONE without an assignee (IllegalArgumentException)
    // ! - Creating a sprint where endDate is before startDate (IllegalArgumentException)
    // ! - Registering with an email that is already taken (IllegalStateException)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessRuleViolation(RuntimeException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage() != null ? ex.getMessage() : "Invalid request"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ? 3. Handles role-based permission errors (@PreAuthorize or MEMBER ownership check) -> 403 Forbidden
    // ! Example: A MEMBER tries to create a Sprint or tries to move another teammate's task.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 403,
                "error", "Forbidden",
                "message", ex.getMessage() != null ? ex.getMessage() : "You do not have permission to perform this action"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ? 4. Handles missing database records (EntityNotFoundException) -> 404 Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ? 5. Handles wrong email or password during login -> 401 Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", "Invalid email or password"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
