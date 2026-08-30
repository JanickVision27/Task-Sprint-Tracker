package com.tracker.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;

    public AuthResponse(String message) {
        this.message = message;
    }
}