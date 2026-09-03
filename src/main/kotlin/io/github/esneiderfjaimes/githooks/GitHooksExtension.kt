package io.github.esneiderfjaimes.githooks

open class GitHooksExtension {
    /**
     * Install hooks automatically after project evaluation. Default: true.
     */
    var autoInstall: Boolean = true

    /**
     * Optional destination directory for the installed Git hooks.
     *
     * - When `null` (default), the plugin asks Git for the real hooks path via
     *   `git rev-parse --git-path hooks`, which correctly resolves worktrees
     *   (where `.git` is a file) and a custom `core.hooksPath`.
     * - When set, this path wins and is used as-is (relative paths are resolved
     *   against the project directory).
     *
     * May also be overridden per-invocation with the Gradle property
     * `-PgitHooksDir=<path>`.
     */
    var hooksDir: String? = null
}
