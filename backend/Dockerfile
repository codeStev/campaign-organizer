# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Create the media dir owned by the runtime user so a fresh named volume mounted
# here inherits that ownership (the app runs as non-root and must write to it).
RUN useradd --system --uid 1001 spring \
    && mkdir -p /data/media \
    && chown -R spring:spring /data
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
