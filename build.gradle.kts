plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.esneiderfjaimes"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Compile the plugin against a low, stable Java version so consumers on older
// Gradle JVMs can load it. Without this, the plugin is compiled with whatever
// JDK is installed locally (e.g. JDK 24), producing bytecode that a consumer's
// Gradle JVM (Java 21 or lower) cannot load. Gradle provisions this toolchain
// automatically (via the foojay resolver in settings.gradle.kts), independent of
// the local JDK. 17 is an LTS that matches CI and covers Gradle 8 on JDK 17+.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

gradlePlugin {
    website = "https://github.com/esneiderfjaimes/githooks-plugin"
    vcsUrl = "https://github.com/esneiderfjaimes/githooks-plugin.git"
    plugins {
        create("gitHooksPlugin") {
            id = "io.github.esneiderfjaimes.githooks"
            implementationClass = "io.github.esneiderfjaimes.githooks.GitHooksPlugin"
            displayName = "Git Hooks Installer Plugin"
            description = "Automatically installs git hooks from the hooks/ folder"
            tags = listOf("git", "hooks", "git-hooks")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}