# ADR-0001: Spring Boot 4 (Java 21) backend

- Status: Accepted
- Date: 2026-08-10

## Context
The application needs a backend for a REST API, persistence, validation, and
authentication. The stack was specified as Spring Boot. We must choose a major
version to avoid a near-term migration.

## Decision
Use **Spring Boot 4.0.x on Java 21**. Spring Boot 4 (GA 2025-11-20) is built on
Spring Framework 7 and Jakarta EE 11. Java 21 (an LTS) is used even though the
Boot 4 baseline is Java 17, for virtual threads and modern language features.

## Consequences
- Long support runway; no imminent framework migration (the stated goal).
- Jakarta EE 11 baseline: Hibernate 7, Bean Validation 3.1, Jackson 3 default.
- Dependencies must be Boot 4 compatible (e.g. springdoc-openapi 3.x, not 2.x).
- Tests avoid depending on a specific Jackson version because Jackson 3
  (`tools.jackson`) is now the default mapper.

## Alternatives considered
- **Spring Boot 3.3 (Java 17):** stable and widely documented, but the user
  explicitly wants to start on 4 to avoid a later migration.
