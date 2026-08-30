package com.tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {
    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment text must be less than 1000 characters")
    private String text;

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long authorId;
}
