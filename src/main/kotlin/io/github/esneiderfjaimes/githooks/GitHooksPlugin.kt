package io.github.esneiderfjaimes.githooks

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Suppress("unused")
class GitHooksPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("gitHooks", GitHooksExtension::class.java)

        project.afterEvaluate {
            if (extension.autoInstall) {
                println("[>] Auto-installing Git hooks after evaluation")
                installGitHooks(project, extension)
            }
        }

        project.tasks.register("installGitHooks") {
            group = "git"
            description = "Cleans and installs Git hooks from the hooks/ directory"

            doLast {
                installGitHooks(project, extension)
            }
        }

        project.tasks.register("uninstallGitHooks") {
            group = "git"
            description = "Removes all Git hooks and the installation marker"

            doLast {
                uninstallGitHooks(project, extension)
            }
        }
    }

    /**
     * Resolves the directory where hooks should be installed, WITHOUT ever failing
     * the build. Precedence:
     *
     * 1. Gradle property `-PgitHooksDir=<path>` (per-invocation override).
     * 2. `gitHooks { hooksDir = "..." }` extension value.
     * 3. Git itself: `git rev-parse --git-path hooks` — resolves worktrees (where
     *    `.git` is a file) and a custom `core.hooksPath`.
     *
     * Returns `null` (and logs a warning) when the path cannot be resolved, e.g.
     * not a Git repository or `git` is unavailable. Callers must skip on null.
     */
    private fun resolveHooksDir(project: Project, extension: GitHooksExtension): File? {
        val override = (project.findProperty("gitHooksDir") as? String)?.takeIf { it.isNotBlank() }
            ?: extension.hooksDir?.takeIf { it.isNotBlank() }

        if (override != null) {
            val resolved = project.file(override)
            println("[>] Using configured hooks directory: ${resolved.absolutePath}")
            return resolved
        }

        val gitPath = gitHooksPathFromGit(project.projectDir)
        if (gitPath == null) {
            println("[!] Could not resolve the Git hooks path (not a Git repository or 'git' unavailable), skipping")
            return null
        }

        // `git rev-parse --git-path hooks` may return a path relative to the project dir.
        val resolved = if (File(gitPath).isAbsolute) File(gitPath) else File(project.projectDir, gitPath)
        println("[>] Resolved Git hooks directory: ${resolved.absolutePath}")
        return resolved
    }

    /**
     * Asks Git for the real hooks path. Returns null on any failure (no repo, no
     * git binary, non-zero exit) instead of throwing.
     */
    private fun gitHooksPathFromGit(workingDir: File): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "--git-path", "hooks")
                .directory(workingDir)
                .redirectErrorStream(false)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                return null
            }
            if (process.exitValue() != 0 || output.isEmpty()) null else output
        } catch (e: Exception) {
            null
        }
    }

    private fun installGitHooks(project: Project, extension: GitHooksExtension) {
        val hooksDir = project.file("hooks")

        if (!hooksDir.exists()) {
            println("[!] hooks/ directory not found, skipping Git hooks installation")
            return
        }

        val gitHooksDir = resolveHooksDir(project, extension) ?: return

        // Never break the build: if the resolved path is blocked by an existing
        // non-directory (e.g. a stale file), warn and skip instead of throwing.
        if (gitHooksDir.exists() && !gitHooksDir.isDirectory) {
            println("[!] Resolved hooks path is not a directory: ${gitHooksDir.absolutePath}, skipping")
            return
        }

        val markerFile = File(gitHooksDir, ".installed")

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
            println("[>] Removing existing hooks directory: ${gitHooksDir.absolutePath}")
            gitHooksDir.deleteRecursively()
        }
        println("[>] Creating fresh hooks directory: ${gitHooksDir.absolutePath}")
        if (!gitHooksDir.mkdirs() && !gitHooksDir.isDirectory) {
            println("[!] Could not create hooks directory: ${gitHooksDir.absolutePath}, skipping")
            return
        }

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

        println("[OK] Git hooks installed successfully")
    }

    private fun uninstallGitHooks(project: Project, extension: GitHooksExtension) {
        val gitHooksDir = resolveHooksDir(project, extension) ?: return

        if (!gitHooksDir.exists() || !gitHooksDir.isDirectory) {
            println("[>] No hooks directory found, nothing to remove")
            return
        }

        val markerFile = File(gitHooksDir, ".installed")

        gitHooksDir.listFiles()?.forEach { file ->
            if (file.name != ".gitignore") {
                file.delete()
            }
        }

        markerFile.delete()

        println("[OK] Git hooks uninstalled successfully")
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
