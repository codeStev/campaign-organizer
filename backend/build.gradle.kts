import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.campaignorganizer"
version = "0.1.0"
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
val jjwtVersion = "0.12.6"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
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

    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

    // Compile-time bean mapping between rings (domain <-> entity <-> DTO).
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Article body Markdown -> HTML rendering (ADR-0054).
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

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
    finalizedBy(tasks.named("jacocoTestReport"))
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
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named("check") {
    dependsOn(integrationTest)
}

// Coverage across unit and integration tests: the agent appends both runs to
// one exec file, reported once both suites have run.
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"), integrationTest)
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
