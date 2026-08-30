package com.tracker.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponse {
    private Long id;
    private String text;
    private Long taskId;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
