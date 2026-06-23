<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — उपभोक्ता गाइड

> LLM-संवर्धित कोडबेस इंडेक्सिंग और vibecoding के लिए RAG-संचालित Gradle प्लगइन।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=परीक्षण)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=लाइसेंस)](../LICENSE)

- **संस्करण**: `0.0.4` · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.codebase`
- **बिल्ड**: `./gradlew build` · **परीक्षण**: `./gradlew testAll` (JUnit5 + 48 Cucumber सूट)
- **कवरेज**: ≥ 80% (Kover, `check` में एकीकृत)

🌐 भाषाएँ: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`codebase-gradle` आपके प्रोजेक्ट स्रोतों को **PostgreSQL + pgvector** में इंडेक्स करता है, संयुक्त संदर्भ संवर्धन, गुमनामीकरण, OCR (Gemini Vision), गुणवत्ता गेट (भावना + PII + विषय से बाहर) और **vibecoding** प्रदान करता है — `koog-agents` द्वारा संचालित LLM आधारित बहु-बारी कोड जनरेशन लूप।

CCCP Education बहु-प्लगइन पारिस्थितिकी तंत्र का हिस्सा:

```
उपयोगकर्ता अभिप्राय → codex-gradle (इंडेक्सिंग) → [codebase-gradle] → koog-agents → गुणवत्ता गेट → आउटपुट
```

## त्वरित आरंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. अपने कोडबेस को इंडेक्स करें

```bash
./gradlew collectFromCodebase          # प्रति-बरो संदर्भ → build/context/
./gradlew collectCompositeContext       # वर्कस्पेस-स्तरीय संयुक्त
```

### 3. vibecoding चलाएँ

```bash
./gradlew vibecode \
  --intention="आर्किटेक्चर का विश्लेषण करें" \
  --dryRun \
  --maxActions=10
```

पूर्ण विकल्पों के लिए [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) देखें।

## उपलब्ध कार्य

| कार्य | समूह | विवरण |
|------|------|--------|
| `collectFromCodebase`      | collect   | प्रति-बरो संवर्धित संदर्भ (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | सभी बरो से वर्कस्पेस-स्तरीय संयुक्त |
| `generateCompositeContext` | generate  | संयुक्त N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | संवर्धित योजना — अभिप्राय वर्गीकरण → EPIC/UserStory/Task |
| `vibecode`                 | generate  | Koog स्वायत्त लूप (संदर्भ → योजना → निष्पादन → ऑडिट) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | EAGER फ़ाइलें अंतर्ग्रहण (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | सत्र सारांश, टोकन लागत, गोपनीयता फ़िल्टर |
| `qualityGate`              | validate  | भावना + विषय से बाहर + PII अवशिष्ट जाँच |
| `ocrDocument`              | collect   | Gemini Vision द्वारा OCR → AsciiDoc |
| `ocrIngest`                | collect   | OCR आउटपुट को pgvector में अंतर्ग्रहण |
| `exposeExperts`            | generate  | slider/plantuml/bakery के लिए विशेषज्ञ मैनिफेस्ट JSON |
| `endSessionBlog`           | generate  | सत्र निकालें → AsciiDoc ब्लॉग लेख |

## एक्सटेंशन DSL

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

## पूर्वापेक्षाएँ

- **Java** 24+ (Kotlin 2.3.20 टूलचेन)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ `pgvector` एक्सटेंशन के साथ
- **Docker** (Testcontainers के लिए)

## बिल्ड और परीक्षण

```bash
./gradlew build                    # पूर्ण बिल्ड
./gradlew testFast                 # त्वरित Cucumber (≤ 8 मिनट)
./gradlew testAll                  # पूर्ण सूट (JUnit5 + सभी Cucumber)
./gradlew testEpics               # सभी EPIC Cucumber BDD
./gradlew testHelp                 # सभी परीक्षण कार्य सूचीबद्ध करें
./gradlew validateDependencies    # CVE ऑडिट + निर्भरता सत्यापन
./gradlew publishToMavenLocal      # स्थानीय रूप से प्रकाशित करें
```

## समस्या निवारण

| लक्षण | समाधान |
|------|--------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Postgres कंटेनर अटक गया | `docker rm -f postgres-*` फिर पुनः प्रयास करें |
| परीक्षण समय समाप्त      | `docker ps` जाँचें, हीप बढ़ाएँ, LLM API विलंबता जाँचें |

विवरण के लिए [BUILDING.md](../codebase-plugin/BUILDING.md) देखें।

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का हिस्सा — `groupId: education.cccp`।_