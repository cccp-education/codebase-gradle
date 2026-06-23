<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Internes du Plugin

> Guide développeur et contributeur pour le plugin Gradle `codebase-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=couverture&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.4` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.codebase`
- **Toolchain** : Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build** : `./gradlew build -x test` · **Tests** : `./gradlew testAll` · **Gate couverture** : `./gradlew koverVerify` (≥80%)

🌐 Langues : [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | **Français** | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Organisation des modules

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # Point d'entrée du plugin — enregistre toutes les tâches
    │   ├── benchmark/                  # Benchmark de perception LLM
    │   ├── blog/                       # Session → dilution d'article de blog
    │   ├── koog/                       # Graphes DSL koog (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # OCR via Gemini Vision + Ollama
    │   ├── quality/                    # Portes qualité ONNX (sentiment, PII, hors-sujet)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # File walker + anonymisation
    └── vibecoding/                     # Contrats Vibecoding (ToolRegistry, etc.)
```

## Contrats N0 (depuis workspace-bom MEMPHIS)

| Contrat | Artefact | Fournit |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## Dépendances N2

- `codex-plugin` — indexation documentaire (PDF/EPUB → pgvector)
- `planner-plugin` — prompting LLM SPG/SPD

## Bibliothèques clés

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — fournisseurs LLM, RAG, embeddings
- **R2DBC** — PostgreSQL réactif (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — config YAML, sérialisation JSON
- **Kover** 0.9.8 — gate de couverture (≥80%)

## Instances Ollama (contrainte globale)

Les ports `11434–11436` sont interdits. Rotation sur `11437–11465` (29 ports).
Modèles autorisés : `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Matrice de tests

| Tâche | Portée | Timeout |
|------|-------|---------|
| `test` | Tests unitaires JUnit5 | défaut |
| `testFast` | Cucumber rapide (≤ 8 min) | 8 min |
| `testAll` | JUnit5 + tout Cucumber | — |
| `testEpics` | Tout Cucumber BDD EPICs | — |
| `cucumberTest*` (48 tâches) | Une par EPIC | 8–15 min |

Tâches de vérification : `validateDependencies` (garde CVE-2015-7501 + transitifs lourds), `koverVerify` (intégré à `check`).

## Réglage JVM

- **CI** : G1GC + ParallelRefProc + MaxGCPauseMillis=200, heap 2g
- **Local** : SerialGC + TieredStopAtLevel=4, heap adaptatif (hôte < 8 GB → 512m, sinon 1g)
- **Metaspace** : 512m (les deux profils)

## Commandes de build

```bash
./gradlew build                       # build complet (compile + teste)
./gradlew build -x test               # compile seulement
./gradlew testFast                    # tests rapides (gate PR, ≤ 8 min)
./gradlew testAll                     # suite complète
./gradlew koverVerify                 # couverture ≥ 80%
./gradlew validateDependencies        # audit CVE + transitifs lourds
./gradlew publishToMavenLocal         # publication locale
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Pipeline CI

`.github/workflows/test.yml` définit trois jobs :
1. **fast-tests** — `./gradlew testFast -x build` sur chaque PR (≤ 15 min)
2. **full-tests** — `./gradlew testAll` sur main/master (≤ 45 min, upload des rapports)
3. **publish** — `./gradlew publishAggregationToCentralPortal` sur tags `v*` (nécessite GPG)

## Publication (NMCP)

Configuré via `com.gradleup.nmcp.settings` (1.5.0) dans `settings.gradle.kts`.
Les identifiants sont lus depuis `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`).
La signature utilise `useGpgCmd()` ; le CI importe la clé GPG via `crazy-max/ghaction-import-gpg@v6`.
Le POM déclare Apache 2.0, développeur `cccp-education`, SCM pointant vers
`github.com/cccp-education/codebase-gradle`.

## Statut des EPICs

Tous les EPICs clôturés en `0.0.4` (voir `.agents/INDEX.adoc`) :
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL.

## Contribuer

1. Le build compile : `./gradlew build -x test`
2. Tests rapides verts : `./gradlew testFast`
3. Couverture respectée : `./gradlew koverVerify`
4. Pas de régression CVE : `./gradlew validateDependencies`
5. Suivre les conventions DDD (value objects, ports/adaptateurs, sans fuites)

## Docs d'architecture

- [BUILDING.md](../codebase-plugin/BUILDING.md) — Config build & réglage JVM
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Vue de l'écosystème
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Setup dev local
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Modes d'utilisation
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs & gouvernance

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._