<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — 插件内部指南

> `codebase-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=测试)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=覆盖率&message=%E2%89%A580%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=许可证)](../LICENSE)

- **版本**：`0.0.4` · **组**：`education.cccp` · **插件 ID**：`education.cccp.codebase`
- **工具链**：Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **构建**：`./gradlew build -x test` · **测试**：`./gradlew testAll` · **覆盖率门禁**：`./gradlew koverVerify`（≥80%）

🌐 语言：[English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 模块布局

```
codebase-plugin/
└── src/main/kotlin/
    ├── codebase/
    │   ├── CodebasePlugin.kt          # 插件入口点 —— 注册所有任务
    │   ├── benchmark/                  # LLM 感知基准测试
    │   ├── blog/                       # 会话 → 博客文章稀释
    │   ├── koog/                       # koog DSL 图（vibecoding、自动聚焦、代理）
    │   ├── ocr/                        # 通过 Gemini Vision + Ollama 进行 OCR
    │   ├── quality/                    # ONNX 质量门禁（情感、PII、离题）
    │   ├── rag/                        # RAG pgvector（VectorStore、嵌入）
    │   └── walker/                     # 文件遍历 + 匿名化
    └── vibecoding/                     # Vibecoding 合约（ToolRegistry 等）
```

## N0 合约（来自 workspace-bom MEMPHIS）

| 合约 | 工件 | 提供 |
|----------|----------|----------|
| `codebase-contracts`   | `education.cccp:codebase-contracts:0.0.1`   | ContextChannel, ChannelBudget, CompositeContext |
| `agent-contracts`      | `education.cccp:agent-contracts:0.0.1`      | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`   | `education.cccp:llm-pool-contracts:0.0.1`   | LlmInstancePool, LlmInstance, QuotaConfig |
| `opencode-session-contracts` | `education.cccp:opencode-session-contracts:0.0.1` | SessionPrompt, SessionResponse, AgentContext |
| `i18n-contracts`       | `education.cccp:i18n-contracts:0.0.1`       | SupportedLanguage, LanguageCatalog, I18nConfig |

## N2 依赖

- `codex-plugin` —— 文档索引（PDF/EPUB → pgvector）
- `planner-plugin` —— LLM 提示 SPG/SPD

## 关键库

- **koog-agents** 1.0.0 —— DSL Kotlin StateGraph / ConditionalEdges / Checkpoints
- **langchain4j** 1.16.3 —— LLM 提供商、RAG、嵌入
- **R2DBC** —— 响应式 PostgreSQL (pgvector)
- **Testcontainers** —— `pgvector/pgvector:pg17`
- **Jackson** —— YAML 配置、JSON 序列化
- **Kover** 0.9.8 —— 覆盖率门禁（≥80%）

## Ollama 实例（全局约束）

端口 `11434–11436` 被禁止使用。在 `11437–11465`（29 个端口）上轮换。
授权模型：`gpt-oss:120b-cloud`、`gemma4:31b-cloud`。

## 测试矩阵

| 任务 | 范围 | 超时 |
|------|-------|---------|
| `test` | JUnit5 单元测试 | 默认 |
| `testFast` | 快速 Cucumber（≤ 8 分钟） | 8 分钟 |
| `testAll` | JUnit5 + 所有 Cucumber | — |
| `testEpics` | 所有 EPIC Cucumber BDD | — |
| `cucumberTest*`（48 个任务） | 每个 EPIC 一个 | 8–15 分钟 |

验证任务：`validateDependencies`（CVE-2015-7501 防护 + 重型传递依赖）、`koverVerify`（集成于 `check`）。

## JVM 调优

- **CI**：G1GC + ParallelRefProc + MaxGCPauseMillis=200，堆 2g
- **本地**：SerialGC + TieredStopAtLevel=4，自适应堆（主机 < 8 GB 时 512m，否则 1g）
- **Metaspace**：512m（两种配置）

## 构建命令

```bash
./gradlew build                       # 完整构建（编译 + 测试）
./gradlew build -x test               # 仅编译
./gradlew testFast                    # 快速测试（PR 门禁，≤ 8 分钟）
./gradlew testAll                     # 完整测试套件
./gradlew koverVerify                 # 覆盖率 ≥ 80%
./gradlew validateDependencies        # CVE 审计 + 重型传递依赖
./gradlew publishToMavenLocal         # 本地发布
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## CI 流水线

`.github/workflows/test.yml` 定义了三个作业：
1. **fast-tests** —— 每个 PR 上运行 `./gradlew testFast -x build`（≤ 15 分钟）
2. **full-tests** —— 在 main/master 上运行 `./gradlew testAll`（≤ 45 分钟，上传报告）
3. **publish** —— 在 `v*` 标签上运行 `./gradlew publishAggregationToCentralPortal`（需要 GPG）

## 发布（NMCP）

通过 `settings.gradle.kts` 中的 `com.gradleup.nmcp.settings` (1.5.0) 配置。
凭据从 `~/.gradle/gradle.properties`（`ossrhUsername`、`ossrhPassword`）读取。
签名使用 `useGpgCmd()`；CI 通过 `crazy-max/ghaction-import-gpg@v6` 导入 GPG 密钥。
POM 声明 Apache 2.0、开发者 `cccp-education`、SCM 指向
`github.com/cccp-education/codebase-gradle`。

## EPIC 状态

`0.0.4` 中所有 EPIC 已关闭（见 `.agents/INDEX.adoc`）：
V, V-9, K, L, W, X, Y, Z, OCR, PUB, V-LOCAL, CR, session-proTOCOL, 8, TRAD, V-6-POOL。

## 贡献

1. 构建编译通过：`./gradlew build -x test`
2. 快速测试通过：`./gradlew testFast`
3. 覆盖率达标：`./gradlew koverVerify`
4. 无 CVE 回归：`./gradlew validateDependencies`
5. 遵循 DDD 约定（值对象、端口/适配器、无泄漏）

## 架构文档

- [BUILDING.md](../codebase-plugin/BUILDING.md) —— 构建配置与 JVM 调优
- [STRATEGIC_ROADMAP.adoc](../codebase-plugin/STRATEGIC_ROADMAP.adoc) —— 生态系统概述
- [LOCAL_VIBECODING_LOOP.adoc](../codebase-plugin/LOCAL_VIBECODING_LOOP.adoc) —— 本地开发设置
- [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) —— 使用模式
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) —— EPIC 与治理

## 许可证

Apache License 2.0 —— 见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_