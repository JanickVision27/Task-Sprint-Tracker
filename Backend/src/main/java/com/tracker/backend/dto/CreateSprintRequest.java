package com.tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateSprintRequest {

    @NotBlank(message = "Sprint name is required")
    @Size(max = 100, message = "Sprint name must be less than 100 characters")
    private String name;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull(message = "Project ID is required")
    private Long projectId;
}
