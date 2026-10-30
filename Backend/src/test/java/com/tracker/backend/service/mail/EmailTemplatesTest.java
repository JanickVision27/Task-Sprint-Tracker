package com.tracker.backend.service.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// * Guards the two hand-written HTML templates: a display name must never be able to
// * inject markup into someone's inbox, and every email needs a working link.
class EmailTemplatesTest {

    @Test
    void welcome_EscapesUserControlledName() {
        String html = EmailTemplates.welcome(
                "<script>alert('xss')</script>", "alice@test.com", "Member", "https://app.example.com/dashboard");

        assertFalse(html.contains("<script>alert"), "the name must be HTML-escaped");
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void welcome_ContainsDashboardLinkAndExpirylessCopy() {
        String html = EmailTemplates.welcome("Alice", "alice@test.com", "Manager", "https://app.example.com/dashboard");

        assertTrue(html.contains("https://app.example.com/dashboard"));
        assertTrue(html.contains("Welcome aboard, Alice!"));
        assertTrue(html.contains("Task &amp; Sprint Tracker"), "brand name must be escaped consistently");
    }

    @Test
    void welcomePlainText_IsHumanReadable() {
        String text = EmailTemplates.welcomePlainText("Alice", "alice@test.com", "Manager", "https://app.example.com/dashboard");

        assertTrue(text.contains("Welcome aboard, Alice!"));
        assertTrue(text.contains("https://app.example.com/dashboard"));
        assertFalse(text.contains("<"), "plain-text part must not contain markup");
    }

    @Test
    void passwordReset_ShowsExpiryAndEscapesLink() {
        String html = EmailTemplates.passwordReset(
                "Alice", "alice@test.com", "https://app.example.com/reset-password?token=abc123", 30);

        assertTrue(html.contains("https://app.example.com/reset-password?token=abc123"));
        assertTrue(html.contains("30 minutes"));
        assertTrue(html.contains("only be used once"));
    }

    @Test
    void passwordResetPlainText_ShowsExpiry() {
        String text = EmailTemplates.passwordResetPlainText(
                "Alice", "alice@test.com", "https://app.example.com/reset-password?token=abc123", 30);

        assertTrue(text.contains("expires in 30 minutes"));
        assertTrue(text.contains("https://app.example.com/reset-password?token=abc123"));
    }

    @Test
    void escapeHtml_HandlesNullsAndAmpersands() {
        assertEquals("", EmailTemplates.escapeHtml(null));
        assertEquals("Tom &amp; Jerry", EmailTemplates.escapeHtml("Tom & Jerry"));
        assertEquals("&quot;quoted&quot;", EmailTemplates.escapeHtml("\"quoted\""));
    }
}
