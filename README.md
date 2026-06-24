<!-- master source — other languages are translations of this file -->
# codebase-gradle — Consumer Guide

> RAG-powered Gradle plugin for LLM-augmented codebase indexing & vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=License)](../LICENSE)

- **Version**: `0.0.4` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.codebase`
- **Build**: `./gradlew build` · **Tests**: `./gradlew testAll` (JUnit5 + 48 Cucumber suites)
- **Coverage**: ≥ 80 % (Kover, wired into `check`)

🌐 Languages: **EN** | [中文](README.consommateurs/README.zh.md) | [हिन्दी](README.consommateurs/README.hi.md) | [Español](README.consommateurs/README.es.md) | [Français](README.consommateurs/README.fr.md) | [العربية](README.consommateurs/README.ar.md) | [বাংলা](README.consommateurs/README.bn.md) | [Português](README.consommateurs/README.pt.md) | [Русский](README.consommateurs/README.ru.md) | [اردو](README.consommateurs/README.ur.md)

---

## What it does

`codebase-gradle` indexes your project sources into **PostgreSQL + pgvector**, exposes
composite context augmentation, anonymization, OCR (Gemini Vision), quality gates
(sentiment + PII + off-topic), and **vibecoding** — an LLM-orchestrated multi-turn
code generation loop powered by `koog-agents`.

Part of the CCCP Education multi-plugin ecosystem:

```
user intent → codex-gradle (indexing) → [codebase-gradle] → koog-agents → quality-gates → output
```

## Quick Start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Index your codebase

```bash
./gradlew collectFromCodebase          # per-borough context → build/context/
./gradlew collectCompositeContext       # workspace-level composite
```

### 3. Run vibecoding

```bash
./gradlew vibecode \
  --intention="Analyze the architecture" \
  --dryRun \
  --maxActions=10
```

See [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) for full options.

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `collectFromCodebase`      | collect   | Per-borough augmented context (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | Workspace-level composite from all boroughs |
| `generateCompositeContext` | generate  | Composite N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | Augmented planning — classify intention → EPICs/US/Tasks |
| `vibecode`                 | generate  | Koog autonomous loop (context → plan → execute → audit) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | Ingest EAGER files (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | Session summary, token costs, privacy filters |
| `qualityGate`              | validate  | Sentiment + off-topic + PII residual checks |
| `ocrDocument`              | collect   | OCR via Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | Ingest OCR output into pgvector |
| `exposeExperts`            | generate  | Expert manifest JSON for slider/plantuml/bakery |
| `endSessionBlog`           | generate  | Extract sessions → AsciiDoc blog articles |

## Extension DSL

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

## Prerequisites

- **Java** 24+ (Kotlin 2.3.20 toolchain)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ with `pgvector` extension
- **Docker** (for Testcontainers)

## Build & test

```bash
./gradlew build                    # full build
./gradlew testFast                 # quick Cucumber (≤ 8 min)
./gradlew testAll                  # full suite (JUnit5 + all Cucumber)
./gradlew testEpics               # all EPIC Cucumber BDD
./gradlew testHelp                 # list all test tasks
./gradlew validateDependencies    # CVE audit + dependency validation
./gradlew publishToMavenLocal      # publish locally
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Postgres container stuck | `docker rm -f postgres-*` then retry |
| Test timeout             | check `docker ps`, increase heap, check LLM API latency |

See [BUILDING.md](../codebase-plugin/BUILDING.md) for details.

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._