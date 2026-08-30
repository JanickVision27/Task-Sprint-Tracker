package com.tracker.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String token; // NEW: This will hold the JWT keycard string

    //! Constructor for Registration (no token yet)
    public AuthResponse(String message) {
        this.message = message;
    }

    //! Constructor for Login (includes the token)
    public AuthResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }
}