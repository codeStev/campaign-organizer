# ADR-0056: Gradle build tool for the backend

- Status: Accepted
- Date: 2026-08-21

## Context
The backend has used Maven (`pom.xml`, Spring Boot's `spring-boot-starter-parent`,
the `maven-failsafe-plugin`/`jacoco-maven-plugin` combo) since ADR-0001. A move to
Gradle was requested directly, independent of any framework/version change.

## Decision
Replace Maven with **Gradle 9.7.1**, invoked exclusively through the wrapper
(`./gradlew`, checked into `gradle/wrapper/` and `gradlew*` at the **repo root**)
so no locally installed Gradle version is required. Build scripts use the
**Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`).

The build is a **root-level multi-module Gradle build**: `settings.gradle.kts` at
the repo root declares `include("backend")`, and `backend/build.gradle.kts` is
the `:backend` subproject's build script. This gives the repo one Gradle entry
point (`./gradlew :backend:<task>` from the root) instead of a build tool nested
inside `backend/`, matching the repo's existing monorepo shape (ADR-0010) without
folding the frontend's separate npm/Vite build (ADR-0002) into Gradle — there is
no Java/JVM code on the frontend side for Gradle to build, and CI already treats
frontend and backend as independent jobs.

- `org.springframework.boot` (4.1.0) + `io.spring.dependency-management` (1.1.7)
  plugins replace the `spring-boot-starter-parent` BOM and
  `spring-boot-maven-plugin`; all dependency coordinates and pinned versions
  (jjwt, MapStruct, PDFBox, flexmark, springdoc) carry over unchanged from
  `pom.xml`. ArchUnit switches from the `archunit-junit5` artifact to
  `archunit-junit6` (same 1.5.0 version) since the project runs JUnit 6 per
  ADR-0051.
- The Java toolchain is set to language version 25 (`java.toolchain.languageVersion`),
  matching `java.version` in the old POM. The `org.gradle.toolchains.foojay-resolver-convention`
  plugin is applied in `settings.gradle.kts` so a matching JDK auto-provisions on
  machines that don't already have one (CI's `actions/setup-java` still installs
  JDK 25 directly, which Gradle detects ahead of any download).
- The Maven surefire/failsafe split (unit tests on `test`, `*IT` Testcontainers
  tests on `verify`) is reproduced with two `Test` tasks: `test` (excludes
  `**/*IT.class`) and a new `integrationTest` task (includes only `**/*IT.class`),
  wired into `check` via `dependsOn`.
- `jacoco-maven-plugin` (0.8.15, pinned for Java 25 class-file support per
  ADR-0051) is replaced by the core `jacoco` plugin at the same tool version,
  with `jacocoTestReport` merging exec data from both `test` and `integrationTest`
  runs, same as the old `prepare-agent`/`report` executions did across surefire
  and failsafe.
- `backend/Dockerfile`'s build stage moves from `maven:3.9-eclipse-temurin-25` to
  `eclipse-temurin:25-jdk` + the Gradle wrapper (dependency-layer caching via a
  `./gradlew :backend:dependencies` warm-up, then `./gradlew :backend:bootJar`);
  the runtime stage is unchanged. Because the wrapper now lives at the repo root,
  the backend image's Docker build context moves from `./backend` to `.` (repo
  root) in `docker-compose.yml`, with `dockerfile: backend/Dockerfile`; a new
  root `.dockerignore` replaces `backend/.dockerignore` (Docker only reads a
  `.dockerignore` at the build context root) and excludes `frontend/node_modules`,
  `.git`, and Gradle/build output from that wider context. CI's backend job runs
  `./gradlew :backend:check` instead of `mvn verify` and switches
  `actions/setup-java`'s cache from `maven` to `gradle`.
- `pom.xml` is deleted; there is no dual-build-tool transition period.

## Consequences
- `mvn ...` no longer works anywhere in this repo; all backend commands (README,
  `CLAUDE.md`, `docs/architecture/architecture-harness.md`) now read
  `./gradlew ...`. Historical ADRs that mention Maven (e.g. ADR-0055's note about
  the `maven:3.9-eclipse-temurin-25` build image) describe the environment as it
  was at that decision's time and are left as-is, per this repo's immutable-ADR
  convention.
- Gradle's incremental build and build cache (`--build-cache` in CI) should make
  repeat builds faster than Maven's; first-run cold-cache time is comparable
  (both resolve the same Maven Central artifacts).
- Contributors need no local Gradle install — only the committed wrapper JAR,
  which downloads the pinned 9.7.1 distribution on first invocation.
- The `test`/`integrationTest` split relies on the existing `*Test`/`*IT` naming
  convention continuing to be followed; a misnamed integration test would
  silently run under the wrong task (true of the old Maven setup too, since
  surefire/failsafe used the same naming-based split).

## Alternatives considered
- **Groovy DSL (`build.gradle`):** more examples to copy from, but the Kotlin DSL
  gives typed accessors and IDE completion, and the rest of the toolchain (Java
  25, Spring Boot 4.1) is already committed to staying current — Kotlin DSL is
  Gradle's own recommended default going forward.
- **Keep Maven, add Gradle alongside:** rejected — running two build tools for one
  module is pure maintenance overhead with no benefit once the migration is
  verified.
- **Wrap the frontend as a Gradle module too** (e.g. via a Node/Gradle plugin so
  `./gradlew build` also drives `npm ci`/`npm run build`): rejected — the
  frontend isn't JVM code, gains nothing from Gradle's dependency/toolchain
  machinery, and CI already builds it as an independent job with its own
  npm-native caching; routing it through Gradle would just add an indirection
  layer over the same `npm` commands.
- **Keep the Gradle wrapper nested in `backend/`** (single-module, no root
  `settings.gradle.kts`): simpler to set up, but leaves the repo without a
  single build entry point and doesn't reflect that `backend/` is a project
  root's subproject rather than a build root of its own — a root-level
  multi-module layout (even with only one subproject today) makes room for
  future JVM subprojects without another restructuring.
- **JUnit tag-based unit/integration split** instead of task-level class-name
  filtering: would let both suites share one `test` task via
  `useJUnitPlatform { includeTags/excludeTags }`, but would require re-tagging
  every existing `*IT` class instead of reusing the naming convention already in
  place.
