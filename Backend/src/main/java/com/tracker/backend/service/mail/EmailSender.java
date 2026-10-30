package com.tracker.backend.service.mail;

/**
 * * Small abstraction over "send one transactional email".
 * * Keeping this as an interface means the provider can be swapped (Brevo today,
 * * another provider tomorrow) and that EmailService can be unit tested with a fake.
 */
public interface EmailSender {

    /**
     * @param toEmail     recipient address
     * @param toName      recipient display name (may be blank)
     * @param subject     inbox subject line
     * @param htmlContent HTML body (already rendered)
     * @param textContent plain-text fallback for clients that do not render HTML
     */
    void sendHtml(String toEmail, String toName, String subject, String htmlContent, String textContent);

    /**
     * * True when the provider has everything it needs to deliver mail.
     * * EmailService uses this for a developer-friendly log line instead of silently
     * * pretending an email went out.
     */
    default boolean isConfigured() {
        return true;
    }

    /**
     * * Logs should never contain a full address: {@code al***@example.com} is enough to
     * * correlate a problem with an account without leaking the contact list.
     */
    static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "(unknown)";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + domain;
    }
}
