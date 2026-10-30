package com.tracker.backend.dto;

import com.tracker.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    // * Matches the reset-password rule so an account can never be created with a weaker
    // * password than the reset flow would allow. 72 is BCrypt's effective input limit.
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    // If the user doesn't provide a role, we will default them to MEMBER
    private Role role;
}