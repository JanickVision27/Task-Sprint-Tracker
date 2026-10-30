package com.tracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * * Answer for "is this reset link still good?" so the UI can show a friendly
 * * "link expired, request a new one" screen instead of failing on submit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetTokenValidationResponse {

    private boolean valid;
    private String message;

    /** Masked account address (e.g. al***@example.com), only when the link is valid. */
    private String email;
}
