<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Guía del Consumidor

> Plugin de Gradle basado en RAG para indexación de código aumentada por LLM y vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Pruebas)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licencia)](../LICENSE)

- **Versión**: `0.0.4` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.codebase`
- **Build**: `./gradlew build` · **Pruebas**: `./gradlew testAll` (JUnit5 + 48 suites Cucumber)
- **Cobertura**: ≥ 80 % (Kover, integrado en `check`)

🌐 Idiomas: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`codebase-gradle` indexa los archivos fuente de tu proyecto en **PostgreSQL + pgvector**, expone
aumento de contexto compuesto, anonimización, OCR (Gemini Vision), puertas de calidad
(sentimiento + PII + fuera de tema) y **vibecoding** — un bucle de generación de código
multi-turno impulsado por LLM mediante `koog-agents`.

Parte del ecosistema multi-plugin de CCCP Education:

```
intención del usuario → codex-gradle (indexación) → [codebase-gradle] → koog-agents → puertas de calidad → salida
```

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Indexar tu código

```bash
./gradlew collectFromCodebase          # contexto por distrito → build/context/
./gradlew collectCompositeContext       # compuesto a nivel workspace
```

### 3. Ejecutar vibecoding

```bash
./gradlew vibecode \
  --intention="Analizar la arquitectura" \
  --dryRun \
  --maxActions=10
```

Consulta [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) para las opciones completas.

## Tareas disponibles

| Tarea | Grupo | Descripción |
|------|-------|-------------|
| `collectFromCodebase`      | collect   | Contexto aumentado por distrito (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | Compuesto a nivel workspace desde todos los distritos |
| `generateCompositeContext` | generate  | Compuesto N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | Planificación aumentada — clasificar intención → EPICs/US/Tasks |
| `vibecode`                 | generate  | Bucle autónomo Koog (contexto → plan → ejecución → auditoría) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | Ingerir archivos EAGER (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | Resumen de sesión, costes de tokens, filtros de privacidad |
| `qualityGate`              | validate  | Comprobaciones de sentimiento + fuera de tema + PII residual |
| `ocrDocument`              | collect   | OCR vía Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | Ingerir salida OCR en pgvector |
| `exposeExperts`            | generate  | Manifiesto de expertos JSON para slider/plantuml/bakery |
| `endSessionBlog`           | generate  | Extraer sesiones → artículos de blog AsciiDoc |

## DSL de extensión

```gradle
codebaseOcr {
    ocrProvider   = "gemini"
    geminiModel   = "gemini-2.5-flash"
    ocrLanguage   = "fr"
    outputFormat  = "asciidoc"
    ocrEnabled    = false
    maxTokens     = 8192
}

codebaseExpert {
    domains             = []
    anonymizeEndpoints  = true
    outputFile          = "build/experts/exposure-manifest.json"
}

codebaseGovernance {
    strictValidation   = false
    outputEnabled      = true
    reportFormat       = "json"
    incremental        = false
    chunkIncremental   = false
}
```

## Requisitos previos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ con extensión `pgvector`
- **Docker** (para Testcontainers)

## Build y pruebas

```bash
./gradlew build                    # build completo
./gradlew testFast                 # Cucumber rápido (≤ 8 min)
./gradlew testAll                  # suite completa (JUnit5 + todo Cucumber)
./gradlew testEpics               # todo Cucumber BDD de EPICs
./gradlew testHelp                 # listar todas las tareas de prueba
./gradlew validateDependencies    # auditoría CVE + validación de dependencias
./gradlew publishToMavenLocal      # publicar localmente
```

## Solución de problemas

| Síntoma | Solución |
|---------|----------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Contenedor Postgres atascado | `docker rm -f postgres-*` y reintentar |
| Tiempo de espera en pruebas | verificar `docker ps`, aumentar heap, revisar latencia de la API LLM |

Consulta [BUILDING.md](../codebase-plugin/BUILDING.md) para más detalles.

## Licencia

Apache License 2.0 — consulta [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._