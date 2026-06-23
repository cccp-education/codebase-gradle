<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Guia do Consumidor

> Plugin Gradle baseado em RAG para indexação de código-base aumentada por LLM e vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Testes)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licença)](../LICENSE)

- **Versão**: `0.0.4` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.codebase`
- **Build**: `./gradlew build` · **Testes**: `./gradlew testAll` (JUnit5 + 48 suítes Cucumber)
- **Cobertura**: ≥ 80 % (Kover, integrado em `check`)

🌐 Idiomas: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

O `codebase-gradle` indexa os arquivos-fonte do seu projeto em **PostgreSQL + pgvector**, expõe
aumento de contexto composto, anonimização, OCR (Gemini Vision), portões de qualidade
(sentimento + PII + fora do tópico) e **vibecoding** — um ciclo de geração de código multiturno
impulsionado por LLM via `koog-agents`.

Parte do ecossistema multi-plugin da CCCP Education:

```
intenção do usuário → codex-gradle (indexação) → [codebase-gradle] → koog-agents → portões de qualidade → saída
```

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Indexar sua base de código

```bash
./gradlew collectFromCodebase          # contexto por distrito → build/context/
./gradlew collectCompositeContext       # composto em nível de workspace
```

### 3. Executar vibecoding

```bash
./gradlew vibecode \
  --intention="Analisar a arquitetura" \
  --dryRun \
  --maxActions=10
```

Consulte [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) para opções completas.

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|------|-------|-------------|
| `collectFromCodebase`      | collect   | Contexto aumentado por distrito (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | Composto em nível de workspace de todos os distritos |
| `generateCompositeContext` | generate  | Composto N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | Planejamento aumentado — classificar intenção → EPICs/US/Tasks |
| `vibecode`                 | generate  | Ciclo autônomo Koog (contexto → plano → execução → auditoria) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | Ingerir arquivos EAGER (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | Resumo de sessão, custos de tokens, filtros de privacidade |
| `qualityGate`              | validate  | Verificações de sentimento + fora do tópico + PII residual |
| `ocrDocument`              | collect   | OCR via Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | Ingerir saída OCR no pgvector |
| `exposeExperts`            | generate  | Manifesto de especialistas JSON para slider/plantuml/bakery |
| `endSessionBlog`           | generate  | Extrair sessões → artigos de blog AsciiDoc |

## DSL de extensão

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

## Pré-requisitos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ com extensão `pgvector`
- **Docker** (para Testcontainers)

## Build e testes

```bash
./gradlew build                    # build completo
./gradlew testFast                 # Cucumber rápido (≤ 8 min)
./gradlew testAll                  # suíte completa (JUnit5 + todo Cucumber)
./gradlew testEpics               # todo Cucumber BDD de EPICs
./gradlew testHelp                 # listar todas as tarefas de teste
./gradlew validateDependencies    # auditoria CVE + validação de dependências
./gradlew publishToMavenLocal      # publicar localmente
```

## Solução de problemas

| Sintoma | Solução |
|---------|---------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Contêiner Postgres travado | `docker rm -f postgres-*` e tentar novamente |
| Timeout de teste           | verificar `docker ps`, aumentar heap, checar latência da API LLM |

Consulte [BUILDING.md](../codebase-plugin/BUILDING.md) para detalhes.

## Licença

Apache License 2.0 — consulte [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._