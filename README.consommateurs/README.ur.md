<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — صارفین کی گائیڈ

> LLM کی تقویت یافتہ کوڈبیس انڈیکسنگ اور vibecoding کے لیے RAM پر مبنی Gradle پلگ ان۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=ٹیسٹ)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=لائسنس)](../LICENSE)

- **ورژن**: `0.0.4` · **گروپ**: `education.cccp` · **پلگ ان آئی ڈی**: `education.cccp.codebase`
- **بلڈ**: `./gradlew build` · **ٹیسٹ**: `./gradlew testAll` (JUnit5 + 48 Cucumber سوٹس)
- **کوریج**: ≥ 80% (Kover، `check` میں ضم)

🌐 زبانیں: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`codebase-gradle` آپ کے پروجیکٹ کے سورس فائلز کو **PostgreSQL + pgvector** میں انڈیکس کرتا ہے، مرکب سیاق و پیاق میں اضافہ، گمنامی، OCR (Gemini Vision)، کوالٹی گیٹ (جذبات + PII + موضوع سے باہر) اور **vibecoding** فراہم کرتا ہے — `koog-agents` کے ذریعے چلایا جانے والا ایک LLM پر مبنی کثیر-مرحلہ کوڈ جنریشن لوپ۔

CCCP Education کے ملٹی پلگ ان ماحولیاتی نظام کا حصہ:

```
صارف کی نیت → codex-gradle (انڈیکسنگ) → [codebase-gradle] → koog-agents → کوالٹی گیٹ → آؤٹ پٹ
```

## جلدی آغاز

### ۱. پلگ ان لگائیں

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### ۲. اپنی کوڈبیس انڈیکس کریں

```bash
./gradlew collectFromCodebase          # ہر بورو کا سیاق و پیاق → build/context/
./gradlew collectCompositeContext       # ورک اسپیس سطح کا مرکب
```

### ۳. vibecoding چلائیں

```bash
./gradlew vibecode \
  --intention="آرکیٹیکچر کا تجزیہ کریں" \
  --dryRun \
  --maxActions=10
```

مکمل اختیارات کے لیے [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) دیکھیں۔

## دستیاب ٹاسکس

| ٹاسک | گروپ | تفصیل |
|------|------|--------|
| `collectFromCodebase`      | collect   | ہر بورو کا بڑھا ہوا سیاق و پیاق (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | تمام بوروز سے ورک اسپیس سطح کا مرکب |
| `generateCompositeContext` | generate  | مرکب N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | بڑھا ہوا منصوبہ — نیت کی درجہ بندی → EPIC/UserStory/Task |
| `vibecode`                 | generate  | Koog خودمختار لوپ (سیاق و پیاق ← منصوبہ ← عمل درآمد ← آڈٹ) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | EAGER فائلز کا استعمال (AGENT.adoc، INDEX.adoc، BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | سیشن خلاصہ، ٹوکن لاگت، رازدازی فلٹرز |
| `qualityGate`              | validate  | جذبات + موضوع سے باہر + PII باقیات کی جانچ |
| `ocrDocument`              | collect   | Gemini Vision کے ذریعے OCR → AsciiDoc |
| `ocrIngest`                | collect   | OCR آؤٹ پٹ کو pgvector میں داخل کرنا |
| `exposeExperts`            | generate  | slider/plantuml/bakery کے لیے ماہرین کا مینیفیسٹ JSON |
| `endSessionBlog`           | generate  | سیشنز نکالنا → AsciiDoc بلاگ مضامین |

## ایکسٹینشن DSL

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

## ضروریات

- **Java** 24+ (Kotlin 2.3.20 ٹول چین)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ `pgvector` ایکسٹینشن کے ساتھ
- **Docker** (Testcontainers کے لیے)

## بلڈ اور ٹیسٹ

```bash
./gradlew build                    # مکمل بلڈ
./gradlew testFast                 # تیز Cucumber (≤ ۸ منٹ)
./gradlew testAll                  # مکمل سوٹ (JUnit5 + تمام Cucumber)
./gradlew testEpics               # تمام EPIC Cucumber BDD
./gradlew testHelp                 # تمام ٹیسٹ ٹاسکس کی فہرست
./gradlew validateDependencies    # CVE آڈٹ + انحصار کی تصدیق
./gradlew publishToMavenLocal      # مقامی طور پر شائع کریں
```

## مسئلہ حل

| علامت | حل |
|------|-----|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Postgres کنٹینر پھنس گیا | `docker rm -f postgres-*` پھر دوبارہ کوشش کریں |
| ٹیسٹ ٹائم آؤٹ      | `docker ps` چیک کریں، ہیپ بڑھائیں، LLM API لےٹنسی چیک کریں |

تفصیلات کے لیے [BUILDING.md](../codebase-plugin/BUILDING.md) دیکھیں۔

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔_