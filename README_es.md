# NexusMCPublisher

[English](README.md) | [简体中文](README_zh.md) | [日本語](README_ja.md) | [Español](README_es.md) | [Deutsch](README_de.md)

Un plugin de Gradle compatible con **JDK 8** que publica versiones de recursos en NexusMC mediante el flujo de subida en dos pasos de la API personal:

1. Sube cada artefacto con `POST /api/upload`.
2. Publica la versión con `POST /api/resources/{id}/versions`, usando la URL, el nombre de archivo original, el tamaño y las sumas de comprobación devueltas por el endpoint de subida.

ID del plugin: `io.github.lijinhong11.nexusmcpublisher`

## Uso

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

    // Al aplicar el plugin de Java, se usa por defecto la salida de la tarea jar.
    // Para publicar otro artefacto:
    // artifact("build/libs/example.jar")
}
```

## Varios archivos

Cada bloque `file` se sube por separado. Después, los metadatos resultantes se envían juntos en el array `files[]` del endpoint de versiones. Debe haber exactamente un archivo marcado como principal.

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
        // primary es false de forma predeterminada
        gameVersions.set(listOf("1.21.1", "1.21.4"))
        subcategoryIds.set(listOf("fabric"))
    }
}
```

Si no se declara ningún bloque `file {}`, el plugin usa la propiedad `artifact` de nivel superior. Cuando se aplica el plugin de Java, esta propiedad usa por defecto la salida de la tarea `jar` y la trata como el único archivo principal.

## Autenticación

No incluyas el token de NexusMC en el script de compilación. Proporciónalo mediante una variable de entorno:

```bash
export NEXUSMC_API_TOKEN='avm_pa...'
./gradlew publishToNexusMC
```

También puedes añadirlo al archivo de usuario `~/.gradle/gradle.properties`. No confirmes este valor en el repositorio:

```properties
nexusMCToken=avm_pa...
```

El token necesita estos permisos:

- `upload:file`
- `resource:update:self`

## Configuración

| Propiedad | Obligatoria | Valor predeterminado | Descripción |
|---|---:|---|---|
| `resourceId` | Sí | — | ID del recurso de NexusMC |
| `token` | Sí | Variable de entorno o propiedad de Gradle | Token de la API personal |
| `version` | Sí | `project.version` | Versión que se publicará |
| `versionTag` | No | `VersionTag.RELEASE` | `RELEASE`, `BETA` o `ALPHA` |
| `versionTitle` | No | `Version <version>` | Título de la versión |
| `changelog` | No | — | Registro de cambios de la versión |
| `mcVersions` | No | Lista vacía | Versiones de Minecraft de la publicación |
| `subcategoryIds` | No | Lista vacía | ID de loader/subcategoría para publicaciones de un solo archivo |
| `downloadType` | No | `local` | Tipo de descarga de NexusMC |
| `artifact` | Sí | Salida de `jar` de Java | Artefacto único que se subirá |
| `file {}` | No | — | Declaración repetible para varios archivos; reemplaza `artifact` |
| `baseUrl` | No | `https://www.nexusmc.cn` | URL base de la API, principalmente para pruebas o despliegues privados |

## Compilación y pruebas

El proyecto se compila con JDK 8:

```bash
./gradlew clean test validatePlugins build
```

Las pruebas HTTP usan un servidor de pruebas local y nunca suben archivos al servicio real de NexusMC.
