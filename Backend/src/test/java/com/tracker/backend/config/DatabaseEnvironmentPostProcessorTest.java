package com.tracker.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// * Pins the startup behaviour: env vars become a usable JDBC configuration, explicit
// * properties win, and a missing database produces an actionable error instead of a stack trace dump.
class DatabaseEnvironmentPostProcessorTest {

    private final DatabaseEnvironmentPostProcessor processor = new DatabaseEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void nodeStyleDatabaseUrlBecomesJdbcWithSplitCredentials() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgresql://app_user:s3cret@ep-host.neon.tech/neondb?sslmode=require")));

        processor.postProcessEnvironment(environment, application);

        assertEquals("jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require",
                environment.getProperty("spring.datasource.url"));
        assertEquals("app_user", environment.getProperty("spring.datasource.username"));
        assertEquals("s3cret", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void jdbcUrlIsLeftAlone() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://host:5432/db",
                "SPRING_DATASOURCE_USERNAME", "u",
                "SPRING_DATASOURCE_PASSWORD", "p")));

        processor.postProcessEnvironment(environment, application);

        // * No override source was added, so nothing is shadowing the user's own configuration.
        assertNull(environment.getPropertySources().get("normalizedDatabaseUrl"));
    }

    @Test
    void missingDatabaseFailsFastWithInstructions() {
        StandardEnvironment environment = new StandardEnvironment();
        // * Spring leaves the placeholder unresolved when neither env var exists.
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "${DATABASE_URL:${SPRING_DATASOURCE_URL}}")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, application));

        assertTrue(ex.getMessage().contains("DATABASE_URL"));
        assertTrue(ex.getMessage().contains("application-local.properties"));
    }

    @Test
    void explicitSeparateCredentialsAreKeptWhenTheUrlHasNoUserInfo() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgresql://ep-host.neon.tech/neondb?sslmode=require",
                "DATABASE_USERNAME", "env_user",
                "DATABASE_PASSWORD", "env_pw")));

        processor.postProcessEnvironment(environment, application);

        assertEquals("jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require",
                environment.getProperty("spring.datasource.url"));
        assertEquals("env_user", environment.getProperty("spring.datasource.username"));
        assertEquals("env_pw", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void explicitJDBCPropertyWinsOverANodeStyleEnvVar() {
        // * application-local.properties must not be silently overridden by a leftover env var.
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "jdbc:postgresql://localhost:5432/localdb",
                "DATABASE_URL", "postgresql://cloud_user:s3cret@ep-host.neon.tech/neondb?sslmode=require")));

        processor.postProcessEnvironment(environment, application);

        assertEquals("jdbc:postgresql://localhost:5432/localdb", environment.getProperty("spring.datasource.url"));
        assertNull(environment.getPropertySources().get("normalizedDatabaseUrl"));
    }

    @Test
    void unsupportedSchemeFailsWithClearMessage() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "mysql://user:pw@host/db")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> processor.postProcessEnvironment(environment, application));

        assertTrue(ex.getMessage().contains("PostgreSQL"));
        assertFalse(ex.getMessage().contains("pw"));
    }
}
