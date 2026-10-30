package com.tracker.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * * Proves the database really works at startup instead of leaving you to discover it later.
 *
 * * Logs the sanitized JDBC target (host + database, password always masked) and runs a
 * * `SELECT 1`. If the connection fails, the log states which settings Spring actually saw and
 * * what the most common causes are — Turn on `SPRING_JPA_SHOW_SQL=true` if you want queries too.
 *
 * * Deliberately does NOT stop the application: the API stays up so /api/health can report the
 * * problem (and Render keeps the container alive while you fix the env vars).
 */
@Component
public class DatabaseStartupVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupVerifier.class);

    private final DataSource dataSource;
    private final Environment environment;

    public DatabaseStartupVerifier(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String activeProfiles = String.join(",", environment.getActiveProfiles());

        log.info("Database target: {} (user: {}, profiles: {})",
                DatabaseUrlNormalizer.describe(url),
                username == null || username.isBlank() ? "not set" : username,
                activeProfiles.isBlank() ? "default" : activeProfiles);

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }

            log.info("Database connection OK — {} {} (read-write check passed)",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion());
        } catch (Exception ex) {
            log.error("""
                    ============================================================
                    DATABASE CONNECTION FAILED
                    ------------------------------------------------------------
                    Target : {}
                    Reason : {}
                    ------------------------------------------------------------
                    Most common causes:
                      * Neon: the host was copied without its "-pooler" suffix swapped in
                        correctly, or the project is suspended/asleep.
                      * The password contains @ # : / or ? and was not percent-encoded.
                      * sslmode=require is missing (Neon rejects non-SSL connections).
                      * A DATABASE_URL still points at a Node-style URL with a wrong host.
                      * The database/pod cannot be reached (Kubernetes NetworkPolicy, IP allow-list).
                    Verify the values in your host's dashboard (Render -> Environment,
                    HuggingFace/K8s Secret, or application-local.properties).
                    ============================================================
                    """,
                    DatabaseUrlNormalizer.describe(url),
                    ex.getMessage());
        }
    }
}
