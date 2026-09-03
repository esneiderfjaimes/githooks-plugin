# Documentation — Git Hooks Installer (Gradle Plugin)

## What is it?

A lightweight **Gradle plugin** that automatically installs your project's *Git hooks*.
It takes the files you place in the project's `hooks/` directory and copies them into
`.git/hooks/`, granting them executable permissions.

- **Plugin ID:** `io.github.esneiderfjaimes.githooks`
- **Group / coordinates:** `io.github.esneiderfjaimes` — version `0.0.3`
- **Language:** Kotlin (using `kotlin-dsl` and `java-gradle-plugin`)
- **Publishing:** Gradle Plugin Portal (via `com.gradle.plugin-publish`)
- **Requirements:** Gradle 8.12 (defined in the wrapper)

## What is it for?

*Git hooks* (for example `pre-commit`, `commit-msg`, `pre-push`) are scripts that Git
runs at specific points in the workflow. The common problem is that the `.git/hooks/`
directory is **not versioned**, so each team member would have to install the hooks
manually.

This plugin solves that: the hooks are stored versioned in `hooks/`, and the plugin
syncs them into `.git/hooks/` automatically. This ensures **consistent hook behavior
across the whole team and every environment**.

## Project structure

```
githooks-plugin/
├── build.gradle.kts                 # Plugin configuration and publishing
├── settings.gradle.kts              # Root project name
├── gradle/wrapper/                  # Gradle Wrapper (8.12)
├── src/
│   ├── main/kotlin/io/github/esneiderfjaimes/githooks/
│   │   ├── GitHooksPlugin.kt        # Main plugin logic
│   │   └── GitHooksExtension.kt     # `gitHooks {}` configuration block
│   └── test/kotlin/io/github/esneiderfjaimes/githooks/
│       └── GitHooksPluginTest.kt    # Tests using Gradle TestKit
├── README.md
└── LICENSE
```

In a project that **uses** the plugin, the expected structure is:

```
your-project/
├── hooks/
│   ├── pre-commit
│   └── commit-msg
└── build.gradle.kts
```

## Main components

### `GitHooksPlugin.kt`
The class that implements `Plugin<Project>`. When applied, it:

1. Creates the `gitHooks` configuration extension.
2. In `afterEvaluate`, if `autoInstall` is enabled **and** the `.git` directory exists,
   it installs the hooks automatically.
3. Registers two Gradle tasks in the `git` group:
   - `installGitHooks`
   - `uninstallGitHooks`

### `GitHooksExtension.kt`
Defines the `gitHooks {}` configuration block with a single option:

```kotlin
open class GitHooksExtension {
    var autoInstall: Boolean = true
}
```

## How installation works

The internal `installGitHooks` function implements an **idempotent** installation:

1. If the `hooks/` directory does not exist, it does nothing.
2. Computes a **signature** of all hooks by concatenating `name:sha256` of each file
   (sorted by name).
3. Compares that signature against the one stored in the marker file
   `.git/hooks/.installed`.
   - If they are equal → hooks are already up to date and installation is **skipped**.
   - If they changed → it continues.
4. Deletes the entire `.git/hooks/` directory, recreates it empty, and copies each file
   from `hooks/`, marking it as executable.
5. Writes the new signature into `.installed`.

> This means hooks are only reinstalled when their **content changes**
> (verified via SHA-256 hash).

## How uninstallation works

The `uninstallGitHooks` function:

1. If `.git/hooks/` does not exist, it does nothing.
2. Deletes all files in `.git/hooks/` **except** `.gitignore`.
3. Removes the `.installed` marker file.

## Available tasks

| Task                | Description                                              |
|---------------------|----------------------------------------------------------|
| `installGitHooks`   | Installs hooks from `hooks/` into `.git/hooks/`          |
| `uninstallGitHooks` | Removes all installed hooks from `.git/hooks/`           |

Manual execution:

```bash
./gradlew installGitHooks
./gradlew uninstallGitHooks
```

## Usage

Apply the plugin in `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.esneiderfjaimes.githooks") version "<version>"
}
```

Optional configuration:

```kotlin
gitHooks {
    autoInstall = true   // Install hooks automatically after evaluation (default: true)
}
```

## Notes and behavior

- If `.git/` does not exist (for example, it is not a Git repository), the plugin
  **skips** installation/uninstallation.
- Hooks are only reinstalled if their content changed (based on the SHA-256 hash).
- The `.installed` file is used as a marker to record the current hook signature.
- The `.gitignore` file inside `.git/hooks/` is never touched.

## Tests

The project includes tests using **Gradle TestKit** (`GitHooksPluginTest.kt`), which
create a temporary project and verify the real scenarios:

**Installation (`HookInstallation`):**
- Copies hooks correctly.
- Does nothing if `hooks/` is empty.
- Replaces existing files in `.git/hooks/` (including removing previous hooks).
- Does nothing if `hooks/` does not exist.

**Uninstallation (`HookUninstallation`):**
- Removes all hooks including the `.installed` marker.
- Does nothing if `.git/hooks/` does not exist.
- Leaves the `.gitignore` file intact.

Run the tests:

```bash
./gradlew test
```
