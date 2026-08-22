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
    
    // ! We return just the projectId instead of the whole Project object.
    // * Reason 1: Prevents Infinite Recursion (Sprint -> Project -> Sprint -> Project...)
    // * Reason 2: Keeps the JSON response small and fast over the network.
    private Long projectId; 
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}