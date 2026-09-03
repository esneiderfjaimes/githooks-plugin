package io.github.esneiderfjaimes.githooks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for resolving the hooks destination directory, covering the git WORKTREE
 * layout (where `.git` is a FILE containing `gitdir: <path>`, not a directory) and
 * a configurable custom destination.
 *
 * Regression: previously the plugin hardcoded `project.file(".git/hooks")` and
 * called `mkdirs()`, so a worktree `.git` file made the copy throw
 * `FileNotFoundException: ... (Not a directory)` and the whole build failed. The
 * fix resolves the real path via `git rev-parse --git-path hooks` (or an explicit
 * `gitHooks.hooksDir` / `-PgitHooksDir=` override) and NEVER breaks the build:
 * when the path cannot be resolved it warns and skips.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitHooksPluginWorktreeTest {

    @TempDir
    lateinit var testProjectDir: File

    private fun writeBuildFile(extensionBlock: String = "") {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.esneiderfjaimes.githooks")
            }
            $extensionBlock
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

    private fun run(vararg args: String) = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments(*args)
        .forwardOutput()
        .build()

    @Nested
    inner class NeverBreaksTheBuild {

        /**
         * With `.git` as a file and no explicit path, the temp project is not a real
         * Git repo, so the path cannot be resolved. The build must SUCCEED with a
         * warn-and-skip instead of failing with "Not a directory".
         */
        @Test
        fun `dot git as a file does not break installGitHooks`() {
            println("worktree: .git is a file, installGitHooks warns and skips (no build failure)")

            writeBuildFile()
            writeWorktreeGitFile()
            writeSampleHook()

            val result = run("installGitHooks")

            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
            assertTrue(File(testProjectDir, ".git").isFile, "Expected .git to remain a file")
        }

        /**
         * Auto-install runs during configuration on ANY task. It must not fail the
         * build in a worktree layout when the path cannot be resolved.
         */
        @Test
        fun `auto install does not break any task in a worktree`() {
            println("worktree: auto-install warns and skips on any task (no build failure)")

            writeBuildFile()
            writeWorktreeGitFile()
            writeSampleHook()

            val result = run("help")

            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
        }
    }

    @Nested
    inner class ConfigurableDestination {

        /**
         * A custom `gitHooks.hooksDir` wins over Git resolution and is used as the
         * install destination — this is how a worktree user pins an explicit path.
         */
        @Test
        fun `installs into the configured hooksDir`() {
            println("configured hooksDir is honored")

            writeBuildFile(
                """
                gitHooks {
                    hooksDir = "custom-hooks"
                }
                """.trimIndent()
            )
            writeSampleHook()

            val result = run("installGitHooks")

            val installed = File(testProjectDir, "custom-hooks/pre-commit")
            assertTrue(installed.exists(), "Expected hook installed into custom-hooks/")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }

        /**
         * The `-PgitHooksDir=<path>` Gradle property overrides everything, even a
         * worktree `.git` file, and must install cleanly without breaking the build.
         */
        @Test
        fun `command line property overrides destination even with dot git file`() {
            println("-PgitHooksDir overrides destination in a worktree layout")

            writeBuildFile()
            writeWorktreeGitFile()
            writeSampleHook()

            val result = run("installGitHooks", "-PgitHooksDir=cli-hooks")

            val installed = File(testProjectDir, "cli-hooks/pre-commit")
            assertTrue(installed.exists(), "Expected hook installed into cli-hooks/")
            assertEquals(TaskOutcome.SUCCESS, result.task(":installGitHooks")?.outcome)
        }
    }
}
