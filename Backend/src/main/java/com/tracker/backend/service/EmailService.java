package com.tracker.backend.service;

import com.tracker.backend.config.AsyncConfig;
import com.tracker.backend.entity.Role;
import com.tracker.backend.service.mail.EmailSender;
import com.tracker.backend.service.mail.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * * Application-level email use cases: the welcome/confirmation email after sign-up and the
 * * password-reset link.
 *
 * * Both methods are @Async, so the HTTP response to the browser never waits on Brevo.
 * * Every failure is swallowed and logged: an unreachable mail provider must never make
 * * registration or a password reset look broken to the user.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender emailSender;
    private final String frontendUrl;

    public EmailService(EmailSender emailSender,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl == null ? "" : frontendUrl.trim().replaceAll("/+$", "");
    }

    @Async(AsyncConfig.EMAIL_EXECUTOR)
    public void sendWelcomeEmail(String name, String email, Role role) {
        try {
            String dashboardUrl = frontendUrl + "/dashboard";
            String roleLabel = roleLabel(role);

            emailSender.sendHtml(
                    email,
                    name,
                    EmailTemplates.WELCOME_SUBJECT,
                    EmailTemplates.welcome(name, email, roleLabel, dashboardUrl),
                    EmailTemplates.welcomePlainText(name, email, roleLabel, dashboardUrl));

            if (emailSender.isConfigured()) {
                log.info("Welcome email dispatched to new account {}", EmailSender.mask(email));
            }
        } catch (Exception ex) {
            // * Registration already succeeded; the user can still sign in.
            log.warn("Could not send the welcome email to {}: {}", EmailSender.mask(email), ex.getMessage());
        }
    }

    @Async(AsyncConfig.EMAIL_EXECUTOR)
    public void sendPasswordResetEmail(String name, String email, String rawToken, long ttlMinutes) {
        try {
            // * UriUtils.encode (not encodeQueryParam): the latter deliberately leaves '+', '/' and
            // * '?' untouched because they are legal inside a query parameter, so a token containing
            // * '+' would arrive as a space after form/query decoding. encode() percent-escapes
            // * everything outside the unreserved set (A-Z a-z 0-9 - . _ ~), which is exactly what a
            // * token needs. Our Base64-URL tokens only use unreserved characters, so they pass
            // * through unchanged and the link stays readable.
            String resetUrl = frontendUrl + "/reset-password?token="
                    + UriUtils.encode(rawToken, StandardCharsets.UTF_8);

            emailSender.sendHtml(
                    email,
                    name,
                    EmailTemplates.PASSWORD_RESET_SUBJECT,
                    EmailTemplates.passwordReset(name, email, resetUrl, ttlMinutes),
                    EmailTemplates.passwordResetPlainText(name, email, resetUrl, ttlMinutes));

            if (emailSender.isConfigured()) {
                log.info("Password reset email dispatched to {}", EmailSender.mask(email));
            } else {
                // * Local development without Brevo credentials: surface the link in the log so
                // * the flow can still be tested end to end. Nothing is emailed in this case.
                log.warn("Brevo is not configured, so the reset email was skipped. "
                        + "Open this link to continue locally: {}", resetUrl);
            }
        } catch (Exception ex) {
            log.warn("Could not send the password reset email to {}: {}", EmailSender.mask(email), ex.getMessage());
        }
    }

    /** Human-friendly role label used inside the welcome email. */
    static String roleLabel(Role role) {
        if (role == null) {
            return "Member";
        }
        return switch (role) {
            case ADMIN -> "Admin (full workspace access)";
            case MANAGER -> "Manager (sprints, tasks and team lead)";
            case MEMBER -> "Member (individual contributor)";
        };
    }
}
