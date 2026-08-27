package com.tracker.backend.dto;

import com.tracker.backend.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {
    @NotBlank(message = "Task title is required")
    @Size(max = 100, message = "Task title must be less than 100 characters")
    private String title;

    @Size(max = 1000, message = "Task description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Task status is required")
    private TaskStatus status;

    @Size(max = 20, message = "Task priority must be less than 20 characters")
    private String priority;

    @NotNull(message = "Sprint ID is required")
    private Long sprintId;

    private Long assigneeId;
}
