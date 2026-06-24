<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Internos do Plugin

> Guia para desenvolvedores e contribuidores do plugin Gradle `codebase-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Testes)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=Cobertura&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licença)](../LICENSE)

- **Versão**: `0.0.4` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.codebase`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build -x test` · **Testes**: `./gradlew testAll` · **Gate cobertura**: `./gradlew koverVerify` (≥80%)

🌐 Idiomas: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Layout de módulos

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # Ponto de entrada do plugin — registra todas as tarefas
    │   ├── benchmark/                  # Benchmark de percepção LLM
    │   ├── blog/                       # Sessão → diluição de artigo de blog
    │   ├── koog/                       # Grafos DSL koog (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # OCR via Gemini Vision + Ollama
    │   ├── quality/                    # Portões de qualidade ONNX (sentimento, PII, off-topic)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # File walker + anonimização
    └── vibecoding/                     # Contratos Vibecoding (ToolRegistry, etc.)
```

## Contratos N0 (de workspace-bom MEMPHIS)

| Contrato | Artefato | Fornece |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## Dependências N2

- `codex-plugin` — indexação de documentos (PDF/EPUB → pgvector)
- `planner-plugin` — prompting LLM SPG/SPD

## Bibliotecas principais

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — provedores LLM, RAG, embeddings
- **R2DBC** — PostgreSQL reativo (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — config YAML, serialização JSON
- **Kover** 0.9.8 — gate de cobertura (≥80%)

## Instâncias Ollama (restrição global)

Portas `11434–11436` são proibidas. Rotacionar sobre `11437–11465` (29 portas).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Matriz de testes

| Tarefa | Escopo | Timeout |
|------|-------|---------|
| `test` | Testes unitários JUnit5 | padrão |
| `testFast` | Cucumber rápido (≤ 8 min) | 8 min |
| `testAll` | JUnit5 + todo Cucumber | — |
| `testEpics` | Todo Cucumber BDD de EPICs | — |
| `cucumberTest*` (48 tarefas) | Um por EPIC | 8–15 min |

Tarefas de verificação: `validateDependencies` (guarda CVE-2015-7501 + transitivos pesados), `koverVerify` (integrado em `check`).

## Ajuste de JVM

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, heap 2g
- **Local**: SerialGC + TieredStopAtLevel=4, heap adaptativo (host < 8 GB → 512m, senão 1g)
- **Metaspace**: 512m (ambos perfis)

## Comandos de build

```bash
./gradlew build                       # build completo (compila + testa)
./gradlew build -x test               # apenas compilar
./gradlew testFast                    # testes rápidos (gate PR, ≤ 8 min)
./gradlew testAll                     # suíte completa
./gradlew koverVerify                 # cobertura ≥ 80%
./gradlew validateDependencies        # auditoria CVE + transitivos pesados
./gradlew publishToMavenLocal         # publicação local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Pipeline CI

`.github/workflows/test.yml` define três jobs:
1. **fast-tests** — `./gradlew testFast -x build` em cada PR (≤ 15 min)
2. **full-tests** — `./gradlew testAll` em main/master (≤ 45 min, sobe relatórios)
3. **publish** — `./gradlew publishAggregationToCentralPortal` em tags `v*` (requer GPG)

## Publicação (NMCP)

Configurado via `com.gradleup.nmcp.settings` (1.5.0) em `settings.gradle.kts`.
Credenciais são lidas de `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`).
Assinatura usa `useGpgCmd()`; CI importa chave GPG via `crazy-max/ghaction-import-gpg@v6`.
POM declara Apache 2.0, desenvolvedor `cccp-education`, SCM apontando para
`github.com/cccp-education/codebase-gradle`.

## Status dos EPICs

Todos os EPICs fechados em `0.0.4` (ver `.agents/INDEX.adoc`):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL.

## Contribuir

1. O build compila: `./gradlew build -x test`
2. Testes rápidos verdes: `./gradlew testFast`
3. Cobertura respeitada: `./gradlew koverVerify`
4. Sem regressão CVE: `./gradlew validateDependencies`
5. Seguir convenções DDD (value objects, ports/adapters, sem leaks)

## Docs de arquitetura

- [BUILDING.md](../codebase-plugin/BUILDING.md) — Config build & ajuste JVM
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Visão do ecossistema
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Setup dev local
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Modos de uso
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs & governança

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._