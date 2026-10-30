package com.tracker.backend.service.mail;

import com.tracker.backend.config.BrevoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * * Sends transactional email through the Brevo v3 HTTPS API.
 *
 * * Request shape (only the fields we need):
 * * {
 * *   "sender":      { "email": "no-reply@yourdomain.com", "name": "Task & Sprint Tracker" },
 * *   "to":          [ { "email": "user@example.com", "name": "Vikranth" } ],
 * *   "subject":     "Welcome aboard!",
 * *   "htmlContent": "<html>...</html>",
 * *   "textContent": "Welcome aboard!"
 * * }
 */
@Service
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);

    private final BrevoProperties properties;
    private final RestClient restClient;

    /**
     * * The RestClient.Builder is injected (rather than built inline) so the HTTP layer can be
     * * swapped or mocked, and so Spring Boot's client customisations still apply.
     */
    public BrevoEmailSender(BrevoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public void sendHtml(String toEmail, String toName, String subject, String htmlContent, String textContent) {
        // * Missing credentials must never break sign-up or password reset.
        // * We log a warning so the developer sees the misconfiguration instead of silence.
        if (!properties.isConfigured()) {
            log.warn("Brevo is not configured (BREVO_API_KEY / BREVO_SENDER_EMAIL are missing or BREVO_ENABLED=false). "
                    + "Skipping '{}' email to {}.", subject, EmailSender.mask(toEmail));
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();

        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("email", properties.getSenderEmail());
        sender.put("name", properties.getSenderName());
        payload.put("sender", sender);

        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("email", toEmail);
        if (toName != null && !toName.isBlank()) {
            recipient.put("name", toName);
        }
        List<Map<String, Object>> to = new ArrayList<>();
        to.add(recipient);
        payload.put("to", to);

        payload.put("subject", subject);
        payload.put("htmlContent", htmlContent);
        if (textContent != null && !textContent.isBlank()) {
            payload.put("textContent", textContent);
        }

        restClient.post()
                .uri(properties.getApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("api-key", properties.getApiKey())
                .header("accept", "application/json")
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Brevo accepted '{}' email for {}", subject, EmailSender.mask(toEmail));
    }
}
