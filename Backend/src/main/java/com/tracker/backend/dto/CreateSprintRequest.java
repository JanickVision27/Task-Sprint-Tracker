package com.tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// * Lombok @Getter/@Setter auto-generates boilerplate getter and setter methods at compile time.
@Getter 
@Setter 
public class CreateSprintRequest {

    // * @NotBlank is for Strings: cannot be null, empty "", or just spaces "   "
    @NotBlank(message = "Sprint name is required")
    @Size(max = 100, message = "Sprint name must be less than 100 characters")
    private String name;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    // * @NotNull is for Objects/Numbers: cannot be null, but can be 0.
    // * The frontend sends this automatically when the user selects a project from a dropdown.
    @NotNull(message = "Project ID is required")
    private Long projectId;
}