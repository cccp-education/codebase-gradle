<!-- master source — other languages are translations of this file -->
# codebase-gradle — Plugin Internals

> Developer & contributor guide for the `codebase-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=License)](../LICENSE)

- **Version**: `0.0.4` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.codebase`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew testAll` · **Coverage gate**: `./gradlew koverVerify` (≥80%)

🌐 Languages: **EN** | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Module layout

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # Plugin entry point — registers all tasks
    │   ├── benchmark/                  # LLM perception benchmark
    │   ├── blog/                       # Session → blog article dilution
    │   ├── koog/                       # koog DSL graphs (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # OCR via Gemini Vision + Ollama
    │   ├── quality/                    # ONNX quality gates (sentiment, PII, off-topic)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # File walker + anonymization
    └── vibecoding/                     # Vibecoding contracts (ToolRegistry, etc.)
```

## N0 contracts (from workspace-bom MEMPHIS)

| Contract | Artifact | Provides |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## N2 dependencies

- `codex-plugin` — document indexing (PDF/EPUB → pgvector)
- `planner-plugin` — LLM prompting SPG/SPD

## Key libraries

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — LLM providers, RAG, embeddings
- **R2DBC** — reactive PostgreSQL (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — YAML config, JSON serialization
- **Kover** 0.9.8 — coverage gate (≥80 %)

## Ollama instances (global constraint)

Ports `11434–11436` are forbidden. Rotate over `11437–11465` (29 ports).
Authorized models: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Test matrix

| Task | Scope | Timeout |
|------|-------|---------|
| `test` | JUnit5 unit tests | default |
| `testFast` | Quick Cucumber (≤ 8 min) | 8 min |
| `testAll` | JUnit5 + all Cucumber | — |
| `testEpics` | All EPIC Cucumber BDD | — |
| `cucumberTest*` (48 tasks) | One per EPIC | 8–15 min |

Verification tasks: `validateDependencies` (CVE-2015-7501 guard + heavy transitives), `koverVerify` (wired into `check`).

## JVM tuning

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, heap 2g
- **Local**: SerialGC + TieredStopAtLevel=4, heap adaptive (512m < 8 GB host, else 1g)
- **Metaspace**: 512m (both profiles)

## Build commands

```bash
./gradlew build                       # full build (compiles + tests)
./gradlew build -x test               # compile only
./gradlew testFast                    # fast tests (PR gate, ≤ 8 min)
./gradlew testAll                     # full test suite
./gradlew koverVerify                 # coverage ≥ 80 %
./gradlew validateDependencies        # CVE audit + heavy transitives
./gradlew publishToMavenLocal         # local publish
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI pipeline

`.github/workflows/test.yml` defines three jobs:
1. **fast-tests** — `./gradlew testFast -x build` on every PR (≤ 15 min)
2. **full-tests** — `./gradlew testAll` on main/master (≤ 45 min, uploads reports)
3. **publish** — `./gradlew publishAggregationToCentralPortal` on `v*` tags (needs GPG)

## Publication (NMCP)

Configured via `com.gradleup.nmcp.settings` (1.5.0) in `settings.gradle.kts`.
Credentials are read from `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`).
Signing uses `useGpgCmd()`; CI imports GPG key via `crazy-max/ghaction-import-gpg@v6`.
POM declares Apache 2.0, developer `cccp-education`, SCM pointing to
`github.com/cccp-education/codebase-gradle`.

## EPIC status

All EPICs closed in `0.0.4` (see `.agents/INDEX.adoc`):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL.

## Contributing

1. Build compiles: `./gradlew build -x test`
2. Fast tests green: `./gradlew testFast`
3. Coverage respected: `./gradlew koverVerify`
4. No CVE regression: `./gradlew validateDependencies`
5. Follow DDD conventions (value objects, ports/adapters, no leaks)

## Architecture docs

- [BUILDING.md](../codebase-plugin/BUILDING.md) — Build config & JVM tuning
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Ecosystem overview
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Local dev setup
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Usage modes
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs & governance

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._