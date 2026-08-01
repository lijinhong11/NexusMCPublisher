# NexusMCPublisher

[English](README.md) | [简体中文](README_zh.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md)

一个兼容 **JDK 8** 的 Gradle 插件，按照 NexusMC 个人 API 的两段式流程发布资源版本：

1. 通过 `POST /api/upload` 上传每个构建产物；
2. 调用 `POST /api/resources/{id}/versions`，使用上传接口返回的 URL、原始文件名、大小和校验值发布版本。

插件 ID：`io.github.lijinhong11.nexusmcpublisher`

## 使用方法

```kotlin
import io.github.lijinhong11.nexusmcpublisher.VersionTag

plugins {
    java
    id("io.github.lijinhong11.nexusmc-publisher") version "1.0.0"
}

version = "1.2.3"

nexusMCPublisher {
    resourceId.set("你的资源 ID")
    versionTag.set(VersionTag.RELEASE)
    versionTitle.set("兼容新版游戏")
    changelog.set("修复若干问题并更新资源文件。")
    mcVersions.set(listOf("1.21.4"))
    subcategoryIds.set(listOf("paper"))

    // 应用 Java 插件时，默认使用 jar 任务的输出。
    // 如需发布其他文件：
    // artifact("build/libs/example.jar")
}
```

## 多文件发布

每个 `file` 配置块会被独立上传，上传结果随后统一写入版本接口的 `files[]` 数组。必须且只能有一个文件设置为主文件。

```kotlin
import io.github.lijinhong11.nexusmcpublisher.VersionTag

nexusMCPublisher {
    resourceId.set("你的资源 ID")
    versionTag.set(VersionTag.RELEASE)
    mcVersions.set(listOf("1.21.1", "1.21.4"))

    file {
        artifact("build/libs/plugin-paper.jar")
        primary.set(true)
        gameVersions.set(listOf("1.21.4"))
        subcategoryIds.set(listOf("paper"))
    }

    file {
        artifact("build/libs/plugin-fabric.jar")
        // primary 默认为 false
        gameVersions.set(listOf("1.21.1", "1.21.4"))
        subcategoryIds.set(listOf("fabric"))
    }
}
```

如果没有声明任何 `file {}`，插件会使用顶层 `artifact` 配置。应用 Java 插件时，该配置默认使用 `jar` 任务的输出，并将其作为唯一主文件。

## 身份认证

不要把 NexusMC Token 写进构建脚本。推荐通过环境变量提供：

```bash
export NEXUSMC_API_TOKEN='avm_pa...'
./gradlew publishToNexusMC
```

也可以写入用户级 `~/.gradle/gradle.properties`，但不要提交此文件中的凭据：

```properties
nexusMCToken=avm_pa...
```

Token 需要以下权限：

- `upload:file`
- `resource:update:self`

## 配置项

| 配置 | 必填 | 默认值 | 说明 |
|---|---:|---|---|
| `resourceId` | 是 | — | NexusMC 资源 ID |
| `token` | 是 | 环境变量或 Gradle 属性 | 个人 API Token |
| `version` | 是 | `project.version` | 要发布的版本号 |
| `versionTag` | 否 | `VersionTag.RELEASE` | `RELEASE`、`BETA` 或 `ALPHA` |
| `versionTitle` | 否 | `Version <version>` | 版本标题 |
| `changelog` | 否 | — | 更新日志 |
| `mcVersions` | 否 | 空列表 | 版本支持的 Minecraft 版本 |
| `subcategoryIds` | 否 | 空列表 | 单文件发布时的 Loader/子分类 ID |
| `downloadType` | 否 | `local` | NexusMC 下载类型 |
| `artifact` | 是 | Java `jar` 输出 | 单文件发布时上传的文件 |
| `file {}` | 否 | — | 可重复声明的多文件配置，支持 `artifact`、`primary`、`gameVersions` 和 `subcategoryIds` |
| `baseUrl` | 否 | `https://www.nexusmc.cn` | API 基础地址，主要用于测试或私有部署 |

## 构建与测试

项目使用 JDK 8 构建：

```bash
./gradlew clean test validatePlugins build
```

HTTP 测试使用本地测试服务器，不会向 NexusMC 真实服务上传文件。
