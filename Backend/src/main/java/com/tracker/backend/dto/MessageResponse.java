package com.tracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * * Generic { "message": "..." } payload used by the auth endpoints that do not
 * * return a session (forgot password, reset password).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;
}
