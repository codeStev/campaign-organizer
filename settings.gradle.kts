plugins {
    // Auto-provisions a matching JDK (Java 25) when one isn't already installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "campaign-organizer"

include("backend")
