pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    // auto-provisions toolchain JDKs (used here for JDK 25 on dev machines and
    // as a fallback in CI). bump via dependabot.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "fsrs-kt"
include(":fsrs-kt")
