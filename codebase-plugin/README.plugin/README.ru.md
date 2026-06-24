<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Внутреннее устройство плагина

> Руководство для разработчиков и контрибьюторов плагина Gradle `codebase-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Тесты)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=покрытие&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Лицензия)](../LICENSE)

- **Версия**: `0.0.4` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.codebase`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Сборка**: `./gradlew build -x test` · **Тесты**: `./gradlew testAll` · **Gate покрытия**: `./gradlew koverVerify` (≥80%)

🌐 Языки: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Структура модулей

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # Точка входа плагина — регистрирует все задачи
    │   ├── benchmark/                  # Бенчмарк восприятия LLM
    │   ├── blog/                       # Сессия ← разрежение статьи блога
    │   ├── koog/                       # Графы DSL koog (vibecoding, autofocus, agentic)
    │   ├── ocr/                        # OCR через Gemini Vision + Ollama
    │   ├── quality/                    # Шлюзы качества ONNX (тональность, PII, офтопик)
    │   ├── rag/                        # RAG pgvector (VectorStore, эмбеддинги)
    │   └── walker/                     # Обход файлов + анонимизация
    └── vibecoding/                     # Контракты Vibecoding (ToolRegistry, и т.д.)
```

## Контракты N0 (из workspace-bom MEMPHIS)

| Контракт | Артефакт | Предоставляет |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## Зависимости N2

- `codex-plugin` — индексация документов (PDF/EPUB → pgvector)
- `planner-plugin` — LLM-промптинг SPG/SPD

## Ключевые библиотеки

- **koog-agents** 1.0.0 — DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 — провайдеры LLM, RAG, эмбеддинги
- **R2DBC** — реактивный PostgreSQL (pgvector)
- **Testcontainers** — `pgvector/pgvector:pg17`
- **Jackson** — конфиг YAML, сериализация JSON
- **Kover** 0.9.8 — gate покрытия (≥80%)

## Инстансы Ollama (глобальное ограничение)

Порты `11434–11436` запрещены. Ротация по `11437–11465` (29 портов).
Авторизованные модели: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Матрица тестов

| Задача | Область | Таймаут |
|------|-------|---------|
| `test` | Unit-тесты JUnit5 | по умолчанию |
| `testFast` | Быстрый Cucumber (≤ 8 мин) | 8 мин |
| `testAll` | JUnit5 + весь Cucumber | — |
| `testEpics` | Весь Cucumber BDD EPICs | — |
| `cucumberTest*` (48 задач) | По одной на EPIC | 8–15 мин |

Задачи верификации: `validateDependencies` (защита CVE-2015-7501 + тяжёлые транзитивные), `koverVerify` (встроено в `check`).

## Настройка JVM

- **CI**: G1GC + ParallelRefProc + MaxGCPauseMillis=200, heap 2g
- **Локально**: SerialGC + TieredStopAtLevel=4, адаптивный heap (хост < 8 ГБ → 512m, иначе 1g)
- **Metaspace**: 512m (оба профиля)

## Команды сборки

```bash
./gradlew build                       # полная сборка (компиляция + тесты)
./gradlew build -x test               # только компиляция
./gradlew testFast                    # быстрые тесты (gate PR, ≤ 8 мин)
./gradlew testAll                     # полный набор
./gradlew koverVerify                 # покрытие ≥ 80%
./gradlew validateDependencies        # аудит CVE + тяжёлые транзитивные
./gradlew publishToMavenLocal         # локальная публикация
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI-пайплайн

`.github/workflows/test.yml` определяет три job:
1. **fast-tests** — `./gradlew testFast -x build` на каждый PR (≤ 15 мин)
2. **full-tests** — `./gradlew testAll` на main/master (≤ 45 мин, загрузка отчётов)
3. **publish** — `./gradlew publishAggregationToCentralPortal` на тегах `v*` (требуется GPG)

## Публикация (NMCP)

Настроено через `com.gradleup.nmcp.settings` (1.5.0) в `settings.gradle.kts`.
Учётные данные читаются из `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`).
Подпись использует `useGpgCmd()`; CI импортирует GPG-ключ через `crazy-max/ghaction-import-gpg@v6`.
POM декларирует Apache 2.0, разработчик `cccp-education`, SCM указывает на
`github.com/cccp-education/codebase-gradle`.

## Статус EPICs

Все EPICs закрыты в `0.0.4` (см. `.agents/INDEX.adoc`):
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL.

## Контрибьюшн

1. Сборка компилируется: `./gradlew build -x test`
2. Быстрые тесты зелёные: `./gradlew testFast`
3. Покрытие соблюдено: `./gradlew koverVerify`
4. Нет регрессии CVE: `./gradlew validateDependencies`
5. Следовать конвенциям DDD (value objects, порты/адаптеры, без утечек)

## Документация по архитектуре

- [BUILDING.md](../codebase-plugin/BUILDING.md) — Конфиг сборки и настройка JVM
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) — Обзор экосистемы
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) — Локальный dev-сетап
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) — Режимы использования
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs и управление

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._