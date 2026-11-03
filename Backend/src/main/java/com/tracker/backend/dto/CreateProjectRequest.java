package com.tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be less than 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    /**
     * * Id of the MANAGER who should own this project.
     *
     * * Optional on purpose: leaving it out (or sending null) creates the project without a
     * * manager, which keeps the endpoint backward compatible for existing API clients.
     */
    @Positive(message = "Manager id must be a positive number")
    private Long managerId;
}
