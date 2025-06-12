package io.github.esneiderfjaimes.githooks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitHooksPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    @BeforeEach
    fun setupBuildFile() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.esneiderfjaimes.githooks")
            }
            """.trimIndent()
        )
    }

    private fun runInstallGitHooksGradleTask() = baseRunGradleTask("installGitHooks")

    private fun runUninstallGitHooksGradleTask() = baseRunGradleTask("uninstallGitHooks")

    private fun baseRunGradleTask(nameTask: String) = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments(nameTask)
        .forwardOutput()
        .build()

    @Nested
    inner class HookInstallation {

        @Test
        fun `copies hooks correctly`() {
            println("copies hooks correctly")

            val hooksDir = File(testProjectDir, "hooks").apply { mkdirs() }
            File(hooksDir, "pre-commit").apply {
                writeText("#!/bin/sh\necho 'Hook test'")
                setExecutable(true)
            }

            File(testProjectDir, ".git/hooks").mkdirs()

            val result = runInstallGitHooksGradleTask()

            val hookInstalled = File(testProjectDir, ".git/hooks/pre-commit")
            assertTrue(hookInstalled.exists(), "Expected pre-commit hook to be installed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }

        @Test
        fun `does nothing if hooks directory is empty`() {
            println("does nothing if hooks directory is empty")

            File(testProjectDir, "hooks").mkdirs()
            File(testProjectDir, ".git/hooks").mkdirs()

            val result = runInstallGitHooksGradleTask()

            val installed = File(testProjectDir, ".git/hooks")
                .listFiles()?.filter { it.name != ".installed" } ?: emptyList()
            assertTrue(installed.isEmpty(), "Expected no hook files to be installed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }

        @Test
        fun `replaces existing files in git hooks directory`() {
            println("replacing existing files in git hooks")

            val hooksDir = File(testProjectDir, "hooks").apply { mkdirs() }
            File(hooksDir, "pre-commit").apply {
                writeText("#!/bin/sh\necho 'New hook'")
                setExecutable(true)
            }

            val gitHooks = File(testProjectDir, ".git/hooks").apply { mkdirs() }
            File(gitHooks, "pre-commit").writeText("#!/bin/sh\necho 'Old hook'")
            File(gitHooks, "custom-hook").writeText("#!/bin/sh\necho 'Should be removed'")

            val result = runInstallGitHooksGradleTask()

            val finalHooks = gitHooks.listFiles()?.map { it.name } ?: emptyList()
            assertTrue("pre-commit" in finalHooks)
            assertTrue("custom-hook" !in finalHooks)

            val newContent = File(gitHooks, "pre-commit").readText()
            assertTrue("New hook" in newContent)
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }

        @Test
        fun `does nothing if hooks directory does not exist`() {
            println("replacing existing files in git hooks")

            File(testProjectDir, ".git/hooks").mkdirs()

            val result = runInstallGitHooksGradleTask()

            val files = File(testProjectDir, ".git/hooks")
                .listFiles()?.filter { it.name != ".installed" } ?: emptyList()
            assertTrue(files.isEmpty(), "Expected no hook files to be installed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }
    }

    @Nested
    inner class HookUninstallation {

        @Test
        fun `removes all hooks including installed marker`() {
            println("removes all hooks including .installed")

            val gitHooksDir = File(testProjectDir, ".git/hooks").apply { mkdirs() }

            // Create dummy hooks and marker
            File(gitHooksDir, "pre-commit").writeText("#!/bin/sh\necho 'hook'")
            File(gitHooksDir, "pre-push").writeText("#!/bin/sh\necho 'another hook'")
            File(gitHooksDir, ".installed").writeText("some-signature")

            val result = runUninstallGitHooksGradleTask()

            val remainingFiles = gitHooksDir.listFiles() ?: emptyArray()
            assertTrue(remainingFiles.isEmpty(), "Expected .git/hooks to be empty after uninstall")
            assertEquals(TaskOutcome.SUCCESS, result.task(":uninstallGitHooks")?.outcome)
        }

        @Test
        fun `does nothing when git hooks directory does not exist`() {
            println("does nothing when hooks dir doesn't exist")

            // No .git/hooks folder created

            val result = runUninstallGitHooksGradleTask()

            assertEquals(TaskOutcome.SUCCESS, result.task(":uninstallGitHooks")?.outcome)
        }

        @Test
        fun `leaves gitignore file intact`() {
            println("leaves .gitignore intact after uninstall")

            val gitHooksDir = File(testProjectDir, ".git/hooks").apply { mkdirs() }

            // Add .gitignore and a hook
            File(gitHooksDir, ".gitignore").writeText("# ignore hooks")
            File(gitHooksDir, "pre-commit").writeText("#!/bin/sh\necho 'hook'")
            File(gitHooksDir, ".installed").writeText("some-signature")

            val result = runUninstallGitHooksGradleTask()

            val remainingFiles = gitHooksDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(".gitignore" in remainingFiles, "Expected .gitignore to remain")
            assertTrue("pre-commit" !in remainingFiles, "Expected pre-commit to be removed")
            assertTrue(".installed" !in remainingFiles, "Expected .installed to be removed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":uninstallGitHooks")?.outcome)
        }
    }
}