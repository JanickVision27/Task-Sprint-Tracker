package com.tracker.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * * Runs before Spring creates the DataSource and fixes two things that otherwise produce
 * * cryptic startup failures:
 *
 * * 1. Node-style connection strings. `DATABASE_URL` is the first choice in
 * *    application.properties, and every managed Postgres (Neon, Render, Heroku, Supabase)
 * *    emits `postgresql://user:pass@host/db?sslmode=require`. JDBC cannot parse that, so it is
 * *    rewritten into `jdbc:postgresql://host/db?sslmode=require` with the credentials split out.
 *
 * * 2. Missing configuration. Instead of the classic
 * *    "Failed to configure a DataSource: 'url' attribute is not specified" wall of text,
 * *    startup stops with an actionable message that names the env vars to set.
 *
 * * Registered in META-INF/spring.factories; runs after application.properties is loaded
 * * (order = LOWEST_PRECEDENCE) so explicit `spring.datasource.*` values still win.
 */
public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";

    static final String UNCONFIGURED_MESSAGE = """
            No database is configured, so the application cannot start.

            FASTEST LOCAL FIX (run these from the repository root):
                docker compose up -d
                cp Backend/application-local.properties.example Backend/application-local.properties
            then start the backend:
                cd Backend && ./mvnw spring-boot:run

            Any ONE of these is enough:
              1. DATABASE_URL              e.g. postgresql://user:password@ep-xxx.neon.tech/neondb?sslmode=require
                                           (a Node-style URL is converted to JDBC automatically; a
                                           jdbc:postgresql://... URL also works)
                 optionally with DATABASE_USERNAME / DATABASE_PASSWORD
              2. SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
                                           e.g. jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require
              3. A local properties file (git-ignored, loaded by the default 'local' profile):
                     Backend/application-local.properties      <- preferred, no rebuild needed
                     Backend/src/main/resources/application-local.properties
                     Backend/config/application-local.properties
                 with:
                     spring.datasource.url=jdbc:postgresql://localhost:5432/sprint_tracker
                     spring.datasource.username=...
                     spring.datasource.password=...

            Templates: Backend/application-local.properties.example and Backend/.env.example
            After starting, open http://localhost:8080/api/health to confirm the database is UP.""";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String envUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("SPRING_DATASOURCE_URL"));
        String configuredUrl = environment.getProperty("spring.datasource.url");

        // * Precedence: an explicitly written spring.datasource.url (for example from
        // * application-local.properties or a system property) beats the env vars, because
        // * application.properties only holds a placeholder that resolves *after* this point.
        String candidate = isResolved(configuredUrl)
                ? configuredUrl
                : (envUrl != null && !envUrl.isBlank() ? envUrl : null);

        if (candidate == null) {
            throw new IllegalStateException(UNCONFIGURED_MESSAGE);
        }

        if (!DatabaseUrlNormalizer.isNodeStyleUrl(candidate)) {
            // * Already a JDBC URL (or something we must not silently rewrite).
            if (!DatabaseUrlNormalizer.isSupportedUrl(candidate)) {
                throw new IllegalArgumentException(
                        "spring.datasource.url / DATABASE_URL must be a PostgreSQL connection string "
                                + "starting with 'jdbc:' or 'postgresql://' but was '"
                                + DatabaseUrlNormalizer.mask(candidate) + "'.");
            }
            return;
        }

        String envUsername = firstNonBlank(
                environment.getProperty("DATABASE_USERNAME"),
                environment.getProperty("SPRING_DATASOURCE_USERNAME"));
        String envPassword = firstNonBlank(
                environment.getProperty("DATABASE_PASSWORD"),
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"));

        String configuredUsername = isResolved(environment.getProperty("spring.datasource.username"))
                ? environment.getProperty("spring.datasource.username")
                : null;
        String configuredPassword = isResolved(environment.getProperty("spring.datasource.password"))
                ? environment.getProperty("spring.datasource.password")
                : null;

        DatabaseUrlNormalizer.NormalizedUrl normalized = DatabaseUrlNormalizer.normalize(
                candidate,
                firstNonBlank(envUsername, configuredUsername),
                firstNonBlank(envPassword, configuredPassword));

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.datasource.url", normalized.jdbcUrl());
        if (isResolved(normalized.username())) {
            overrides.put("spring.datasource.username", normalized.username());
        }
        if (normalized.password() != null) {
            overrides.put("spring.datasource.password", normalized.password());
        }

        // * addFirst so these values beat the ${...} placeholders in application.properties.
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }

    @Override
    public int getOrder() {
        // * After ConfigData (application.properties / application-local.properties) has loaded.
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * * A property is usable when it exists and is not an unresolved placeholder such as
     * * "${DATABASE_URL:${SPRING_DATASOURCE_URL}}" — which is what `spring.datasource.url`
     * * resolves to when neither env var is set.
     */
    private static boolean isResolved(String value) {
        return value != null && !value.isBlank() && !value.contains("${");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
