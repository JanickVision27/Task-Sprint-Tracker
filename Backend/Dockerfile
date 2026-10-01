# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies (this layer gets cached so future builds are fast)
RUN mvn dependency:go-offline
COPY src ./src
# Build the JAR file
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Render provides the PORT env variable, Spring Boot needs to know it
ENV PORT=8080
EXPOSE ${PORT}

# Start the Java app
ENTRYPOINT ["java", "-jar", "app.jar"]