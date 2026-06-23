<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — प्लगइन आंतरिक

> `codebase-plugin` Gradle प्लगइन के लिए डेवलपर और योगदानकर्ता गाइड।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=परीक्षण)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=कवरेज&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=लाइसेंस)](../LICENSE)

- **संस्करण**: `0.0.4` · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.codebase`
- **टूलचेन**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **बिल्ड**: `./gradlew build -x test` · **परीक्षण**: `./gradlew testAll` · **कवरेज गेट**: `./gradlew koverVerify` (≥80%)

🌐 भाषाएँ: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## मॉड्यूल लेआउट

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # प्लगइन प्रवेश बिंदु — सभी कार्य पंजीकृत करता है
    │   ├── benchmark/                  # LLM धारणा बेंचमार्क
    │   ├── blog/                       # सत्र → ब्लॉग लेख पतन
    │   ├── koog/                       # koog DSL ग्राफ (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # Gemini Vision + Ollama द्वारा OCR
    │   ├── quality/                    # ONNX कोयालिटी गेट (भावना, PII, विषय से बाहर)
    │   ├── rag/                        # RAG pgvector (VectorStore, embeddings)
    │   └── walker/                     # फ़ाइल वॉकर + अनामीकरण
    └── vibecoding/                     # Vibecoding अनुबंध (ToolRegistry, आदि)
```

## N0 अनुबंध (workspace-bom MEMPHIS से)

| अनुबंध | आर्टिफ़ैक्ट | प्रदान करता है |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## N2 निर्भरताएँ

- `codex-plugin` — दस्तावेज़ अनुक्रमण (PDF/EPUB → pgvector)
- `planner-plugin` — LLM प्रॉम्प्टिंग SPG/SPD

## प्रमुख लाइब्रेरीज़

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — LLM प्रदाता, RAG, embeddings
- **R2DBC** — अभिक्रियाशील PostgreSQL (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — YAML कॉन्फ़िग, JSON क्रमांकन
- **Kover** 0.9.8 — कवरेज गेट (≥80%)

## Ollama इंस्टेंस (वैश्विक बाध्यता)

पोर्ट `11434–11436` निषिद्ध हैं। `11437–11465` (29 पोर्ट) पर रोटेशन।
अधिकृत मॉडल: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।

## परीक्षण मैट्रिक्स

| कार्य | दायरा | टाइमआउट |
|------|-------|---------|
| `test` | JUnit5 इकाई परीक्षण | डिफ़ॉल्ट |
| `testFast` | त्वरित Cucumber (≤ 8 मिनट) | 8 मिनट |
| `testAll` | JUnit5 + सभी Cucumber | — |
| `testEpics` | सभी EPIC Cucumber BDD | — |
| `cucumberTest*` (48 कार्य) | प्रत्येक EPIC के लिए एक | 8–15 मिनट |

सत्यापन कार्य: `validateDependencies` (CVE-2015-7501 रक्षा + भारी ट्रांज़िटिव), `koverVerify` (`check` में एकीकृत)।

## JVM ट्यूनिंग

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, हीप 2g
- **स्थानीय**: SerialGC + TieredStopAtLevel=4, अनुकूली हीप (होस्ट < 8 GB → 512m, अन्यथा 1g)
- **Metaspace**: 512m (दोनों प्रोफ़ाइल)

## बिल्ड कमांड

```bash
./gradlew build                       # पूर्ण बिल्ड (कंपाइल + परीक्षण)
./gradlew build -x test               # केवल कंपाइल
./gradlew testFast                    # त्वरित परीक्षण (PR गेट, ≤ 8 मिनट)
./gradlew testAll                     # पूर्ण परीक्षण सूट
./gradlew koverVerify                 # कवरेज ≥ 80%
./gradlew validateDependencies        # CVE ऑडिट + भारी ट्रांज़िटिव
./gradlew publishToMavenLocal         # स्थानीय प्रकाशन
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI पाइपलाइन

`.github/workflows/test.yml` तीन जॉब परिभाषित करता है:
1. **fast-tests** — प्रत्येक PR पर `./gradlew testFast -x build` (≤ 15 मिनट)
2. **full-tests** — main/master पर `./gradlew testAll` (≤ 45 मिनट, रिपोर्ट अपलोड)
3. **publish** — `v*` टैग पर `./gradlew publishAggregationToCentralPortal` (GPG आवश्यक)

## प्रकाशन (NMCP)

`settings.gradle.kts` में `com.gradleup.nmcp.settings` (1.5.0) के माध्यम से कॉन्फ़िगर किया गया।
क्रेडेंशियल `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`) से पढ़े जाते हैं।
साइनिंग `useGpgCmd()` का उपयोग करती है; CI `crazy-max/ghaction-import-gpg@v6` द्वारा GPG कुंजी आयात करता है।
POM Apache 2.0, डेवलपर `cccp-education`, SCM घोषित करता है जो
`github.com/cccp-education/codebase-gradle` को इंगित करता है।

## EPIC स्थिति

`0.0.4` में सभी EPIC बंद (`.agents/INDEX.adoc` देखें):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL।

## योगदान

1. बिल्ड कंपाइल होता है: `./gradlew build -x test`
2. त्वरित परीक्षण हरे: `./gradlew testFast`
3. कवरेज सम्मानित: `./gradlew koverVerify`
4. कोई CVE प्रतिगमन नहीं: `./gradlew validateDependencies`
5. DDD सम्मेलनों का पालन (मूल्य वस्तु, पोर्ट/एडेप्टर, कोई लीक नहीं)

## आर्किटेक्चर दस्तावेज़

- [BUILDING.md](../codebase-plugin/BUILDING.md) — बिल्ड कॉन्फ़िग और JVM ट्यूनिंग
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — पारिस्थितिकी तंत्र अवलोकन
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — स्थानीय विकास सेटअप
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — उपयोग मोड
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC और शासन

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का हिस्सा — `groupId: education.cccp`।_