plugins {
    // Lets Gradle auto-download the Java toolchain pinned in build.gradle.kts
    // (so the plugin's bytecode target is reproducible regardless of the local JDK).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "githooks-plugin"
