plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.esneiderfjaimes"
version = "0.0.2"

repositories {
    mavenCentral()
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