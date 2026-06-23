<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Internos del Plugin

> Guía para desarrolladores y colaboradores del plugin Gradle `codebase-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Pruebas)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=Cobertura&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licencia)](../LICENSE)

- **Versión**: `0.0.4` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.codebase`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build -x test` · **Pruebas**: `./gradlew testAll` · **Gate cobertura**: `./gradlew koverVerify` (≥80%)

🌐 Idiomas: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Estructura de módulos

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # Punto de entrada del plugin — registra todas las tareas
    │   ├── benchmark/                  # Benchmark de percepción LLM
    │   ├── blog/                       # Sesión → dilución de artículo de blog
    │   ├── koog/                       # Grafos DSL koog (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # OCR vía Gemini Vision + Ollama
    │   ├── quality/                    # Puertas de calidad ONNX (sentimiento, PII, off-topic)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # File walker + anonimización
    └── vibecoding/                     # Contratos Vibecoding (ToolRegistry, etc.)
```

## Contratos N0 (de workspace-bom MEMPHIS)

| Contrato | Artefacto | Proporciona |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## Dependencias N2

- `codex-plugin` — indexación de documentos (PDF/EPUB → pgvector)
- `planner-plugin` — prompting LLM SPG/SPD

## Librerías clave

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — proveedores LLM, RAG, embeddings
- **R2DBC** — PostgreSQL reactivo (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — config YAML, serialización JSON
- **Kover** 0.9.8 — gate de cobertura (≥80%)

## Instancias Ollama (restricción global)

Los puertos `11434–11436` están prohibidos. Rotar sobre `11437–11465` (29 puertos).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Matriz de pruebas

| Tarea | Alcance | Timeout |
|------|-------|---------|
| `test` | Pruebas unitarias JUnit5 | por defecto |
| `testFast` | Cucumber rápido (≤ 8 min) | 8 min |
| `testAll` | JUnit5 + todo Cucumber | — |
| `testEpics` | Todo Cucumber BDD de EPICs | — |
| `cucumberTest*` (48 tareas) | Una por EPIC | 8–15 min |

Tareas de verificación: `validateDependencies` (guardia CVE-2015-7501 + transitivos pesados), `koverVerify` (integrado en `check`).

## Ajuste de JVM

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, heap 2g
- **Local**: SerialGC + TieredStopAtLevel=4, heap adaptativo (host < 8 GB → 512m, si no 1g)
- **Metaspace**: 512m (ambos perfiles)

## Comandos de build

```bash
./gradlew build                       # build completo (compila + prueba)
./gradlew build -x test               # solo compilar
./gradlew testFast                    # pruebas rápidas (gate PR, ≤ 8 min)
./gradlew testAll                     # suite completa
./gradlew koverVerify                 # cobertura ≥ 80%
./gradlew validateDependencies        # auditoría CVE + transitivos pesados
./gradlew publishToMavenLocal         # publicación local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Pipeline CI

`.github/workflows/test.yml` define tres jobs:
1. **fast-tests** — `./gradlew testFast -x build` en cada PR (≤ 15 min)
2. **full-tests** — `./gradlew testAll` en main/master (≤ 45 min, sube reportes)
3. **publish** — `./gradlew publishAggregationToCentralPortal` en tags `v*` (requiere GPG)

## Publicación (NMCP)

Configurado vía `com.gradleup.nmcp.settings` (1.5.0) en `settings.gradle.kts`.
Las credenciales se leen de `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`).
El firmado usa `useGpgCmd()`; CI importa la clave GPG vía `crazy-max/ghaction-import-gpg@v6`.
El POM declara Apache 2.0, desarrollador `cccp-education`, SCM apuntando a
`github.com/cccp-education/codebase-gradle`.

## Estado de EPICs

Todos los EPICs cerrados en `0.0.4` (ver `.agents/INDEX.adoc`):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL.

## Contribuir

1. El build compila: `./gradlew build -x test`
2. Pruebas rápidas en verde: `./gradlew testFast`
3. Cobertura respetada: `./gradlew koverVerify`
4. Sin regresión CVE: `./gradlew validateDependencies`
5. Seguir convenciones DDD (value objects, ports/adapters, sin leaks)

## Docs de arquitectura

- [BUILDING.md](../codebase-plugin/BUILDING.md) — Config build y ajuste JVM
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Vista del ecosistema
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Setup dev local
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Modos de uso
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs y gobernanza

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._