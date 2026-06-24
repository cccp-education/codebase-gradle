<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — پلگ ان کی اندرونی ساخت

> `codebase-plugin` Gradle پلگ ان کے لیے ڈویلپر اور Contributor گائیڈ۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=ٹیسٹ)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=کوریج&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=لائسنس)](../LICENSE)

- **ورژن**: `0.0.4` · **گروپ**: `education.cccp` · **پلگ ان آئی ڈی**: `education.cccp.codebase`
- **ٹول چین**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **بلڈ**: `./gradlew build -x test` · **ٹیسٹ**: `./gradlew testAll` · **کوریج گیٹ**: `./gradlew koverVerify` (≥80%)

🌐 زبانیں: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## ماڈیول کا ترتیب

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # پلگ ان کا داخلہ نقطہ — تمام ٹاسکس رجسٹر کرتا ہے
    │   ├── benchmark/                  # LLM پرسپشن بینچ مارک
    │   ├── blog/                       # سیشن ← بلاگ مضمون کا تخفیف
    │   ├── koog/                       # koog DSL گراف (vibecoding، autofocus، agentic)
    │   ├── ocr/                        # Gemini Vision + Ollama کے ذریعے OCR
    │   ├── quality/                    # ONNX کوالٹی گیٹ (جذبات، PII، موضوع سے باہر)
    │   ├── rag/                        # RAG pgvector (VectorStore، embeddings)
    │   └── walker/                     # فائل واکر + گمنامی
    └── vibecoding/                     # Vibecoding معاہدے (ToolRegistry، وغیرہ)
```

## N0 معاہدے (workspace-bom MEMPHIS سے)

| معاہدہ | آرٹیفیکٹ | فراہم کرتا ہے |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## N2 انحصارات

- `codex-plugin` — دستاویز انڈیکسنگ (PDF/EPUB → pgvector)
- `planner-plugin` — LLM prompting SPG/SPD

## اہم لائبریریاں

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — LLM فراہم کنندگان، RAG، embeddings
- **R2DBC** — reactive PostgreSQL (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — YAML config، JSON serialization
- **Kover** 0.9.8 — کوریج گیٹ (≥80%)

## Ollama مثالیں (عالمی پابندی)

پورٹس `11434–11436` ممنوعہ ہیں۔ `11437–11465` (29 پورٹس) پر rotation۔
اجازت یافتہ ماڈلز: `gpt-oss:120b-cloud`، `gemma4:31b-cloud`。

## ٹیسٹ میٹرکس

| ٹاسک | دائرہ کار | ٹائم آؤٹ |
|------|-------|---------|
| `test` | JUnit5 unit tests | default |
| `testFast` | تیز Cucumber (≤ 8 منٹ) | 8 منٹ |
| `testAll` | JUnit5 + تمام Cucumber | — |
| `testEpics` | تمام EPIC Cucumber BDD | — |
| `cucumberTest*` (48 ٹاسکس) | ہر EPIC کے لیے ایک | 8–15 منٹ |

تصدیقی ٹاسکس: `validateDependencies` (CVE-2015-7501 گارڈ + بھاری transitives)، `koverVerify` (`check` میں ضم)。

## JVM tuning

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200، heap 2g
- **مقامی**: SerialGC + TieredStopAtLevel=4، adaptive heap (host < 8 GB → 512m، ورنہ 1g)
- **Metaspace**: 512m (دونوں پروفائلز)

## بلڈ کمانڈز

```bash
./gradlew build                       # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew build -x test               # صرف کمپائل
./gradlew testFast                    # تیز ٹیسٹ (PR gate، ≤ 8 منٹ)
./gradlew testAll                     # مکمل سوٹ
./gradlew koverVerify                 # کوریج ≥ 80%
./gradlew validateDependencies        # CVE آڈیٹ + بھاری transitives
./gradlew publishToMavenLocal         # مقامی اشاعت
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI پائپ لائن

`.github/workflows/test.yml` تین jobs کی وضاحت کرتا ہے:
1. **fast-tests** — ہر PR پر `./gradlew testFast -x build` (≤ 15 منٹ)
2. **full-tests** — main/master پر `./gradlew testAll` (≤ 45 منٹ، رپورٹس اپلوڈ)
3. **publish** — `v*` tags پر `./gradlew publishAggregationToCentralPortal` (GPG ضروری)

## اشاعت (NMCP)

`settings.gradle.kts` میں `com.gradleup.nmcp.settings` (1.5.0) کے ذریعے configure کیا گیا۔
اسناد `~/.gradle/gradle.properties` (`ossrhUsername`، `ossrhPassword`) سے پڑھی جاتی ہیں۔
دستخط `useGpgCmd()` استعمال کرتا ہے؛ CI `crazy-max/ghaction-import-gpg@v6` کے ذریعے GPG key import کرتا ہے۔
POM Apache 2.0، developer `cccp-education`، SCM کا اعلان کرتا ہے جو
`github.com/cccp-education/codebase-gradle` کی طرف اشارہ کرتا ہے۔

## EPIC کی حیثیت

`0.0.4` میں تمام EPICs بند (`.agents/INDEX.adoc` دیکھیں):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL。

## تعاون

1. بلڈ کمپائل ہوتا ہے: `./gradlew build -x test`
2. تیز ٹیسٹ سبز: `./gradlew testFast`
3. کوریج کا احترام: `./gradlew koverVerify`
4. CVE regression نہیں: `./gradlew validateDependencies`
5. DDD conventions کی پیروی (value objects، ports/adapters، no leaks)

## آرکیٹیکچر docs

- [BUILDING.md](../codebase-plugin/BUILDING.md) — بلڈ config & JVM tuning
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Ecoystem overview
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Local dev setup
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Usage modes
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs & governance

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں。

---

_CCCP Education ecosystem کا حصہ — `groupId: education.cccp`۔_