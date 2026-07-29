# NexusMCPublisher

[English](README.md) | [简体中文](README_zh.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md)

NexusMC Personal API の2段階アップロードフローを使用してリソースのバージョンを公開する、**JDK 8** 対応の Gradle プラグインです。

1. `POST /api/upload` で各成果物をアップロードします。
2. アップロードAPIから返された URL、元のファイル名、サイズ、チェックサムを使用し、`POST /api/resources/{id}/versions` でバージョンを公開します。

プラグインID：`io.github.lijinhong11.nexusmc-publisher`

## 使用方法

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

    // Java プラグインが適用されている場合、jar タスクの出力がデフォルトで使用されます。
    // 別の成果物を公開する場合：
    // artifact("build/libs/example.jar")
}
```

## 複数ファイル

各 `file` ブロックは個別にアップロードされ、そのメタデータはバージョンAPIの `files[]` 配列にまとめて送信されます。プライマリファイルは必ず1つだけ指定してください。

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
        // primary のデフォルト値は false です
        gameVersions.set(listOf("1.21.1", "1.21.4"))
        loaders.set(listOf("fabric"))
    }
}
```

`file {}` ブロックがない場合、トップレベルの `artifact` 設定が使用されます。Java プラグインが適用されている場合は `jar` タスクの出力がデフォルトとなり、唯一のプライマリファイルとして扱われます。

## 認証

NexusMC トークンをビルドスクリプトに記述しないでください。環境変数で指定します。

```bash
export NEXUSMC_API_TOKEN='avm_pa...'
./gradlew publishToNexusMC
```

または、ユーザー単位の `~/.gradle/gradle.properties` に追加できます。この値をコミットしないでください。

```properties
nexusMCToken=avm_pa...
```

トークンには次の権限が必要です。

- `upload:file`
- `resource:update:self`

## 設定項目

| プロパティ | 必須 | デフォルト | 説明 |
|---|---:|---|---|
| `resourceId` | はい | — | NexusMC リソースID |
| `token` | はい | 環境変数または Gradle プロパティ | Personal API トークン |
| `version` | はい | `project.version` | 公開するバージョン |
| `versionTag` | いいえ | `VersionTag.RELEASE` | `RELEASE`、`BETA`、または `ALPHA` |
| `versionTitle` | いいえ | `Version <version>` | バージョンタイトル |
| `changelog` | いいえ | — | 変更履歴 |
| `mcVersions` | いいえ | 空のリスト | 対応する Minecraft バージョン |
| `downloadType` | いいえ | `local` | NexusMC のダウンロード形式 |
| `artifact` | はい | Java の `jar` 出力 | 単一ファイル公開時の成果物 |
| `file {}` | いいえ | — | 複数回指定可能なファイル設定。トップレベルの `artifact` より優先されます |
| `baseUrl` | いいえ | `https://www.nexusmc.cn` | API のベースURL。主にテストやプライベート環境向けです |

## ビルドとテスト

このプロジェクトは JDK 8 でビルドされます。

```bash
./gradlew clean test validatePlugins build
```

HTTP テストではローカルテストサーバーを使用し、実際の NexusMC サービスへファイルをアップロードしません。

## Gradle Plugin Portal への公開

1. [Gradle Plugin Portal](https://plugins.gradle.org/) に登録し、APIキーを作成します。
2. 認証情報はリポジトリ外で管理してください。環境変数の使用を推奨します。

   ```bash
   export GRADLE_PUBLISH_KEY='your-portal-key'
   export GRADLE_PUBLISH_SECRET='your-portal-secret'
   ```

   または、ユーザー単位の `~/.gradle/gradle.properties` を使用できます。

   ```properties
   gradle.publish.key=your-portal-key
   gradle.publish.secret=your-portal-secret
   ```

3. アップロードせずにローカルでリリースを検証します。

   ```bash
   ./gradlew clean test validatePlugins build
   ```

4. SNAPSHOT ではないプロジェクトバージョンを設定して公開します。

   ```bash
   ./gradlew publishPlugins
   ```

初回リリースは Gradle Plugin Portal による手動審査の対象です。それ以降も、公開済みではない新しいバージョン番号を使用する必要があります。
