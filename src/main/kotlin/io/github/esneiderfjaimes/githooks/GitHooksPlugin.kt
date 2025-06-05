package io.github.esneiderfjaimes.githooks

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

@Suppress("unused")
class GitHooksPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            if (project.file(".git").exists()) {
                println("[>] Installing Git hooks after project evaluation")
                installGitHooks(project)
            }
        }

        project.tasks.register("installGitHooks") {
            group = "git"
            description = "Cleans and installs Git hooks from the hooks/ directory"

            doLast {
                installGitHooks(project)
            }
        }
    }

    private fun installGitHooks(project: Project) {
        val gitHooksDir = project.file(".git/hooks")
        val hooksDir = project.file("hooks")
        val markerFile = File(gitHooksDir, ".installed")

        if (!hooksDir.exists()) {
            println("[X] hooks/ directory not found, skipping Git hooks installation")
            return
        }

        val hooksLastModified = hooksDir.listFiles()?.maxOfOrNull { it.lastModified() } ?: 0L

        val markerLastModified = if (markerFile.exists()) markerFile.lastModified() else 0L

        if (markerFile.exists() && markerLastModified >= hooksLastModified) {
            println("[>] Git hooks already installed and up to date, skipping installation")
            return
        }

        if (gitHooksDir.exists()) {
            println("[>] Removing existing .git/hooks directory")
            gitHooksDir.deleteRecursively()
        }
        println("[>] Creating fresh .git/hooks directory")
        gitHooksDir.mkdirs()

        println("[>] Copying hooks with executable permissions")
        hooksDir.listFiles()?.forEach { hookFile ->
            val target = File(gitHooksDir, hookFile.name)
            hookFile.inputStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target.setExecutable(true)
        }

        markerFile.writeText("Installed on ${System.currentTimeMillis()}")

        println("[OK] Git hooks installed successfully")
    }
}