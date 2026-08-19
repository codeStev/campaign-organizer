# ADR-0051: Upgrade to Java 25, Spring Boot 4.1, JUnit 6

- Status: Accepted
- Date: 2026-08-19
- Supersedes: the Java/Spring Boot version line of ADR-0001 (Spring Boot 4
  (Java 21) backend). ADR-0001's other decisions (Boot 4, Jakarta EE 11
  baseline) stand.

## Context
ADR-0001 pinned Spring Boot 4.0.x on Java 21 to get a long support runway
without an imminent framework migration. Spring Boot 4.1.0 and Java 25 (both
LTS-track) are now released. Spring Boot 4.1.0's dependency management already
moves `junit-jupiter.version` to `6.0.3`, so adopting it brings JUnit 6 as a
side effect rather than a separate manual bump.

## Decision
Upgrade the backend to:
- **Java 25** (`java.version` / `maven.compiler.release`), from 21.
- **Spring Boot 4.1.0** (`spring-boot-starter-parent`), from 4.0.1.
- **JUnit 6** (`junit-jupiter` 6.0.3), inherited from the Spring Boot 4.1.0 BOM —
  no explicit version property needed.
- **ArchUnit 1.5.0**, from 1.3.0 — ArchUnit parses bytecode directly (via ASM),
  so its version needs to keep pace with the JDK the build targets.
- **jacoco-maven-plugin 0.8.15**, from 0.8.12 — 0.8.12 fails to instrument Java
  25 class files (`Unsupported class file major version 69`); confirmed by
  building against JDK 25 before/after the bump.
- MapStruct stays at 1.6.3 (already latest; no Java-25-specific release needed).

Build/runtime images move in lockstep: `maven:3.9-eclipse-temurin-25` for the
Docker build stage, `eclipse-temurin:25-jre` for the runtime image, and
`java-version: "25"` in CI's `actions/setup-java`.

## Consequences
- Any JUnit 5 → 6 breaking API usage needs fixing (JUnit 6 raises its own
  baseline; check migration notes before merging).
- CI runner and local dev need JDK 25 available; `mvn` invocations that pin a
  JDK image (this project's Docker-wrapped Maven calls) update alongside.
- Re-validate the full Testcontainers integration suite before merging, not
  just unit tests + ArchUnit — a JDK/framework major-version bump can surface
  behavior differences unit tests alone won't catch.

## Alternatives considered
- **Stay on 4.0.x/Java 21:** avoids migration risk, but reopens exactly the
  "imminent migration" problem ADR-0001 was written to avoid, now that 4.1/25
  are the current LTS-track versions.
- **Bump Java only, stay on Spring Boot 4.0.x:** JUnit 6 wouldn't come for
  free (4.0.x's BOM still manages JUnit 5), requiring a manual, unsupported
  override instead of riding the framework's own dependency management.
