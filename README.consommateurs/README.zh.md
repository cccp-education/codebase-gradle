<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — 消费者指南

> 基于 RAG 的 Gradle 插件，用于 LLM 增强的代码库索引与 vibecoding。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=测试)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=许可证)](../LICENSE)

- **版本**：`0.0.4` · **组**：`education.cccp` · **插件 ID**：`education.cccp.codebase`
- **构建**：`./gradlew build` · **测试**：`./gradlew testAll`（JUnit5 + 48 个 Cucumber 套件）
- **覆盖率**：≥ 80%（Kover，集成于 `check`）

🌐 语言：[English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能简介

`codebase-gradle` 将项目源码索引到 **PostgreSQL + pgvector**，提供组合上下文增强、匿名化、OCR（Gemini Vision）、质量门禁（情感 + PII + 离题检测）以及 **vibecoding** —— 由 `koog-agents` 驱动的 LLM 多轮代码生成循环。

属于 CCCP Education 多插件生态系统的一部分：

```
用户意图 → codex-gradle（索引）→ [codebase-gradle] → koog-agents → 质量门禁 → 输出
```

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. 索引代码库

```bash
./gradlew collectFromCodebase          # 各区上下文 → build/context/
./gradlew collectCompositeContext       # 工作区级组合
```

### 3. 运行 vibecoding

```bash
./gradlew vibecode \
  --intention="分析架构" \
  --dryRun \
  --maxActions=10
```

完整选项见 [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc)。

## 可用任务

| 任务 | 组 | 说明 |
|------|----|------|
| `collectFromCodebase`      | collect   | 各区增强上下文（EAGER/RAG/Graphify） |
| `collectCompositeContext`  | collect   | 全工作区组合上下文 |
| `generateCompositeContext` | generate  | 组合 N1+N2（codex + training）→ JSON |
| `generatePlan`             | generate  | 增强规划 —— 意图分类 → 史诗/故事/任务 |
| `vibecode`                 | generate  | Koog 自主循环（上下文 → 规划 → 执行 → 审计） |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | 摄取 EAGER 文件（AGENT.adoc、INDEX.adoc、BACKLOG.adoc） |
| `vibecodingDashboard`      | tracking  | 会话摘要、令牌成本、隐私过滤 |
| `qualityGate`              | validate  | 情感 + 离题 + PII 残留检查 |
| `ocrDocument`              | collect   | 通过 Gemini Vision 进行 OCR → AsciiDoc |
| `ocrIngest`                | collect   | 将 OCR 结果摄取至 pgvector |
| `exposeExperts`            | generate  | 专家清单 JSON（供 slider/plantuml/bakery 使用） |
| `endSessionBlog`           | generate  | 提取会话 → AsciiDoc 博客文章 |

## 扩展 DSL

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

## 前置条件

- **Java** 24+（Kotlin 2.3.20 工具链）
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ 且带 `pgvector` 扩展
- **Docker**（用于 Testcontainers）

## 构建与测试

```bash
./gradlew build                    # 完整构建
./gradlew testFast                 # 快速 Cucumber（≤ 8 分钟）
./gradlew testAll                  # 完整套件（JUnit5 + 所有 Cucumber）
./gradlew testEpics               # 所有 EPIC Cucumber BDD
./gradlew testHelp                 # 列出所有测试任务
./gradlew validateDependencies    # CVE 审计 + 依赖校验
./gradlew publishToMavenLocal      # 本地发布
```

## 故障排除

| 症状 | 解决方法 |
|------|----------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Postgres 容器卡住       | `docker rm -f postgres-*` 后重试 |
| 测试超时                 | 检查 `docker ps`，增加堆内存，检查 LLM API 延迟 |

详见 [BUILDING.md](../codebase-plugin/BUILDING.md)。

## 许可证

Apache License 2.0 —— 见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_