<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — ভোক্তা গাইড

> LLM-বর্ধিত কোডবেস ইনডেক্সিং এবং vibecoding-এর জন্য RAG-চালিত Gradle প্লাগইন।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=পরীক্ষা)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=লাইসেন্স)](../LICENSE)

- **সংস্করণ**: `0.0.4` · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.codebase`
- **বিল্ড**: `./gradlew build` · **পরীক্ষা**: `./gradlew testAll` (JUnit5 + 48 Cucumber স্যুট)
- **কভারেজ**: ≥ 80% (Kover, `check`-এ একত্রিত)

🌐 ভাষা: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`codebase-gradle` আপনার প্রোজেক্টের সোর্স ফাইলগুলো **PostgreSQL + pgvector**-এ ইনডেক্স করে, যৌগিক প্রসঙ্গ বৃদ্ধি, অনামিককরণ, OCR (Gemini Vision), কোয়ালিটি গেট (আবেগ + PII + বিষয়বহির্ভূত) এবং **vibecoding** প্রদান করে — `koog-agents` দ্বারা চালিত একটি LLM-ভিত্তিক বহু-পর্যায় কোড জেনারেশন লুপ।

CCCP Education বহু-প্লাগইন ইকোসিস্টেমের অংশ:

```
ব্যবহারকারীর অভিপ্রায় → codex-gradle (ইনডেক্সিং) → [codebase-gradle] → koog-agents → কোয়ালিটি গেট → আউটপুট
```

## দ্রুত শুরু

### ১. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### ২. আপনার কোডবেস ইনডেক্স করুন

```bash
./gradlew collectFromCodebase          # প্রতি-বরো প্রসঙ্গ → build/context/
./gradlew collectCompositeContext       # ওয়ার্কস্পেস-স্তরের যৌগিক
```

### ৩. vibecoding চালান

```bash
./gradlew vibecode \
  --intention="আর্কিটেকচার বিশ্লেষণ করুন" \
  --dryRun \
  --maxActions=10
```

সম্পূর্ণ বিকল্পের জন্য [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) দেখুন।

## উপলব্ধ কাজ

| কাজ | গ্রুপ | বিবরণ |
|------|------|--------|
| `collectFromCodebase`      | collect   | প্রতি-বরো বৃদ্ধিত প্রসঙ্গ (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | সব বরো থেকে ওয়ার্কস্পেস-স্তরের যৌগিক |
| `generateCompositeContext` | generate  | যৌগিক N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | বৃদ্ধিত পরিকল্পনা — অভিপ্রায় শ্রেণীবিভাজন → EPIC/UserStory/Task |
| `vibecode`                 | generate  | Koog স্বায়ত্তশাসিত লুপ (প্রসঙ্গ → পরিকল্পনা → সম্পাদন → নিরীক্ষা) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | EAGER ফাইল গ্রহণ (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | সেশন সারাংশ, টোকেন খরচ, গোপনীয়তা ফিল্টার |
| `qualityGate`              | validate  | আবেগ + বিষয়বহির্ভূত + PII অবশিষ্ট যাচাই |
| `ocrDocument`              | collect   | Gemini Vision দ্বারা OCR → AsciiDoc |
| `ocrIngest`                | collect   | OCR আউটপুট pgvector-এ গ্রহণ |
| `exposeExperts`            | generate  | slider/plantuml/bakery-এর জন্য বিশেষজ্ঞ ম্যানিফেস্ট JSON |
| `endSessionBlog`           | generate  | সেশন নিষ্কাশন → AsciiDoc ব্লগ নিবন্ধ |

## এক্সটেনশন DSL

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

## পূর্বশর্ত

- **Java** 24+ (Kotlin 2.3.20 টুলচেইন)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ `pgvector` এক্সটেনশন সহ
- **Docker** (Testcontainers-এর জন্য)

## বিল্ড ও পরীক্ষা

```bash
./gradlew build                    # সম্পূর্ণ বিল্ড
./gradlew testFast                 # দ্রুত Cucumber (≤ ৮ মিনিট)
./gradlew testAll                  # সম্পূর্ণ স্যুট (JUnit5 + সব Cucumber)
./gradlew testEpics               # সব EPIC Cucumber BDD
./gradlew testHelp                 # সব পরীক্ষার কাজ তালিকাভুক্ত করুন
./gradlew validateDependencies    # CVE অডিট + নির্ভরতা যাচাই
./gradlew publishToMavenLocal      # স্থানীয়ভাবে প্রকাশ করুন
```

## সমস্যা সমাধান

| লক্ষণ | সমাধান |
|------|--------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Postgres কন্টেইনার আটকে আছে | `docker rm -f postgres-*` তারপর পুনঃপ্রচেষ্টা |
| পরীক্ষা টাইমআউট      | `docker ps` যাচাই করুন, হিপ বাড়ান, LLM API লেটেন্সি যাচাই করুন |

বিস্তারিত জানতে [BUILDING.md](../codebase-plugin/BUILDING.md) দেখুন।

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_