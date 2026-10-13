
# Stage 1: Build Spring Boot application using Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

# Stage 2: Run application using Java 21 JRE (non-root user for security)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a dedicated non-root user and group for running the application
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/target/*.jar app.jar

# Ensure the non-root user owns the jar
RUN chown appuser:appgroup app.jar

# Switch to non-root user before starting the process
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
