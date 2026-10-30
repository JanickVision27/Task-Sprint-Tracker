package com.tracker.backend.controller;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.dto.ForgotPasswordRequest;
import com.tracker.backend.dto.LoginRequest;
import com.tracker.backend.dto.MessageResponse;
import com.tracker.backend.dto.RegisterRequest;
import com.tracker.backend.dto.ResetPasswordRequest;
import com.tracker.backend.dto.ResetTokenValidationResponse;
import com.tracker.backend.service.AuthService;
import com.tracker.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * * Same answer for "account exists" and "no such account", so this endpoint cannot be
     * * used to enumerate registered emails.
     */
    private static final String FORGOT_PASSWORD_ACK =
            "If an account exists for that email, a password reset link is on its way. "
                    + "Check your inbox and spam folder — the link expires in a short while.";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    // Registration endpoint (also triggers the Brevo welcome email)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * * Step 1 of "forgot password": email a single-use reset link via Brevo.
     * * Always returns 200 with a generic message (no user enumeration).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(FORGOT_PASSWORD_ACK));
    }

    /**
     * * Step 2 (optional): lets /reset-password check the link before rendering the form,
     * * so the user sees "this link expired" instead of failing after typing a password.
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<ResetTokenValidationResponse> validateResetToken(@RequestParam("token") String token) {
        PasswordResetService.PasswordResetTokenValidation validation = passwordResetService.validateToken(token);
        return ResponseEntity.ok(new ResetTokenValidationResponse(
                validation.valid(),
                validation.message(),
                validation.maskedEmail()));
    }

    /**
     * * Step 3: store the new BCrypt-hashed password. Invalid/expired tokens become a 400
     * * with a readable message through GlobalExceptionHandler.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse(
                "Your password has been updated. You can now sign in with your new password."));
    }
}
