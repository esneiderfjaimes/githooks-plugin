# Publishing Guide — Git Hooks Installer Plugin

How to publish this plugin, both **locally** (for testing against another project)
and **remotely** to the [Gradle Plugin Portal](https://plugins.gradle.org).

This project is already configured for portal publishing via the
`com.gradle.plugin-publish` plugin in `build.gradle.kts`:

---

## 0. Before every release: bump the version

`version` in `build.gradle.kts` is the published version. The portal **rejects a
re-publish of an existing version**, so bump it before publishing:

```kotlin
version = "0.1.0"
```

Also update the version badge / examples in `README.md` if they pin a version.
Commit and tag the release:

```bash
git commit -am "release: 0.1.0"
git tag v0.1.0
```

---

## 1. Local publishing (test before you ship)

Local publishing installs the plugin into a local repository so you can consume it
from another project **without** going to the portal. Two options:

### Option A — publish to Maven Local (`~/.m2/repository`)

The `java-gradle-plugin` sets up a `pluginMaven` publication, so `publishToMavenLocal`
works out of the box:

```bash
./gradlew publishToMavenLocal
```

Then, in the **consuming** project's `settings.gradle.kts`, add `mavenLocal()` to the
plugin repositories:

```kotlin
// settings.gradle.kts (consumer)
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

And apply it in the consumer's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.esneiderfjaimes.githooks") version "0.1.0"
}
```

### Option B — publish to a local directory repository

Useful for a throwaway repo you can point a test build at:

```bash
./gradlew publish -PpublishDir="$PWD/build/local-repo"
```

> Note: plain `publish` only works if a `maven { url = ... }` target exists. If you
> have not added one, prefer Option A (`publishToMavenLocal`) or `publishPlugins`
> with the portal's validation-only flag (see the dry run below).

### Sanity-check the artifacts

```bash
./gradlew build          # compiles + runs tests + assembles the jar
ls build/libs            # githooks-plugin-<version>.jar
```

---

## 2. Remote publishing to the Gradle Plugin Portal

### 2.1 One-time: create portal credentials

1. Sign in at [plugins.gradle.org](https://plugins.gradle.org) (GitHub login works).
2. Go to your profile → **API Keys** and generate a key. You get a **key** and a
   **secret**.

Store them in `~/.gradle/gradle.properties` (NOT in the repo):

```properties
gradle.publish.key=<your-key>
gradle.publish.secret=<your-secret>
```

Alternatively pass them per-invocation:

```bash
./gradlew publishPlugins \
  -Pgradle.publish.key=<your-key> \
  -Pgradle.publish.secret=<your-secret>
```

### 2.2 Validate first (dry run)

The `plugin-publish` plugin can validate the publication without actually shipping it:

```bash
./gradlew publishPlugins --validate-only
```

Fix anything it flags (missing `website`/`vcsUrl`, empty description, etc.).

### 2.3 Publish

```bash
./gradlew publishPlugins
```

On success the plugin appears at:

```
https://plugins.gradle.org/plugin/io.github.esneiderfjaimes.githooks
```

The first publish of a **new plugin id** goes through an approval step on the portal;
subsequent version publishes are immediate.

### 2.4 Consume the published plugin

```kotlin
plugins {
    id("io.github.esneiderfjaimes.githooks") version "0.1.0"
}
```

No extra repository is needed — `gradlePluginPortal()` is in Gradle's default plugin
repositories.

---

## 3. Optional: publish from CI (GitHub Actions)

The current CI (`.github/workflows/ci.yml`) only builds and tests — it does **not**
publish. To publish on a tag, add a separate workflow that runs `publishPlugins` with
the credentials stored as GitHub repository secrets
(`GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`):

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags: ["v*"]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - uses: gradle/actions/setup-gradle@v4
      - name: Publish to Gradle Plugin Portal
        env:
          GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
          GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}
        run: |
          ./gradlew publishPlugins \
            -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" \
            -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"
```

Tagging `v0.1.0` and pushing the tag then publishes automatically.

---

## Quick reference

| Goal                              | Command                                              |
|-----------------------------------|------------------------------------------------------|
| Build + test + jar                | `./gradlew build`                                    |
| Publish to Maven Local            | `./gradlew publishToMavenLocal`                      |
| Validate portal publication       | `./gradlew publishPlugins --validate-only`           |
| Publish to Gradle Plugin Portal   | `./gradlew publishPlugins`                           |

## Gotchas

- **Version already exists** → the portal rejects it. Bump `version` first.
- **Credentials not found** → set `gradle.publish.key` / `gradle.publish.secret` in
  `~/.gradle/gradle.properties` or pass them with `-P`. Never commit them.
- **`publishToMavenLocal` not visible in the consumer** → add `mavenLocal()` to the
  consumer's `pluginManagement { repositories { ... } }`.
- **Wrapper** → always publish through `./gradlew` (pinned to Gradle 8.14) so the
  build is reproducible.
