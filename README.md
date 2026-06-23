# codebase-gradle

RAG-powered Gradle plugin for LLM-augmented codebase indexing and orchestration.

> Published to [Maven Central](https://central.sonatype.com/namespace/education.cccp) as `education.cccp:codebase-plugin`

## What This Is

**codebase-gradle** indexes project source files into PostgreSQL + pgvector,
exposes composite context augmentation, anonymization, benchmark, quality gates,
OCR (Gemini Vision), and **vibecoding** (LLM-orchestrated multi-turn code
generation via koog-agents).

Part of the CCCP Education multi-plugin ecosystem:

```
user intent → codex-gradle (indexing) → [codebase-gradle] → koog-agents → quality-gates → output
```

## Quick Start

### 1. Add Plugin to Your Project

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Index Your Codebase

```bash
./gradlew collectFromCodebase          # Per-borough context → build/context/
./gradlew collectCompositeContext       # Workspace-level composite
```

### 3. Run Vibecoding

```bash
./gradlew vibecode \
  --intention="Analyze the architecture" \
  --dryRun \
  --maxActions=10
```

See [VIBECODING_USAGE_GUIDE.adoc](./VIBECODING_USAGE_GUIDE.adoc) for complete options.

## Tasks

| Task | Group | Description |
|------|-------|-------------|
| `collectFromCodebase` | collect | Collect per-borough augmented context (EAGER/RAG/Graphify) |
| `collectCompositeContext` | collect | Assemble workspace-level composite from all boroughs |
| `generateCompositeContext` | generate | Composite context N1+N2 (codex + training) → JSON |
| `generatePlan` | generate | Augmented planning — classify intention → EPICs/UserStories/Tasks |
| `vibecode` | generate | Koog autonomous loop (context → plan → execute → audit) |
| `sessionProtocolDaemon` | generate | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance` | generate | Ingest EAGER files (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard` | tracking | Session summary, token costs, privacy filters |
| `qualityGate` | validate | Sentiment + off-topic + PII residual checks |
| `ocrDocument` | collect | OCR via Gemini Vision → AsciiDoc |
| `ocrIngest` | collect | Ingest OCR output into pgvector (chunk → embed → RAG) |
| `exposeExperts` | generate | Expert manifest JSON for slider/plantuml/bakery |
| `endSessionBlog` | generate | Extract sessions → AsciiDoc blog articles |

## Extensions

```gradle
codebaseOcr {
    ocrProvider = "gemini"
    geminiModel = "gemini-2.5-flash"
    ocrLanguage = "fr"
    outputFormat = "asciidoc"
    ocrEnabled = false
    maxTokens = 8192
}

codebaseExpert {
    domains = []
    anonymizeEndpoints = true
    outputFile = "build/experts/exposure-manifest.json"
}

codebaseGovernance {
    strictValidation = false
    outputEnabled = true
    reportFormat = "json"
    incremental = false
    chunkIncremental = false
}
```

## Prerequisites

- Java 24+
- Gradle 9.5.1+
- PostgreSQL 15+ with pgvector extension
- Docker (for Testcontainers tests)

## Building Locally

```bash
./gradlew build                    # Full build
./gradlew testFast                 # Quick Cucumber tests (timeout <= 8 min)
./gradlew testAll                  # Full test suite (JUnit5 + all Cucumber)
./gradlew testEpics                # All EPIC Cucumber BDD tests
./gradlew testHelp                 # List all test tasks
./gradlew validateDependencies     # CVE audit + dependency validation
./gradlew publishToMavenLocal      # Publish locally
```

See [BUILDING.md](./codebase-plugin/BUILDING.md) for detailed build guide.

## Architecture

```
codebase/
├── rag/        pgvector integration, RAG indexing, composite context
├── vibecoding/ Orchestration, tool registry, planning layer
├── ocr/        Gemini Vision + Ollama integration
├── quality/    ONNX-based quality gates (sentiment, PII, off-topic)
├── koog/       Agentic DSL graphs, session protocol, governance
├── walker/     Source code walking, anonymization
├── benchmark/  Anonymization benchmark
├── blog/       Session → blog article generation
└── i18n/       TranslationService cross-borough
```

See [STRATEGIC_ROADMAP.adoc](./codebase-plugin/STRATEGIC_ROADMAP.adoc) for ecosystem overview.

## Documentation

- [BUILDING.md](./codebase-plugin/BUILDING.md) — Build configuration & tasks
- [LOCAL_VIBECODING_LOOP.adoc](./codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Local development setup
- [VIBECODING_USAGE_GUIDE.adoc](./VIBECODING_USAGE_GUIDE.adoc) — Usage modes (with/without runner)
- [STRATEGIC_ROADMAP.adoc](./codebase-plugin/STRATEGIC_ROADMAP.adoc) — Full ecosystem architecture
- [AGENT.adoc](./codebase-plugin/AGENT.adoc) — Agent instructions & protocols

## Test Coverage

48 Cucumber BDD test suites covering EPICs V-6, V-7, V-8, V-9.x, Y-3–Y-7,
W-4, X-1, X-2, X-4, X-5, X-6, SP-1–SP-6, Z-7, L-3, OCR, Translation, and more.

## Publishing to Maven Central

Publication is handled via NMCP (`com.gradleup.nmcp`) configured in
`settings.gradle.kts`. Credentials are read from `~/.gradle/gradle.properties`.

```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## License

Apache License 2.0 — See [LICENSE](./LICENSE)