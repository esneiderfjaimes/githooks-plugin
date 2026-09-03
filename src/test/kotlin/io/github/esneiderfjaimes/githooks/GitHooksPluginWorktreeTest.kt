package io.github.esneiderfjaimes.githooks

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Characterization tests for the git WORKTREE layout, where `.git` is a regular
 * FILE (containing `gitdir: <path>`) instead of a directory.
 *
 * These tests lock in the CURRENT (buggy) behavior so a later fix has a baseline:
 * the plugin hardcodes `project.file(".git/hooks")` (GitHooksPlugin.kt) and calls
 * `gitHooksDir.mkdirs()`. Because `.git` is a file, the path `.git/hooks/<hook>`
 * cannot be created and the copy throws `FileNotFoundException: ... (Not a directory)`.
 * That exception is not caught, so the WHOLE build fails unsafely instead of logging
 * a warning and skipping (which is what the README promises for a missing `.git/`).
 *
 * The intended fix will resolve the real hooks path (via `git rev-parse --git-path
 * hooks`, which also honors `core.hooksPath`, plus an optional configurable path on
 * the `gitHooks` extension) and, when git is unavailable, warn-and-continue. When
 * that lands, the expectations below are expected to change from `buildAndFail()` to
 * a successful warn-and-skip / correct-path install.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitHooksPluginWorktreeTest {

    @TempDir
    lateinit var testProjectDir: File

    private fun writeBuildFile() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.esneiderfjaimes.githooks")
            }
            """.trimIndent()
        )
    }

    /** Emulate a worktree: create `.git` as a FILE pointing at a real git dir. */
    private fun writeWorktreeGitFile() {
        val realGitDir = File(testProjectDir, "realgit/worktrees/wt").apply { mkdirs() }
        File(testProjectDir, ".git").writeText("gitdir: ${realGitDir.absolutePath}\n")
    }

    private fun writeSampleHook() {
        val hooksDir = File(testProjectDir, "hooks").apply { mkdirs() }
        File(hooksDir, "pre-commit").apply {
            writeText("#!/bin/sh\necho 'Hook test'")
            setExecutable(true)
        }
    }

    private fun runAndFail(task: String) = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments(task)
        .forwardOutput()
        .buildAndFail()

    @Nested
    inner class CurrentBehavior {

        /**
         * CURRENT behavior: running `installGitHooks` when `.git` is a file makes the
         * build FAIL with "Not a directory" instead of warning and skipping.
         */
        @Test
        fun `dot git as file fails the build unsafely on installGitHooks`() {
            println("worktree: .git is a file, installGitHooks fails the build unsafely")

            writeBuildFile()
            writeWorktreeGitFile()
            writeSampleHook()

            val result = runAndFail("installGitHooks")

            assertTrue(
                result.output.contains("Not a directory"),
                "CURRENT behavior: build fails with a 'Not a directory' error"
            )
            // `.git` is left as a file (not clobbered into a directory).
            assertTrue(File(testProjectDir, ".git").isFile, "Expected .git to remain a file")
        }

        /**
         * CURRENT behavior: the auto-install guard is `project.file(".git").exists()`,
         * which is TRUE for a file too, so auto-install runs during configuration on
         * ANY task and fails the build unsafely in a worktree layout.
         */
        @Test
        fun `auto install fails the build on any task in a worktree`() {
            println("worktree: auto-install runs on any task and fails the build unsafely")

            writeBuildFile()
            writeWorktreeGitFile()
            writeSampleHook()

            val result = runAndFail("help")

            assertTrue(
                result.output.contains("Not a directory"),
                "CURRENT behavior: auto-install fails the build with 'Not a directory'"
            )
        }
    }
}
