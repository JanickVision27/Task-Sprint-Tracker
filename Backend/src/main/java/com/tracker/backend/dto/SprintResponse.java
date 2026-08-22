package com.tracker.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SprintResponse {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
