# Building codebase-gradle

## Prerequisites

- Java 24+ (Kotlin 2.3.20 requires recent Java)
- PostgreSQL 15+ (for testcontainers)
- 8 GB RAM (or adjust heap sizes)
- Docker (for testcontainers pgvector)

## Quick Start

### Local Build

```bash
./gradlew build
```

### Publish to Local Maven Repository

```bash
./gradlew publishToMavenLocal
```

### Run Tests

```bash
# Fast tests (no containers): 5-10 min
./gradlew testFast

# All Epic tests: 20-30 min
./gradlew testEpics

# Complete test suite: 45+ min
./gradlew testAll

# JUnit5 unit tests only
./gradlew test

# List all test tasks
./gradlew testHelp
```

## Build Cache

Build cache is enabled by default (`org.gradle.caching=true`). First build takes ~45s, subsequent builds with no changes: ~1-2s.

```bash
# Disable cache (if debugging)
./gradlew build --no-build-cache
```

## Troubleshooting

### Out of Memory: "Java heap space"

Reduce heap size in local development:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew build
```

### PostgreSQL Container Won't Start

Container reuse is enabled. If it fails:

```bash
# Cleanup Docker resources
docker ps -a
docker rm -f postgres-*  # Remove stuck containers

# Retry
./gradlew testFast
```

### Test Timeout: "Gradle task timeout after N seconds"

Tests are configured with buffer timeouts. If you see timeouts:

1. Check PostgreSQL startup: `docker ps`
2. Check network latency to LLM API
3. Increase heap size (see OOM above)

## Performance Profiling

To profile a build:

```bash
./gradlew build --profile

# Open build/reports/profile/profile-TIMESTAMP.html
```

## Architecture

See `STRATEGIC_ROADMAP.adoc` for full ecosystem overview.

Key concepts:
- **codebase-gradle** (this repo): Core vibecoding orchestrator + RAG socle pgvector
- **codebase-plugin**: Gradle plugin — indexes project source files into pgvector, exposes composite context augmentation, anonymization, benchmark, STIMULUS cascade, vibecoding, OCR, agentic literature compiler
- **Protocol v1.0.0**: Immutable contract for I/O (opencode-session-contracts N0)
- **Reproducibility ID**: UUID for exact replay
- **Quality Gates**: Sentiment, PII, off-topic checks (EPIC 6 ONNX)

### Source Packages

```
src/main/kotlin/
├── codebase/
│   ├── CodebasePlugin.kt          # Plugin entry point
│   ├── benchmark/                 # LLM perception benchmark
│   ├── blog/                      # Blog article dilution
│   ├── koog/                      # koog DSL graphs (vibecoding, autofocus, agentic)
│   ├── ocr/                       # OCR via Gemini Vision + Ollama
│   ├── quality/                   # ONNX quality gates
│   ├── rag/                       # RAG pgvector (VectorStore, embeddings)
│   └── walker/                    # File walker + anonymization
└── vibecoding/                    # Vibecoding contracts (ToolRegistry, etc.)
```

### Test Tasks

| Task | Description | Timeout |
|------|-------------|---------|
| `test` | JUnit5 unit tests | default |
| `testAll` | JUnit5 + all Cucumber | — |
| `testEpics` | All EPIC Cucumber BDD | — |
| `testFast` | Fast Cucumber (≤8 min) | — |
| `testHelp` | List all test tasks | — |
| `cucumberTest` | EPIC 9 pgvector infra | 15 min |
| `cucumberTestEpicV6` | EPIC V-6 Feedback Loop | 15 min |
| `cucumberTestEpicV7` | EPIC V-7 Resume Session | 15 min |
| `cucumberTestEpicL3` | EPIC L-3 KoogAugmentedContextGraph | 15 min |
| `cucumberTestEpicV8` | EPIC V-8 DashboardTask | 15 min |
| `cucumberTestEpicVPool` | EPIC V-Pool Ollama Pool | 15 min |
| `cucumberTestEpicOcr` | EPIC OCR Gemini Vision | 8 min |
| `cucumberTestEpicOcrIngest` | EPIC OCR-4 Ingest | 15 min |
| `cucumberTestEpicOcr45` | EPIC OCR-4.5 Metrics | 8 min |
| `cucumberTestEpicY3` | EPIC Y-3 AgenticSchema | 15 min |
| `cucumberTestEpicY4` | EPIC Y-4 AgenticCompiler | 8 min |
| `cucumberTestEpicY5` | EPIC Y-5 AgenticIngestor | 15 min |
| `cucumberTestEpicY6` | EPIC Y-6 ExternalImporter | 15 min |
| `cucumberTestEpicY7` | EPIC Y-7 ChunkEnforcement | 15 min |
| `cucumberTestEpicW4` | EPIC W-4 List Tasks | 15 min |
| `cucumberTestEpicX1` | EPIC X-1 VibecodingPlan | 15 min |
| `cucumberTestEpicX2` | EPIC X-2 TaskResultVerifier | 15 min |
| `cucumberTestEpicX4` | EPIC X-4 RollbackStrategy | 15 min |
| `cucumberTestEpicX5` | EPIC X-5 Replan Catalog | 15 min |
| `cucumberTestEpicX6` | EPIC X-6 E2E Plan→Fail→Adapt | 15 min |
| `cucumberTestEpicSp1` | EPIC SP-1 SessionProtocol | 15 min |
| `cucumberTestEpicZ7` | EPIC Z-7 Cross-Borough Autofocus | 15 min |
| `cucumberTestEpic12` | EPIC 12 Blog Narration | 15 min |
| `cucumberTestEpicSp2` | EPIC SP-2 Server Daemon | 15 min |
| `cucumberTestEpicSp3` | EPIC SP-3 Session Lifecycle | 15 min |
| `cucumberTestEpicSp4` | EPIC SP-4 E2E opencode→runner | 15 min |
| `cucumberTestEpicSp5` | EPIC SP-5 ToolEventStream | 15 min |
| `cucumberTestEpicSp6` | EPIC SP-6 LiveContextInjector | 15 min |

### Verification Tasks

| Task | Description |
|------|-------------|
| `validateDependencies` | Validates annotations:13.0 resolution, CVE audit, heavy transitive report |
| `koverVerify` | Coverage threshold ≥80% (wired in `check`) |

### JVM Tuning

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200
- **Local**: SerialGC + TieredStopAtLevel=4
- **Metaspace**: 512m (both CI and local)
- **Cucumber tasks**: Adaptive heap (CI=2g, <8GB RAM=512m, else=1g)

## Dependencies

### N0 Contracts (workspace-bom MEMPHIS)

- `codebase-contracts` — ContextChannel, ChannelBudget, CompositeContext
- `agent-contracts` — Epic, UserStory, GradleTask, AgentState
- `vibecoding-contracts` — ToolRegistry, ExecShellTool, ExecGradleTool
- `llm-pool-contracts` — LlmInstancePool, LlmInstance, QuotaConfig
- `opencode-session-contracts` — SessionPrompt, SessionResponse, AgentContext

### N2 Dependencies

- `codex-plugin` — Document indexing (PDF/EPUB → pgvector)
- `planner-plugin` — LLM prompting SPG/SPD

### Key Libraries

- **koog-agents** 0.8.0 — DSL Kotlin StateGraph/ConditionalEdges
- **langchain4j** 1.14.1 — LLM providers, RAG, embeddings
- **R2DBC** — Reactive PostgreSQL (pgvector)
- **Testcontainers** — PostgreSQL pgvector/pgvector:pg17
- **Jackson** — YAML config, JSON serialization
- **Kotlin 2.3.20** — JDK 24 toolchain
