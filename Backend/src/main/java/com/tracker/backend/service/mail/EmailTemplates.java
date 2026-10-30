package com.tracker.backend.service.mail;

/**
 * * Renders the two transactional emails of the app.
 *
 * * The markup deliberately mirrors the product UI (indigo #4f46e5 brand, slate text,
 * * rounded white card) so an email from us is instantly recognisable.
 *
 * * Everything is inline-styled with tables because Gmail, Outlook and Apple Mail strip
 * * <style> blocks and do not support flexbox/grid.
 *
 * * Templates are plain Java strings with {{TOKEN}} placeholders. We avoid
 * * String.format() here on purpose: the CSS contains '%' characters which would be
 * * interpreted as format specifiers.
 */
public final class EmailTemplates {

    public static final String WELCOME_SUBJECT = "Welcome to Task & Sprint Tracker 🎉";
    public static final String PASSWORD_RESET_SUBJECT = "Reset your Task & Sprint Tracker password";

    private static final String FONT_STACK = "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif";

    private EmailTemplates() {
        // * Utility class.
    }

    // ---------------------------------------------------------------------
    // Welcome / registration confirmation
    // ---------------------------------------------------------------------

    public static String welcome(String name, String email, String roleLabel, String dashboardUrl) {
        String safeName = escapeHtml(name);
        String safeEmail = escapeHtml(email);
        String safeRole = escapeHtml(roleLabel);

        String body = """
                <h1 style="margin:0 0 8px;font:700 24px/1.3 {{FONT}};color:#0f172a;">Welcome aboard, {{NAME}}! 🎉</h1>
                <p style="margin:0 0 20px;font:400 15px/1.6 {{FONT}};color:#475569;">
                  Your account is ready. You signed up as <strong style="color:#0f172a;">{{ROLE}}</strong>
                  with <strong style="color:#0f172a;">{{EMAIL}}</strong>.
                </p>

                <p style="margin:0 0 14px;font:600 14px/1.5 {{FONT}};color:#0f172a;">Here is what you can do right away</p>
                {{FEATURES}}

                {{CTA_BLOCK}}

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:28px 0 0;">
                  <tr>
                    <td style="background-color:#eef2ff;border-left:3px solid #4f46e5;border-radius:8px;padding:14px 16px;">
                      <p style="margin:0;font:400 13px/1.6 {{FONT}};color:#3730a3;">
                        <strong>Team tip:</strong> create a Project, add a Sprint inside it, then break the work into
                        Tasks. Everyone on the board sees changes live — no refresh needed.
                      </p>
                    </td>
                  </tr>
                </table>

                <p style="margin:24px 0 0;font:400 15px/1.6 {{FONT}};color:#475569;">
                  Happy shipping,<br>
                  <strong style="color:#0f172a;">The Task &amp; Sprint Tracker team</strong>
                </p>
                """

                .replace("{{FONT}}", FONT_STACK)
                .replace("{{NAME}}", safeName)
                .replace("{{EMAIL}}", safeEmail)
                .replace("{{ROLE}}", safeRole)
                .replace("{{FEATURES}}", featureList())
                .replace("{{CTA_BLOCK}}", ctaBlock("Open my dashboard", dashboardUrl));

        return layout(
                "Your Task & Sprint Tracker account is ready — here is how to get started.",
                body,
                "You are receiving this email because an account was created with " + safeEmail
                        + ". If that was not you, no action is needed and you can ignore this message.");
    }

    public static String welcomePlainText(String name, String email, String roleLabel, String dashboardUrl) {
        return """
                Welcome aboard, %s!

                Your Task & Sprint Tracker account is ready.
                Email: %s
                Role:  %s

                What you can do right away:
                  - Organise work as Projects -> Sprints -> Tasks
                  - Drag tasks across Todo / In Progress / Done on a live Kanban board
                  - Comment on tasks to keep the whole team in the loop
                  - Access is role-based: Admin, Manager or Member

                Open your dashboard: %s

                Team tip: create a Project, add a Sprint inside it, then break the work into Tasks.

                Happy shipping,
                The Task & Sprint Tracker team

                You are receiving this email because an account was created with %s.
                If that was not you, you can ignore this message.
                """.formatted(name, email, roleLabel, dashboardUrl, email);
    }

    // ---------------------------------------------------------------------
    // Password reset
    // ---------------------------------------------------------------------

    public static String passwordReset(String name, String email, String resetUrl, long ttlMinutes) {
        String safeName = escapeHtml(name);
        String safeEmail = escapeHtml(email);
        String safeUrl = escapeHtml(resetUrl);
        String ttl = String.valueOf(ttlMinutes);

        String body = """
                <h1 style="margin:0 0 8px;font:700 24px/1.3 {{FONT}};color:#0f172a;">Reset your password</h1>
                <p style="margin:0 0 20px;font:400 15px/1.6 {{FONT}};color:#475569;">
                  Hi {{NAME}}, we received a request to reset the password for
                  <strong style="color:#0f172a;">{{EMAIL}}</strong>.
                  Click the button below to choose a new one.
                </p>

                {{CTA_BLOCK}}

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:26px 0 0;">
                  <tr>
                    <td style="background-color:#fff7ed;border-left:3px solid #f59e0b;border-radius:8px;padding:14px 16px;">
                      <p style="margin:0;font:400 13px/1.6 {{FONT}};color:#9a3412;">
                        <strong>This link expires in {{TTL}} minutes</strong> and can only be used once.
                        After that you will need to request a new one.
                      </p>
                    </td>
                  </tr>
                </table>

                <p style="margin:24px 0 0;font:400 15px/1.6 {{FONT}};color:#475569;">
                  Didn&apos;t request a password reset? You can safely ignore this email —
                  your current password will stay exactly the same.
                </p>

                <p style="margin:16px 0 0;font:400 13px/1.6 {{FONT}};color:#94a3b8;">
                  If you keep receiving these emails, ask a workspace admin to review your account.
                </p>
                """

                .replace("{{FONT}}", FONT_STACK)
                .replace("{{NAME}}", safeName)
                .replace("{{EMAIL}}", safeEmail)
                .replace("{{TTL}}", ttl)
                .replace("{{CTA_BLOCK}}", ctaBlock("Reset my password", safeUrl));

        return layout(
                "Use this secure link to set a new password for your Task & Sprint Tracker account.",
                body,
                "This password reset was requested for " + safeEmail
                        + ". If it was not you, you can ignore this email.");
    }

    public static String passwordResetPlainText(String name, String email, String resetUrl, long ttlMinutes) {
        return """
                Hi %s,

                We received a request to reset the password for your Task & Sprint Tracker account (%s).

                Reset your password here:
                %s

                This link expires in %d minutes and can only be used once.

                Didn't request a password reset? You can safely ignore this email -
                your current password will stay exactly the same.

                The Task & Sprint Tracker team
                """.formatted(name, email, resetUrl, ttlMinutes);
    }

    // ---------------------------------------------------------------------
    // Building blocks
    // ---------------------------------------------------------------------

    private static String featureList() {
        String[] features = {
                "Plan work as <strong style=\"color:#0f172a;\">Projects → Sprints → Tasks</strong>",
                "Drag tasks across <strong style=\"color:#0f172a;\">Todo, In Progress and Done</strong> on a live Kanban board",
                "Comment on tasks so the whole team stays in the loop",
                "Everything updates in real time for teammates viewing the same board"
        };

        StringBuilder rows = new StringBuilder();
        for (String feature : features) {
            rows.append("""
                      <tr>
                        <td valign="top" style="width:20px;padding:0 10px 10px 0;font:700 14px/1.6 {{FONT}};color:#4f46e5;">&#10003;</td>
                        <td valign="top" style="padding:0 0 10px;font:400 14px/1.6 {{FONT}};color:#475569;">{{FEATURE}}</td>
                      </tr>
                    """.replace("{{FONT}}", FONT_STACK).replace("{{FEATURE}}", feature));
        }

        return """
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 4px;">
                {{ROWS}}
                </table>
                """.replace("{{ROWS}}", rows.toString());
    }

    private static String ctaBlock(String label, String url) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:26px 0 0;">
                  <tr>
                    <td align="center" style="border-radius:10px;background-color:#4f46e5;">
                      <a href="{{URL}}" target="_blank" rel="noopener"
                         style="display:inline-block;padding:14px 30px;font:600 15px/1 {{FONT}};color:#ffffff;text-decoration:none;border-radius:10px;">
                        {{LABEL}}
                      </a>
                    </td>
                  </tr>
                </table>
                <p style="margin:18px 0 0;font:400 12px/1.6 {{FONT}};color:#94a3b8;">
                  Button not working? Paste this link into your browser:
                </p>
                <p style="margin:6px 0 0;font:400 12px/1.6 {{FONT}};color:#4f46e5;word-break:break-all;">
                  <a href="{{URL}}" target="_blank" rel="noopener" style="color:#4f46e5;text-decoration:underline;">{{URL}}</a>
                </p>
                """
                .replace("{{FONT}}", FONT_STACK)
                .replace("{{LABEL}}", label)
                .replace("{{URL}}", url);
    }

    /**
     * * Wraps the content in the branded shell: indigo header with the "ST" mark,
     * * white content card, muted footer with the "why you got this" line.
     */
    private static String layout(String preheader, String body, String footerReason) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light">
                  <title>Task &amp; Sprint Tracker</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;">
                  <div style="display:none;max-height:0;max-width:0;overflow:hidden;opacity:0;color:#f1f5f9;font-size:1px;line-height:1px;">
                    {{PREHEADER}}
                  </div>

                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f1f5f9;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
                               style="max-width:600px;background-color:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;">

                          <tr>
                            <td style="background-color:#4f46e5;padding:22px 32px;">
                              <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td align="center" style="width:40px;height:40px;background-color:#4338ca;border-radius:10px;font:700 15px/40px {{FONT}};color:#ffffff;">
                                    ST
                                  </td>
                                  <td style="padding-left:12px;font:600 16px/1.4 {{FONT}};color:#ffffff;">
                                    Task &amp; Sprint Tracker
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:32px;">
                              {{BODY}}
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:20px 32px 26px;background-color:#f8fafc;border-top:1px solid #e2e8f0;">
                              <p style="margin:0 0 6px;font:400 12px/1.6 {{FONT}};color:#94a3b8;">
                                {{FOOTER}}
                              </p>
                              <p style="margin:0;font:400 12px/1.6 {{FONT}};color:#cbd5e1;">
                                Task &amp; Sprint Tracker · Projects → Sprints → Tasks, together in real time.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .replace("{{FONT}}", FONT_STACK)
                .replace("{{PREHEADER}}", escapeHtml(preheader))
                .replace("{{BODY}}", body)
                .replace("{{FOOTER}}", footerReason);
    }

    /**
     * * User-controlled values (name, email) are escaped before they land in HTML,
     * * otherwise a display name like {@code <img src=x onerror=...>} would be injected
     * * straight into the recipient's inbox.
     */
    static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
