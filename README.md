# NexusMCPublisher

[English](README.md) | [简体中文](README_zh.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md)

A Java 8-compatible Gradle plugin that publishes resource versions to NexusMC using the Personal API's two-step upload flow:

1. Upload each artifact with `POST /api/upload`.
2. Publish the version with `POST /api/resources/{id}/versions`, using the URL, original filename, size, and checksums returned by the upload endpoint.

Plugin ID: `io.github.lijinhong11.nexusmc-publisher`

## Usage

```kotlin
import io.github.lijinhong11.nexusmcpublisher.VersionTag

plugins {
    java
    id("io.github.lijinhong11.nexusmc-publisher") version "1.0.0"
}

version = "1.2.3"

nexusMCPublisher {
    resourceId.set("your-resource-id")
    versionTag.set(VersionTag.RELEASE)
    versionTitle.set("Minecraft compatibility update")
    changelog.set("Fixes several issues and updates the resource artifact.")
    mcVersions.set(listOf("1.21.4"))

    // When the Java plugin is applied, the jar task output is used by default.
    // To publish a different artifact:
    // artifact("build/libs/example.jar")
}
```

## Multiple files

Each `file` block is uploaded separately. The resulting metadata is then submitted together in the version endpoint's `files[]` array. Exactly one file must be marked as primary.

```kotlin
import io.github.lijinhong11.nexusmcpublisher.VersionTag

nexusMCPublisher {
    resourceId.set("your-resource-id")
    versionTag.set(VersionTag.RELEASE)
    mcVersions.set(listOf("1.21.1", "1.21.4"))

    file {
        artifact("build/libs/plugin-paper.jar")
        primary.set(true)
        gameVersions.set(listOf("1.21.4"))
        loaders.set(listOf("paper"))
    }

    file {
        artifact("build/libs/plugin-fabric.jar")
        // primary defaults to false
        gameVersions.set(listOf("1.21.1", "1.21.4"))
        loaders.set(listOf("fabric"))
    }
}
```

If no `file {}` blocks are declared, the plugin uses the top-level `artifact` setting. When the Java plugin is applied, that setting defaults to the `jar` task output and is treated as the only primary file.

## Authentication

Do not put your NexusMC token in the build script. Provide it through an environment variable:

```bash
export NEXUSMC_API_TOKEN='avm_pa...'
./gradlew publishToNexusMC
```

Alternatively, add it to the user-level `~/.gradle/gradle.properties` file. Do not commit this value:

```properties
nexusMCToken=avm_pa...
```

The token requires these permissions:

- `upload:file`
- `resource:update:self`

## Configuration

| Property | Required | Default | Description |
|---|---:|---|---|
| `resourceId` | Yes | — | NexusMC resource ID |
| `token` | Yes | Environment variable or Gradle property | Personal API token |
| `version` | Yes | `project.version` | Version being published |
| `versionTag` | No | `VersionTag.RELEASE` | `RELEASE`, `BETA`, or `ALPHA` |
| `versionTitle` | No | `Version <version>` | Version title |
| `changelog` | No | — | Version changelog |
| `mcVersions` | No | Empty list | Minecraft versions for the release |
| `downloadType` | No | `local` | NexusMC download type |
| `artifact` | Yes | Java `jar` output | Single artifact to upload |
| `file {}` | No | — | Repeatable multi-file declaration; overrides top-level `artifact` |
| `baseUrl` | No | `https://www.nexusmc.cn` | API base URL, mainly useful for testing or private deployments |

## Building and testing

The project is built with JDK 8:

```bash
./gradlew clean test validatePlugins build
```

HTTP tests use a local test server and never upload files to the real NexusMC service.

## Publishing to the Gradle Plugin Portal

1. Register at the [Gradle Plugin Portal](https://plugins.gradle.org/) and create an API key.
2. Keep the credentials outside the repository. Environment variables are recommended:

   ```bash
   export GRADLE_PUBLISH_KEY='your-portal-key'
   export GRADLE_PUBLISH_SECRET='your-portal-secret'
   ```

   You can instead use the user-level `~/.gradle/gradle.properties` file:

   ```properties
   gradle.publish.key=your-portal-key
   gradle.publish.secret=your-portal-secret
   ```

3. Validate the release locally without uploading:

   ```bash
   ./gradlew clean test validatePlugins build
   ```

4. Set a non-SNAPSHOT project version and publish:

   ```bash
   ./gradlew publishPlugins
   ```

The first release is subject to manual review by the Gradle Plugin Portal. Every later release must use a version that has not already been published.
