<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — دليل المستهلك

> مكوّن Gradle يعتمد على RAG لفهرسة الشيفرة المعزّزة بـ LLM و vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=الاختبارات)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=الرخصة)](../LICENSE)

- **الإصدار**: `0.0.4` · **المجموعة**: `education.cccp` · **معرّف المكوّن**: `education.cccp.codebase`
- **البناء**: `./gradlew build` · **الاختبارات**: `./gradlew testAll` (JUnit5 + 48 مجموعة Cucumber)
- **التغطية**: ≥ 80% (Kover، مُدمج في `check`)

🌐 اللغات: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا يفعل

يقوم `codebase-gradle` بفهرسة ملفات مصدر مشروعك في **PostgreSQL + pgvector**، ويوفّر
تعزيز السياق المركّب، وإخفاء الهوية، و OCR (Gemini Vision)، وبوابات الجودة
(المشاعر + PII + خارج الموضوع) و **vibecoding** — حلقة توليد شيفرة متعددة الأدوار
مدعومة بـ LLM عبر `koog-agents`.

جزء من نظام CCCP Education متعدد المكوّنات:

```
نية المستخدم → codex-gradle (الفهرسة) → [codebase-gradle] → koog-agents → بوابات الجودة → المخرجات
```

## البدء السريع

### 1. تطبيق المكوّن

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. فهرسة قاعدة الشيفرة

```bash
./gradlew collectFromCodebase          # سياق لكل حي → build/context/
./gradlew collectCompositeContext       # سياق مركّب على مستوى مساحة العمل
```

### 3. تشغيل vibecoding

```bash
./gradlew vibecode \
  --intention="تحليل البنية" \
  --dryRun \
  --maxActions=10
```

راجع [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) للاطلاع على الخيارات الكاملة.

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|------|-------|-------------|
| `collectFromCodebase`      | collect   | سياق معزّز لكل حي (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | سياق مركّب على مستوى مساحة العمل من جميع الأحياء |
| `generateCompositeContext` | generate  | مركّب N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | تخطيط معزّز — تصنيف النية → EPICs/UserStory/Task |
| `vibecode`                 | generate  | حلقة Koog المستقلة (سياق ← خطة ← تنفيذ ← تدقيق) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | استيعاب ملفات EAGER (AGENT.adoc، INDEX.adoc، BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | ملخص الجلسة، تكاليف الرموز، مرشحات الخصوصية |
| `qualityGate`              | validate  | فحوصات المشاعر + خارج الموضوع + PII المتبقي |
| `ocrDocument`              | collect   | OCR عبر Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | استيعاب مخرجات OCR إلى pgvector |
| `exposeExperts`            | generate  | بيان الخبراء JSON لـ slider/plantuml/bakery |
| `endSessionBlog`           | generate  | استخراج الجلسات ← مقالات مدونة AsciiDoc |

## DSL الامتداد

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

## المتطلبات المسبقة

- **Java** 24+ (سلسلة أدوات Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ مع امتداد `pgvector`
- **Docker** (لـ Testcontainers)

## البناء والاختبار

```bash
./gradlew build                    # بناء كامل
./gradlew testFast                 # Cucumber سريع (≤ 8 دقيقة)
./gradlew testAll                  # مجموعة كاملة (JUnit5 + كل Cucumber)
./gradlew testEpics               # كل Cucumber BDD للملحمات
./gradlew testHelp                 # سرد جميع مهام الاختبار
./gradlew validateDependencies    # تدقيق CVE + التحقق من التبعيات
./gradlew publishToMavenLocal      # نشر محلي
```

## استكشاف الأخطاء وإصلاحها

| العَرَض | الحل |
|---------|-----|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| توقف حاوية Postgres     | `docker rm -f postgres-*` ثم إعادة المحاولة |
| انتهاء وقت الاختبار      | تحقق من `docker ps`، زيادة الذاكرة، التحقق من زمن استجابة LLM API |

راجع [BUILDING.md](../codebase-plugin/BUILDING.md) للتفاصيل.

## الرخصة

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من نظام CCCP Education — `groupId: education.cccp`._