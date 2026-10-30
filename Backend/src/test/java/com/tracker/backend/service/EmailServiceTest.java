package com.tracker.backend.service;

import com.tracker.backend.entity.Role;
import com.tracker.backend.service.mail.EmailSender;
import com.tracker.backend.service.mail.EmailTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

// * Verifies the two email use cases: correct links for each environment,
// * URL-safe reset tokens, and that a Brevo outage never bubbles up to the caller.
class EmailServiceTest {

    private RecordingEmailSender sender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        sender = new RecordingEmailSender();
        // * Trailing slash on purpose: the constructor must normalise it.
        emailService = new EmailService(sender, "https://app.example.com/");
    }

    @Test
    void sendWelcomeEmail_BuildsDashboardLinkAndKeepsBranding() {
        emailService.sendWelcomeEmail("Alice", "alice@test.com", Role.MANAGER);

        assertEquals(1, sender.sent.size());
        SentEmail email = sender.sent.get(0);

        assertEquals("alice@test.com", email.toEmail());
        assertEquals("Alice", email.toName());
        assertEquals(EmailTemplates.WELCOME_SUBJECT, email.subject());
        assertTrue(email.html().contains("https://app.example.com/dashboard"));
        assertTrue(email.html().contains("Welcome aboard, Alice!"));
        assertTrue(email.html().contains("Manager"));
        assertTrue(email.plainText().contains("https://app.example.com/dashboard"));
    }

    @Test
    void sendWelcomeEmail_UnknownRole_FallsBackToMemberLabel() {
        emailService.sendWelcomeEmail("Bob", "bob@test.com", null);

        assertTrue(sender.sent.get(0).html().contains("Member"));
    }

    @Test
    void sendPasswordResetEmail_UsesFrontendUrlAndEncodesToken() {
        String token = "tok+en/=";

        emailService.sendPasswordResetEmail("Alice", "alice@test.com", token, 30L);

        assertEquals(1, sender.sent.size());
        SentEmail email = sender.sent.get(0);

        assertEquals(EmailTemplates.PASSWORD_RESET_SUBJECT, email.subject());
        assertTrue(email.html().contains("30 minutes"));

        // * '+' must never survive into a query string: it is decoded as a space, so the link
        // * would arrive with a token the backend cannot find. '/' and '=' must be escaped too.
        // * Hex digits are compared case-insensitively because the encoder may emit either form.
        String expectedParam = "?token=tok%2ben%2f%3d";
        assertTrue(email.html().toLowerCase(Locale.ROOT).contains(expectedParam),
                "the token must be percent-encoded inside the HTML link");
        assertTrue(email.plainText().toLowerCase(Locale.ROOT).contains(expectedParam),
                "the token must be percent-encoded inside the plain-text link");
        assertFalse(email.html().contains("?token=" + token), "the raw token must never appear in a link");
    }

    // * Real tokens are Base64-URL (A-Z a-z 0-9 - _), so percent-encoding them must be a
    // * no-op: the link has to stay readable and clickable in every mail client.
    @Test
    void sendPasswordResetEmail_RealisticUrlSafeTokenIsNotMangled() {
        String token = "Zm9vYmFyLXRva2VuXzEyMzQ1Njc4OTA";

        emailService.sendPasswordResetEmail("Alice", "alice@test.com", token, 30L);

        assertTrue(sender.sent.get(0).html().contains("?token=" + token));
        assertTrue(sender.sent.get(0).plainText().contains("?token=" + token));
    }

    @Test
    void sendWelcomeEmail_ProviderFailure_IsSwallowed() {
        sender.failWith(new IllegalStateException("Brevo is down"));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail("Alice", "alice@test.com", Role.MEMBER));
    }

    @Test
    void sendPasswordResetEmail_ProviderFailure_IsSwallowed() {
        sender.failWith(new IllegalStateException("Brevo is down"));

        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("Alice", "alice@test.com", "token", 30L));
    }

    // ------------------------------------------------------------------
    // Test doubles
    // ------------------------------------------------------------------

    private record SentEmail(String toEmail, String toName, String subject, String html, String plainText) {
    }

    private static final class RecordingEmailSender implements EmailSender {
        private final List<SentEmail> sent = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void sendHtml(String toEmail, String toName, String subject, String htmlContent, String textContent) {
            if (failure != null) {
                throw failure;
            }
            sent.add(new SentEmail(toEmail, toName, subject, htmlContent, textContent));
        }
    }
}
