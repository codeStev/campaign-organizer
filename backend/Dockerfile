# syntax=docker/dockerfile:1

# ---- Build stage ----
# Build context is the repo root (see docker-compose.yml) so the root Gradle
# wrapper/multi-module build can see the :backend subproject.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Cache dependencies first.
COPY gradlew settings.gradle.kts ./
COPY gradle ./gradle
COPY backend/build.gradle.kts backend/build.gradle.kts
RUN ./gradlew --no-daemon :backend:dependencies
COPY backend/src backend/src
RUN ./gradlew --no-daemon clean :backend:bootJar -x :backend:test -x :backend:integrationTest

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
# postgresql-client backs the one-click instance backup (ADR-0055): pg_dump
# only needs to be the same version or newer than the server it's dumping
# (docker-compose.yml's postgres:16-alpine), which the distro's default
# package already satisfies — no need to pin/add a third-party apt repo.
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*
# Create the media dir owned by the runtime user so a fresh named volume mounted
# here inherits that ownership (the app runs as non-root and must write to it).
RUN useradd --system --uid 1001 spring \
    && mkdir -p /data/media \
    && chown -R spring:spring /data
COPY --from=build /app/backend/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
