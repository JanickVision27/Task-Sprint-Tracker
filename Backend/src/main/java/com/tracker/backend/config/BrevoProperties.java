package com.tracker.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * * Holds the Brevo (formerly Sendinblue) transactional-email settings.
 *
 * * Brevo is called over its HTTPS API (https://api.brevo.com/v3/smtp/email) instead of SMTP,
 * * because free hosting tiers (Render, Railway, Fly.io) usually block outbound SMTP ports.
 *
 * * Every value can be supplied through an environment variable, so no secret is ever committed:
 * *   BREVO_API_KEY      -> app.brevo.api-key
 * *   BREVO_SENDER_EMAIL -> app.brevo.sender-email
 * *   BREVO_SENDER_NAME  -> app.brevo.sender-name
 */
@Component
@ConfigurationProperties(prefix = "app.brevo")
public class BrevoProperties {

    /** Brevo API v3 key (starts with "xkeysib-"). Never commit it. */
    private String apiKey;

    /** Full Brevo transactional email endpoint. */
    private String apiUrl = "https://api.brevo.com/v3/smtp/email";

    /** Sender address; must be a verified sender/domain inside Brevo. */
    private String senderEmail;

    /** Friendly sender name shown in the recipient's inbox. */
    private String senderName = "Task & Sprint Tracker";

    /** Master switch: set BREVO_ENABLED=false to develop offline without sending mail. */
    private boolean enabled = true;

    /**
     * * True only when every required value is present.
     * * When false, EmailService logs a warning instead of throwing, so local development,
     * * unit tests, and CI keep working without real Brevo credentials.
     */
    public boolean isConfigured() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && senderEmail != null && !senderEmail.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
