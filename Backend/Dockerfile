# syntax=docker/dockerfile:1

# Stage 1: build the executable Spring Boot JAR with Maven and a full JDK.
# This stage is discarded, so Maven and compiler tools never enter the runtime image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the dependency manifest first. Docker can reuse the downloaded-dependency
# layer when application source changes but pom.xml stays the same.
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

# Compile/package after dependencies are cached. CI remains responsible for running tests.
COPY src/ ./src/
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

# Stage 2: use only a JRE on Alpine to reduce the production image's size and attack surface.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Use a fixed, unprivileged UID/GID so a compromised process has no root privileges.
RUN addgroup -S -g 10001 appuser \
    && adduser -S -D -H -u 10001 -G appuser -s /sbin/nologin appuser

# Copy only the packaged application; the Maven cache, source, and build tools stay behind.
COPY --from=build --chown=appuser:appuser /build/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Spring Boot reads PORT from the environment and defaults to 8080.
ENV PORT=8080
EXPOSE 8080

# Drop root privileges before starting the application.
USER appuser

# Report unhealthy containers if the Spring Boot health endpoint is not responding.
# The endpoint is explicitly exposed and permitted without JWT in application config/security.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1

# Let the JVM size itself from container memory limits and exit promptly on OOM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
