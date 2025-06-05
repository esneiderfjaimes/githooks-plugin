# Git Hooks Installer Gradle Plugin

A lightweight Gradle plugin that automatically installs Git hooks from your project's `hooks/` directory into `.git/hooks/`, with executable permissions.

---

## 🧩 Plugin ID

```kotlin
plugins {
    id("io.github.esneiderfjaimes.githooks") version "1.0.0"
}
```

---

## 📦 What It Does

This plugin installs all files from the `hooks/` directory into your project's `.git/hooks/` directory, replacing any existing ones. It also ensures all copied files are executable.

Useful when working in teams to ensure Git hooks (e.g., pre-commit, pre-push) are consistently installed.

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

1. Add the plugin to your `plugins` block.
2. Create a `hooks/` directory in the root of your project.
3. Add your Git hook scripts (`pre-commit`, `commit-msg`, etc.).
4. Run the task:

```bash
./gradlew installGitHooks
```

This will copy all files from `hooks/` to `.git/hooks/` with executable permissions.

---