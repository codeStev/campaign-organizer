# syntax=docker/dockerfile:1

# Combined image: Spring Boot serves the built frontend statically via its
# embedded Tomcat (ADR-0059), instead of running frontend/backend as two
# containers. Build context is the repo root.

# ---- Frontend build stage ----
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci || npm install
COPY frontend/ .
RUN npm run build

# ---- Backend build stage ----
FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /app
# Cache dependencies first.
COPY gradlew settings.gradle.kts ./
COPY gradle ./gradle
COPY backend/build.gradle.kts backend/build.gradle.kts
RUN ./gradlew --no-daemon :backend:dependencies
COPY backend/src backend/src
# The frontend build lands on the backend's static classpath root before
# packaging, so Spring Boot's default static-resource handling serves it
# with no extra config (see SpaWebConfig for the SPA deep-link fallback).
COPY --from=frontend-build /app/frontend/dist backend/src/main/resources/static
RUN ./gradlew --no-daemon clean :backend:bootJar -x :backend:test -x :backend:integrationTest

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
# Create the media dir owned by the runtime user so a fresh named volume mounted
# here inherits that ownership (the app runs as non-root and must write to it).
RUN useradd --system --uid 1001 spring \
    && mkdir -p /data/media \
    && chown -R spring:spring /data
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
