<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — داخليات المكوّن

> دليل المطوّر والمساهم لمكوّن Gradle `codebase-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=الاختبارات)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=التغطية&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=الرخصة)](../LICENSE)

- **الإصدار**: `0.0.4` · **المجموعة**: `education.cccp` · **معرّف المكوّن**: `education.cccp.codebase`
- **سلسلة الأدوات**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **البناء**: `./gradlew build -x test` · **الاختبارات**: `./gradlew testAll` · **بوابة التغطية**: `./gradlew koverVerify` (≥80%)

🌐 اللغات: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## تخطيط الوحدات

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # نقطة دخول المكوّن — يسجّل جميع المهام
    │   ├── benchmark/                  # قياس أداء إدراك LLM
    │   ├── blog/                       # جلسة ← تخفيف مقالة المدونة
    │   ├── koog/                       # رسومات DSL koog (vibecoding، autofocus، agentic)
    │   ├── ocr/                        # OCR عبر Gemini Vision + Ollama
    │   ├── quality/                    # بوابات جودة ONNX (المشاعر، PII، خارج الموضوع)
    │   ├── rag/                        # RAG pgvector (VectorStore، التضمينات)
    │   └── walker/                     # متجوّل الملفات + إخفاء الهوية
    └── vibecoding/                     # عقود Vibecoding (ToolRegistry، إلخ)
```

## عقود N0 (من workspace-bom MEMPHIS)

| العقد | الأرتيفاكت | يوفّر |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## تبعيات N2

- `codex-plugin` — فهرسة المستندات (PDF/EPUB ← pgvector)
- `planner-plugin` — LLM prompting لـ SPG/SPD

## المكتبات الرئيسية

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — مزوّدو LLM، RAG، التضمينات
- **R2DBC** — PostgreSQL تفاعلي (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — إعداد YAML، تسلسل JSON
- **Kover** 0.9.8 — بوابة التغطية (≥80%)

## حالات Ollama (قيد عام)

المنافذ `11434–11436` محظورة. التدوير على `11437–11465` (29 منفذ).
النماذج المعتمدة: `gpt-oss:120b-cloud`، `gemma4:31b-cloud`。

## مصفوفة الاختبارات

| المهمة | النطاق | المهلة |
|------|-------|---------|
| `test` | اختبارات وحدة JUnit5 | افتراضي |
| `testFast` | Cucumber سريع (≤ 8 دقيقة) | 8 دقائق |
| `testAll` | JUnit5 + كل Cucumber | — |
| `testEpics` | كل Cucumber BDD للملحمات | — |
| `cucumberTest*` (48 مهمة) | واحدة لكل ملحمة | 8–15 دقيقة |

مهام التحقق: `validateDependencies` (حارس CVE-2015-7501 + التبعيات الثقيلة)، `koverVerify` (مدمج في `check`).

## ضبط JVM

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200، كومة 2g
- **محلي**: SerialGC + TieredStopAtLevel=4، كومة تكيّفية (مضيف < 8 GB → 512m، وإلا 1g)
- **Metaspace**: 512m (كلا الملفين الشخصيين)

## أوامر البناء

```bash
./gradlew build                       # بناء كامل (ترجمة + اختبار)
./gradlew build -x test               # ترجمة فقط
./gradlew testFast                    # اختبارات سريعة (بوابة PR، ≤ 8 دقائق)
./gradlew testAll                     # مجموعة اختبارات كاملة
./gradlew koverVerify                 # تغطية ≥ 80%
./gradlew validateDependencies        # تدقيق CVE + تبعيات ثقيلة
./gradlew publishToMavenLocal         # نشر محلي
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## خط أنابيب CI

يعرّف `.github/workflows/test.yml` ثلاث وظائف:
1. **fast-tests** — `./gradlew testFast -x build` على كل PR (≤ 15 دقيقة)
2. **full-tests** — `./gradlew testAll` على main/master (≤ 45 دقيقة، رفع التقارير)
3. **publish** — `./gradlew publishAggregationToCentralPortal` على وسوم `v*` (يتطلب GPG)

## النشر (NMCP)

مُكوّن عبر `com.gradleup.nmcp.settings` (1.5.0) في `settings.gradle.kts`。
تُقرأ بيانات الاعتماد من `~/.gradle/gradle.properties` (`ossrhUsername`، `ossrhPassword`)。
التوقيع يستخدم `useGpgCmd()`؛ CI يستورد مفتاح GPG عبر `crazy-max/ghaction-import-gpg@v6`。
يصرّح POM بـ Apache 2.0، المطوّر `cccp-education`، SCM يشير إلى
`github.com/cccp-education/codebase-gradle`。

## حالة الملحمات

جميع الملحمات مغلقة في `0.0.4` (راجع `.agents/INDEX.adoc`):
V، V-9، K، L، W، X، Y، Z، OCR، PUB، V-LOCAL، CR، session-proTOCOL، 8، TRAD، V-6-POOL。

## المساهمة

1. البناء يترجم: `./gradlew build -x test`
2. الاختبارات السريعة خضراء: `./gradlew testFast`
3. التغطية محترمة: `./gradlew koverVerify`
4. لا تراجع CVE: `./gradlew validateDependencies`
5. اتباع اصطلاحات DDD (كائنات القيمة، المنافذ/المحوّلات، بدون تسرب)

## وثائق البنية

- [BUILDING.md](../codebase-plugin/BUILDING.md) — إعداد البناء وضبط JVM
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — نظرة النظام البيئي
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — إعداد التطوير المحلي
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — أوضاع الاستخدام
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — الملحمات والحوكمة

## الرخصة

Apache License 2.0 — راجع [LICENSE](../LICENSE)。

---

_جزء من نظام CCCP Education — `groupId: education.cccp`._