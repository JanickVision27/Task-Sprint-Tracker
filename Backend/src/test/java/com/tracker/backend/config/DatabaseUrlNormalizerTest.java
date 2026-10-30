package com.tracker.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// * The connection string is the single most common reason this app fails to boot, so the
// * conversion rules are pinned down here.
class DatabaseUrlNormalizerTest {

    @Test
    void jdbcUrlIsReturnedUnchanged() {
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require", "user", "pw");

        assertEquals("jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require", result.jdbcUrl());
        assertEquals("user", result.username());
        assertEquals("pw", result.password());
        assertFalse(DatabaseUrlNormalizer.isNodeStyleUrl(result.jdbcUrl()));
    }

    @Test
    void neonStyleUrlIsConvertedToJdbc() {
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "postgresql://app_user:s3cret@ep-cool-host.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require",
                null,
                null);

        assertEquals(
                "jdbc:postgresql://ep-cool-host.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require",
                result.jdbcUrl());
        assertEquals("app_user", result.username());
        assertEquals("s3cret", result.password());
    }

    @Test
    void herokuStyleSchemeIsConvertedToo() {
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "postgres://user:pw@localhost:5432/tasks", null, null);

        assertEquals("jdbc:postgresql://localhost:5432/tasks", result.jdbcUrl());
        assertEquals("user", result.username());
        assertEquals("pw", result.password());
    }

    @Test
    void percentEncodedCredentialsAreDecoded() {
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "postgresql://app_user:p%40ss%3Aword@host/neondb?sslmode=require", null, null);

        assertEquals("app_user", result.username());
        assertEquals("p@ss:word", result.password());
    }

    @Test
    void rawAtSignInsidePasswordStillResolvesTheRealHost() {
        // * Users often paste an un-encoded password; splitting on the LAST '@' keeps the host intact.
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "postgresql://user:pa@ss@ep-host.neon.tech/neondb", null, null);

        assertEquals("jdbc:postgresql://ep-host.neon.tech/neondb", result.jdbcUrl());
        assertEquals("user", result.username());
        assertEquals("pa@ss", result.password());
    }

    @Test
    void missingUserInfoFallsBackToSeparatelyConfiguredCredentials() {
        DatabaseUrlNormalizer.NormalizedUrl result = DatabaseUrlNormalizer.normalize(
                "postgresql://ep-host.neon.tech/neondb?sslmode=require", "env_user", "env_pw");

        assertEquals("jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require", result.jdbcUrl());
        assertEquals("env_user", result.username());
        assertEquals("env_pw", result.password());
    }

    @Test
    void unsupportedSchemeIsRejectedWithAHelpfulMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DatabaseUrlNormalizer.normalize("mysql://user:pw@host/db", null, null));

        assertTrue(ex.getMessage().contains("PostgreSQL"));
        assertFalse(ex.getMessage().contains("pw"), "the password must never appear in an error message");
    }

    @Test
    void maskHidesPasswordsInBothUrlShapes() {
        assertEquals(
                "jdbc:postgresql://host/db?user=app&password=***",
                DatabaseUrlNormalizer.mask("jdbc:postgresql://host/db?user=app&password=hunter2"));

        assertEquals(
                "postgresql://app_user:***@host/db",
                DatabaseUrlNormalizer.mask("postgresql://app_user:hunter2@host/db"));
    }

    @Test
    void supportedUrlRecognition() {
        assertTrue(DatabaseUrlNormalizer.isSupportedUrl("jdbc:postgresql://host/db"));
        assertTrue(DatabaseUrlNormalizer.isSupportedUrl("postgresql://user:pw@host/db"));
        assertFalse(DatabaseUrlNormalizer.isSupportedUrl("mysql://user:pw@host/db"));
        assertFalse(DatabaseUrlNormalizer.isSupportedUrl("   "));
        assertFalse(DatabaseUrlNormalizer.isSupportedUrl(null));
    }
}
