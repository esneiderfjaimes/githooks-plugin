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

    private fun runGradleTask() = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installGitHooks")
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

            val result = runGradleTask()

            val hookInstalled = File(testProjectDir, ".git/hooks/pre-commit")
            assertTrue(hookInstalled.exists(), "Expected pre-commit hook to be installed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }

        @Test
        fun `does nothing if hooks directory is empty`() {
            println("does nothing if hooks directory is empty")

            File(testProjectDir, "hooks").mkdirs()
            File(testProjectDir, ".git/hooks").mkdirs()

            val result = runGradleTask()

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

            val result = runGradleTask()

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

            val result = runGradleTask()

            val files = File(testProjectDir, ".git/hooks")
                .listFiles()?.filter { it.name != ".installed" } ?: emptyList()
            assertTrue(files.isEmpty(), "Expected no hook files to be installed")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }
    }
}