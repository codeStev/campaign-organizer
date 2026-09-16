import org.gradle.api.tasks.testing.Test

plugins {
    java
    // 4.1.1, not 4.1.0: manages Spring Security 7.1.1, which fixes CVE-2026-47841
    // (WebAuthn user-verification bypass via session deserialization identity comparison).
    // Doesn't affect this app's exploit surface directly (no distributed session store here),
    // but there's no reason to build new WebAuthn support on a version with a known WebAuthn CVE.
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.campaignorganizer"
// Not authoritative — the git tag is the version (ADR-0060). Left static.
version = "0.0.0"
description = "Personal worldbuilding and RPG campaign manager"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"
val archunitVersion = "1.5.0"
val jjwtVersion = "0.13.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Not bundled with spring-boot-starter-security — a separate module (ADR-0111
    // "Consequences" originally claimed otherwise; corrected after direct jar
    // inspection found no org.springframework.security.web.webauthn.* package in
    // spring-security-web). No explicit version: Spring Boot's BOM manages it.
    implementation("org.springframework.security:spring-security-webauthn")
    // Spring's WebAuthnConfigurer wraps this internally (Webauthn4JRelyingPartyOperations)
    // but Spring's own docs still list it as a required companion dependency. No explicit
    // version: spring-security-webauthn:7.1.1 itself declares 0.31.9.RELEASE as its own
    // dependency — pinning an older version here (an earlier draft tried 0.29.1.RELEASE,
    // based on a stale Maven Central search-index result) would silently downgrade below
    // what Spring actually built and tested against.
    implementation("com.webauthn4j:webauthn4j-core")
    // Google sign-in (ADR-0113) — no explicit version, Boot's BOM manages it alongside
    // spring-boot-starter-security/spring-security-webauthn.
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Boot 4 modularized autoconfig: this module carries FlywayAutoConfiguration
    // (and pulls flyway-core). Without it, migrations never run.
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // TOTP generation/verification (ADR-0111).
    implementation("dev.samstevens.totp:totp:1.7.1")
    // Server-rendered TOTP enrollment QR code (data:image/png;base64,... — no client-side
    // QR-rendering library needed).
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.google.zxing:javase:3.5.4")

    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260313.1")

    // Compile-time bean mapping between rings (domain <-> entity <-> DTO).
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("org.apache.pdfbox:pdfbox:3.0.8")

    // Article body Markdown -> HTML rendering (ADR-0054).
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Boot 4 moved MockMvc test auto-configuration into its own module.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // Architecture fitness functions (dependency rule, ring isolation).
    testImplementation("com.tngtech.archunit:archunit-junit6:$archunitVersion")
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Unit tests only; *IT classes run under the integrationTest task below.
    exclude("**/*IT.class")
}

// Mirrors the Maven build's failsafe execution: *IT classes exercise the
// Testcontainers-backed integration suite and run separately from unit tests.
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests (*IT)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*IT.class")
    useJUnitPlatform()
    shouldRunAfter(tasks.named("test"))
}

// Coverage across unit and integration tests: the agent appends both runs to
// one exec file, reported once both suites have run. Only wired into `check`
// (not `test`), so a plain `test` invocation stays unit-tests-only.
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"), integrationTest)
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(integrationTest, tasks.named("jacocoTestReport"))
}
