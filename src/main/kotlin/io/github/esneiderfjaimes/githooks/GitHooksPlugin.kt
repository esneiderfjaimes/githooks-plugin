package io.github.esneiderfjaimes.githooks

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest

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
            println("[!] hooks/ directory not found, skipping Git hooks installation")
            return
        }

        val currentHooksSignature = hooksDir.listFiles()
            ?.sortedBy { it.name }
            ?.joinToString("\n") {
                buildString {
                    append(it.name)
                    append(":")
                    append(it.sha256())
                }
            }
            ?: ""

        val previousSignature = if (markerFile.exists()) markerFile.readText() else ""

        if (currentHooksSignature == previousSignature) {
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

        markerFile.writeText(currentHooksSignature)

        println("[>] currentHooksSignature is $currentHooksSignature")

        println("[OK] Git hooks installed successfully")
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            if (bytesRead == -1) {
                return digest.digest().joinToString("") { "%02x".format(it) }
            }
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

}