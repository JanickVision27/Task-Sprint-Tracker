package com.tracker.backend.dto;

import jakarta.validation.constraints.Email; // Validates that the string is a proper email format
import jakarta.validation.constraints.NotBlank; // Validates that the string is not null or empty
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
