<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Руководство потребителя

> Плагин Gradle на базе RAG для индексации кодовой базы с дополнением LLM и vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Тесты)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Лицензия)](../LICENSE)

- **Версия**: `0.0.4` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.codebase`
- **Сборка**: `./gradlew build` · **Тесты**: `./gradlew testAll` (JUnit5 + 48 наборов Cucumber)
- **Покрытие**: ≥ 80 % (Kover, встроено в `check`)

🌐 Языки: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`codebase-gradle` индексирует исходные файлы вашего проекта в **PostgreSQL + pgvector**, обеспечивает
составное расширение контекста, анонимизацию, OCR (Gemini Vision), шлюзы качества
(тональность + PII + отклонение от темы) и **vibecoding** — многоходовый цикл генерации кода
на базе LLM через `koog-agents`.

Часть мультиплагинной экосистемы CCCP Education:

```
намерение пользователя → codex-gradle (индексация) → [codebase-gradle] → koog-agents → шлюзы качества → вывод
```

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Индексировать кодовую базу

```bash
./gradlew collectFromCodebase          # контекст по районам → build/context/
./gradlew collectCompositeContext       # составной контекст уровня рабочего пространства
```

### 3. Запустить vibecoding

```bash
./gradlew vibecode \
  --intention="Проанализировать архитектуру" \
  --dryRun \
  --maxActions=10
```

Полный список параметров см. в [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc).

## Доступные задачи

| Задача | Группа | Описание |
|------|-------|-------------|
| `collectFromCodebase`      | collect   | Расширенный контекст по районам (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | Составной контекст уровня рабочего пространства из всех районов |
| `generateCompositeContext` | generate  | Составной N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | Расширенное планирование — классификация намерения → EPICs/UserStories/Tasks |
| `vibecode`                 | generate  | Автономный цикл Koog (контекст → план → выполнение → аудит) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | Загрузка файлов EAGER (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | Сводка сессии, затраты токенов, фильтры приватности |
| `qualityGate`              | validate  | Проверки тональности + отклонения от темы + остаточного PII |
| `ocrDocument`              | collect   | OCR через Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | Загрузка вывода OCR в pgvector |
| `exposeExperts`            | generate  | Манифест экспертов JSON для slider/plantuml/bakery |
| `endSessionBlog`           | generate  | Извлечение сессий → статьи блога AsciiDoc |

## DSL расширений

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

## Требования

- **Java** 24+ (цепочка инструментов Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ с расширением `pgvector`
- **Docker** (для Testcontainers)

## Сборка и тесты

```bash
./gradlew build                    # полная сборка
./gradlew testFast                 # быстрый Cucumber (≤ 8 мин)
./gradlew testAll                  # полный набор (JUnit5 + весь Cucumber)
./gradlew testEpics               # весь Cucumber BDD EPICs
./gradlew testHelp                 # список всех тестовых задач
./gradlew validateDependencies    # аудит CVE + проверка зависимостей
./gradlew publishToMavenLocal      # локальная публикация
```

## Устранение неполадок

| Симптом | Решение |
|---------|---------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Контейнер Postgres завис | `docker rm -f postgres-*` и повторить |
| Таймаут теста             | проверить `docker ps`, увеличить heap, проверить задержку API LLM |

Подробности см. в [BUILDING.md](../codebase-plugin/BUILDING.md).

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._