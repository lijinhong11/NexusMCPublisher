# NexusMCPublisher

[English](README.md) | [简体中文](README_zh.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md)

Ein mit **JDK 8** kompatibles Gradle-Plugin, das Ressourcenversionen über den zweistufigen Upload-Ablauf der persönlichen NexusMC-API veröffentlicht:

1. Jedes Artefakt wird mit `POST /api/upload` hochgeladen.
2. Die Version wird mit `POST /api/resources/{id}/versions` veröffentlicht. Dabei werden die vom Upload-Endpunkt zurückgegebenen Werte für URL, ursprünglichen Dateinamen, Größe und Prüfsummen verwendet.

Plugin-ID: `io.github.lijinhong11.nexusmc-publisher`

## Verwendung

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
    subcategoryIds.set(listOf("paper"))

    // Wenn das Java-Plugin angewendet wird, wird standardmäßig die Ausgabe der jar-Task verwendet.
    // So kann ein anderes Artefakt veröffentlicht werden:
    // artifact("build/libs/example.jar")
}
```

## Mehrere Dateien

Jeder `file`-Block wird separat hochgeladen. Anschließend werden die resultierenden Metadaten gemeinsam im Array `files[]` an den Versionsendpunkt übermittelt. Genau eine Datei muss als Primärdatei markiert sein.

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
        subcategoryIds.set(listOf("paper"))
    }

    file {
        artifact("build/libs/plugin-fabric.jar")
        // primary ist standardmäßig false
        gameVersions.set(listOf("1.21.1", "1.21.4"))
        subcategoryIds.set(listOf("fabric"))
    }
}
```

Wenn keine `file {}`-Blöcke deklariert werden, verwendet das Plugin die übergeordnete Einstellung `artifact`. Bei aktiviertem Java-Plugin verweist diese Einstellung standardmäßig auf die Ausgabe der `jar`-Task, die als einzige Primärdatei behandelt wird.

## Authentifizierung

Das NexusMC-Token sollte nicht im Build-Skript stehen. Es kann über eine Umgebungsvariable bereitgestellt werden:

```bash
export NEXUSMC_API_TOKEN='avm_pa...'
./gradlew publishToNexusMC
```

Alternativ kann es in der benutzerspezifischen Datei `~/.gradle/gradle.properties` gespeichert werden. Dieser Wert darf nicht in das Repository übernommen werden:

```properties
nexusMCToken=avm_pa...
```

Das Token benötigt folgende Berechtigungen:

- `upload:file`
- `resource:update:self`

## Konfiguration

| Eigenschaft | Erforderlich | Standardwert | Beschreibung |
|---|---:|---|---|
| `resourceId` | Ja | — | NexusMC-Ressourcen-ID |
| `token` | Ja | Umgebungsvariable oder Gradle-Eigenschaft | Persönliches API-Token |
| `version` | Ja | `project.version` | Zu veröffentlichende Version |
| `versionTag` | Nein | `VersionTag.RELEASE` | `RELEASE`, `BETA` oder `ALPHA` |
| `versionTitle` | Nein | `Version <version>` | Titel der Version |
| `changelog` | Nein | — | Änderungsprotokoll der Version |
| `mcVersions` | Nein | Leere Liste | Minecraft-Versionen der Veröffentlichung |
| `subcategoryIds` | Nein | Leere Liste | Loader-/Unterkategorie-IDs für die Einzeldatei-Veröffentlichung |
| `downloadType` | Nein | `local` | NexusMC-Downloadtyp |
| `artifact` | Ja | Ausgabe der Java-`jar`-Task | Einzelnes hochzuladendes Artefakt |
| `file {}` | Nein | — | Wiederholbare Mehrdatei-Deklaration; überschreibt `artifact` |
| `baseUrl` | Nein | `https://www.nexusmc.cn` | Basis-URL der API, hauptsächlich für Tests oder private Bereitstellungen |

## Erstellen und Testen

Das Projekt wird mit JDK 8 erstellt:

```bash
./gradlew clean test validatePlugins build
```

Die HTTP-Tests verwenden einen lokalen Testserver und laden niemals Dateien zum echten NexusMC-Dienst hoch.

## Veröffentlichung im Gradle Plugin Portal

1. Im [Gradle Plugin Portal](https://plugins.gradle.org/) registrieren und einen API-Schlüssel erstellen.
2. Die Zugangsdaten außerhalb des Repositorys speichern. Umgebungsvariablen werden empfohlen:

   ```bash
   export GRADLE_PUBLISH_KEY='your-portal-key'
   export GRADLE_PUBLISH_SECRET='your-portal-secret'
   ```

   Alternativ kann die benutzerspezifische Datei `~/.gradle/gradle.properties` verwendet werden:

   ```properties
   gradle.publish.key=your-portal-key
   gradle.publish.secret=your-portal-secret
   ```

3. Die Veröffentlichung lokal prüfen, ohne etwas hochzuladen:

   ```bash
   ./gradlew clean test validatePlugins build
   ```

4. Eine Projektversion ohne SNAPSHOT setzen und das Plugin veröffentlichen:

   ```bash
   ./gradlew publishPlugins
   ```

Die erste Veröffentlichung wird vom Gradle Plugin Portal manuell geprüft. Jede spätere Veröffentlichung muss eine bisher noch nicht veröffentlichte Versionsnummer verwenden.
