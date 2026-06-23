<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — প্লাগইন অভ্যন্তর

> `codebase-plugin` Gradle প্লাগইনের জন্য ডেভেলপার ও অবদানকারী গাইড।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=পরীক্ষা)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=কভারেজ&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=লাইসেন্স)](../LICENSE)

- **সংস্করণ**: `0.0.4` · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.codebase`
- **টুলচেইন**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **বিল্ড**: `./gradlew build -x test` · **পরীক্ষা**: `./gradlew testAll` · **কভারেজ গেট**: `./gradlew koverVerify` (≥80%)

🌐 ভাষা: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## মডিউল বিন্যাস

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # প্লাগইন এন্ট্রি পয়েন্ট — সকল কাজ নিবন্ধন করে
    │   ├── benchmark/                  # LLM পারসেপশন বেঞ্চমার্ক
    │   ├── blog/                       # সেশন ← ব্লগ নিবন্ধ লঘুকরণ
    │   ├── koog/                       # koog DSL গ্রাফ (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # Gemini Vision + Ollama দ্বারা OCR
    │   ├── quality/                    # ONNX কোয়ালিটি গেট (আবেগ, PII, বিষয়বহির্ভূত)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # ফাইল ওয়াকার + অনামিককরণ
    └── vibecoding/                     # Vibecoding চুক্তি (ToolRegistry, ইত্যাদি)
```

## N0 চুক্তি (workspace-bom MEMPHIS থেকে)

| চুক্তি | আর্টিফ্যাক্ট | প্রদান করে |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## N2 নির্ভরতা

- `codex-plugin` — নথি ইনডেক্সিং (PDF/EPUB → pgvector)
- `planner-plugin` — LLM prompting SPG/SPD

## মূল লাইব্রেরি

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — LLM প্রোভাইডার, RAG, embeddings
- **R2DBC** — প্রতিক্রিয়াশীল PostgreSQL (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — YAML কনফিগ, JSON সিরিয়ালাইজেশন
- **Kover** 0.9.8 — কভারেজ গেট (≥80%)

## Ollama ইনস্ট্যান্স (বৈশ্বিক সীমাবদ্ধতা)

পোর্ট `11434–11436` নিষিদ্ধ। `11437–11465` (29 পোর্ট) এ রোটেশন।
অনুমোদিত মডেল: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।

## পরীক্ষা ম্যাট্রিক্স

| কাজ | পরিধি | টাইমআউট |
|------|-------|---------|
| `test` | JUnit5 ইউনিট পরীক্ষা | ডিফল্ট |
| `testFast` | দ্রুত Cucumber (≤ ৮ মিনিট) | ৮ মিনিট |
| `testAll` | JUnit5 + সব Cucumber | — |
| `testEpics` | সব EPIC Cucumber BDD | — |
| `cucumberTest*` (৪৮ কাজ) | প্রতি EPIC এর জন্য একটি | ৮–১৫ মিনিট |

যাচাই কাজ: `validateDependencies` (CVE-2015-7501 রক্ষা + ভারী ট্রানজিটিভ), `koverVerify` (`check` এ একত্রিত)।

## JVM টিউনিং

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, হিপ 2g
- **স্থানীয়**: SerialGC + TieredStopAtLevel=4, অভিয়োজনশীল হিপ (হোস্ট < 8 GB → 512m, অন্যথায় 1g)
- **Metaspace**: 512m (উভয় প্রোফাইল)

## বিল্ড কমান্ড

```bash
./gradlew build                       # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew build -x test               # শুধু কম্পাইল
./gradlew testFast                    # দ্রুত পরীক্ষা (PR গেট, ≤ ৮ মিনিট)
./gradlew testAll                     # সম্পূর্ণ স্যুট
./gradlew koverVerify                 # কভারেজ ≥ 80%
./gradlew validateDependencies        # CVE অডিট + ভারী ট্রানজিটিভ
./gradlew publishToMavenLocal         # স্থানীয় প্রকাশনা
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI পাইপলাইন

`.github/workflows/test.yml` তিনটি কাজ সংজ্ঞায়িত করে:
1. **fast-tests** — প্রতিটি PR এ `./gradlew testFast -x build` (≤ ১৫ মিনিট)
2. **full-tests** — main/master এ `./gradlew testAll` (≤ ৪৫ মিনিট, রিপোর্ট আপলোড)
3. **publish** — `v*` ট্যাগে `./gradlew publishAggregationToCentralPortal` (GPG প্রয়োজন)

## প্রকাশনা (NMCP)

`settings.gradle.kts` এ `com.gradleup.nmcp.settings` (1.5.0) এর মাধ্যমে কনফিগার করা।
শংসাপত্র `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`) থেকে পড়া হয়।
স্বাক্ষর `useGpgCmd()` ব্যবহার করে; CI `crazy-max/ghaction-import-gpg@v6` দ্বারা GPG কী আমদানি করে।
POM Apache 2.0, ডেভেলপার `cccp-education`, SCM ঘোষণা করে যা
`github.com/cccp-education/codebase-gradle` এ নির্দেশ করে।

## EPIC অবস্থা

`0.0.4` এ সব EPIC বন্ধ (`.agents/INDEX.adoc` দেখুন):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL।

## অবদান

1. বিল্ড কম্পাইল হয়: `./gradlew build -x test`
2. দ্রুত পরীক্ষা সবুজ: `./gradlew testFast`
3. কভারেজ সম্মানিত: `./gradlew koverVerify`
4. CVE প্রত্যাবর্তন নেই: `./gradlew validateDependencies`
5. DDD রীতি অনুসরণ (ভ্যালু অবজেক্ট, পোর্ট/অ্যাডাপ্টার, লিক নেই)

## আর্কিটেকচার ডকস

- [BUILDING.md](../codebase-plugin/BUILDING.md) — বিল্ড কনফিগ ও JVM টিউনিং
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — ইকোসিস্টেম ওভারভিউ
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — লোকাল ডেভ সেটআপ
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — ব্যবহার মোড
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC ও গভর্নেন্স

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_