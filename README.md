# Git Hooks Installer Gradle Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.esneiderfjaimes.githooks?color=%2302303a)](https://plugins.gradle.org/plugin/io.github.esneiderfjaimes.githooks)

A lightweight Gradle plugin that automatically installs Git hooks from your project's `hooks/` directory into
`.git/hooks/`, with executable permissions. Includes support for uninstalling hooks and optional configuration.

---

## 🧩 Plugin ID

```kotlin
plugins {
    id("io.github.esneiderfjaimes.githooks") version "<version>"
}
```

---

## 📦 What It Does

* Installs all files from the `hooks/` directory into your project's `.git/hooks/`, replacing any existing ones.
* Ensures all copied files are executable.
* Can uninstall previously installed hooks.
* Supports configuration via the `gitHooks` block in `build.gradle.kts`.

This is especially useful when working in teams to ensure consistent Git hook behavior across environments.

---

## 📂 Directory Structure Example

```
your-project/
├── hooks/
│   ├── pre-commit
│   └── commit-msg
└── build.gradle.kts
```

---

## 🛠 How to Use

### 1. Install Hooks Manually

Run the following Gradle task:

```bash
./gradlew installGitHooks
```

This will copy all files from `hooks/` to `.git/hooks/` with executable permissions.

---

### 2. Uninstall Hooks

To remove all previously installed hooks (including the internal `.installed` signature file):

```bash
./gradlew uninstallGitHooks
```

This clears the `.git/hooks/` directory, unless the `hooks/` directory is missing or empty. Files like `.gitignore` will
remain untouched.

---

### 3. Configure Behavior (Optional)

You can configure the plugin using the `gitHooks` extension block:

```kotlin
gitHooks {
    autoInstall = true     // Install hooks automatically after evaluation (default: true)
}
```

This is helpful if you want hooks to be installed/uninstalled automatically when running any Gradle task.

---

## ✅ Tasks Provided

| Task Name           | Description                                    |
|---------------------|------------------------------------------------|
| `installGitHooks`   | Installs hooks from `hooks/` to `.git/hooks/`  |
| `uninstallGitHooks` | Removes all installed hooks from `.git/hooks/` |

---

## 💡 Notes

* If `.git/` is missing (e.g., not a Git repo), the plugin will skip installation/uninstallation.
* Hooks are only reinstalled if their content has changed (based on SHA-256 hash).
* A `.installed` file is used to track the current hook signature.

---
