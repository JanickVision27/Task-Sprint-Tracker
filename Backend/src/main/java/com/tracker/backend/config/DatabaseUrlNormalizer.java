package com.tracker.backend.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * * Normalises the many shapes of "database connection string" into what the
 * * PostgreSQL JDBC driver actually accepts.
 *
 * * Why this exists: Neon, Render, Heroku, Railway and Supabase all hand you a URL like
 * *   postgresql://app_user:s3cret@ep-cool-host.eu-central-1.aws.neon.tech/neondb?sslmode=require
 * * Java's JDBC driver cannot read that shape — it needs
 * *   jdbc:postgresql://ep-cool-host.eu-central-1.aws.neon.tech/neondb?sslmode=require
 * * plus the username and password delivered separately. Feeding the Node-style URL straight to
 * * Hikari results in the classic "Unable to determine Dialect without JDBC metadata" crash.
 *
 * * This class only *describes* the conversion; {@link DatabaseEnvironmentPostProcessor} applies it.
 */
public final class DatabaseUrlNormalizer {

    /** Only PostgreSQL is supported by this application (see pom.xml). */
    private static final List<String> NODE_STYLE_SCHEMES = List.of("postgresql://", "postgres://");

    private DatabaseUrlNormalizer() {
        // * Utility class.
    }

    /**
     * * Result of a conversion. {@code username}/{@code password} are null when the URL did not
     * * carry user info, so the caller can fall back to separately configured credentials.
     */
    public record NormalizedUrl(String jdbcUrl, String username, String password) {
    }

    /**
     * * True when the value is a Node/Heroku-style URL rather than a JDBC URL.
     * * A blank value or an already-JDBC value returns false (nothing to do).
     */
    public static boolean isNodeStyleUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        if (trimmed.regionMatches(true, 0, "jdbc:", 0, 5)) {
            return false;
        }
        return NODE_STYLE_SCHEMES.stream().anyMatch(scheme -> trimmed.regionMatches(true, 0, scheme, 0, scheme.length()));
    }

    /**
     * * True when the value looks like a connection string we understand at all:
     * * either a JDBC URL or a postgres:// / postgresql:// URL.
     */
    public static boolean isSupportedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.regionMatches(true, 0, "jdbc:", 0, 5) || isNodeStyleUrl(trimmed);
    }

    /**
     * * Converts a connection string into a JDBC URL plus credentials.
     *
     * @param rawUrl           a JDBC URL (returned unchanged) or a postgres/postgresql URL
     * @param fallback         username used when the URL carries no user info (may be null)
     * @param fallbackPassword password used when the URL carries no user info (may be null)
     * @throws IllegalArgumentException when the scheme is not supported (for example `mysql://`)
     */
    public static NormalizedUrl normalize(String rawUrl, String fallback, String fallbackPassword) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("The database URL is empty.");
        }
        String url = rawUrl.trim();

        if (url.regionMatches(true, 0, "jdbc:", 0, 5)) {
            return new NormalizedUrl(url, fallback, fallbackPassword);
        }

        String matchedScheme = NODE_STYLE_SCHEMES.stream()
                .filter(scheme -> url.regionMatches(true, 0, scheme, 0, scheme.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported database URL scheme in '" + mask(url) + "'. "
                                + "This application only supports PostgreSQL: use jdbc:postgresql://... "
                                + "or postgresql://..."));

        String remainder = url.substring(matchedScheme.length());

        // * Credentials live before the LAST '@' so an un-encoded '@' inside a password still works.
        String userInfo = null;
        int atIndex = remainder.lastIndexOf('@');
        if (atIndex >= 0) {
            userInfo = remainder.substring(0, atIndex);
            remainder = remainder.substring(atIndex + 1);
        }

        // * Everything after '?' is a driver option such as sslmode=require — keep it verbatim.
        String query = null;
        int queryIndex = remainder.indexOf('?');
        if (queryIndex >= 0) {
            query = remainder.substring(queryIndex + 1);
            remainder = remainder.substring(0, queryIndex);
        }

        String jdbcUrl = "jdbc:postgresql://" + remainder
                + (query != null && !query.isBlank() ? "?" + query : "");

        String username = fallback;
        String password = fallbackPassword;
        if (userInfo != null && !userInfo.isBlank()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                String rawPassword = userInfo.substring(colon + 1);
                username = decode(userInfo.substring(0, colon));
                // * An empty password in the URL means "no password", not "use the fallback".
                password = rawPassword.isEmpty() ? fallbackPassword : decode(rawPassword);
            } else {
                username = decode(userInfo);
            }
        }

        return new NormalizedUrl(jdbcUrl, username, password);
    }

    /**
     * * Removes the password (and any pgjdbc `password=` option) so a URL can be logged safely.
     */
    public static String mask(String url) {
        if (url == null || url.isBlank()) {
            return "(not set)";
        }
        return url.trim()
                .replaceAll("(?i)(password|pwd)=([^&]*)", "$1=***")
                .replaceAll("//([^:/@]+):[^@/]*@", "//$1:***@");
    }

    /**
     * * Turns a URL into a short human-readable target for startup logs, e.g.
     * * {@code jdbc:postgresql://ep-host/neondb?sslmode=require}.
     */
    public static String describe(String url) {
        return mask(url);
    }

    private static String decode(String value) {
        // * Connection strings percent-encode special characters in the user info section.
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
