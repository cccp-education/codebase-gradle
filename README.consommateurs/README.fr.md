<!-- translated from README.md rev 0.0.4 -->
# codebase-gradle — Guide Consommateur

> Plugin Gradle basé sur RAG pour l'indexation de codebase augmentée par LLM et le vibecoding.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/codebase-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/codebase-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.codebase.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.codebase)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/codebase-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/codebase-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/codebase-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.4` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.codebase`
- **Build** : `./gradlew build` · **Tests** : `./gradlew testAll` (JUnit5 + 48 suites Cucumber)
- **Couverture** : ≥ 80 % (Kover, intégré à `check`)

🌐 Langues : [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | **Français** | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Ce que ça fait

`codebase-gradle` indexe les sources de votre projet dans **PostgreSQL + pgvector**, expose
l'augmentation de contexte composite, l'anonymisation, l'OCR (Gemini Vision), les portes qualité
(sentiment + PII + hors-sujet) et le **vibecoding** — une boucle de génération de code
multi-tours pilotée par LLM via `koog-agents`.

Partie de l'écosystème multi-plugins CCCP Education :

```
intention utilisateur → codex-gradle (indexation) → [codebase-gradle] → koog-agents → portes qualité → sortie
```

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.codebase") version "0.0.4"
}
```

### 2. Indexer votre codebase

```bash
./gradlew collectFromCodebase          # contexte par borough → build/context/
./gradlew collectCompositeContext       # composite niveau workspace
```

### 3. Lancer le vibecoding

```bash
./gradlew vibecode \
  --intention="Analyser l'architecture" \
  --dryRun \
  --maxActions=10
```

Voir [VIBECODING_USAGE_GUIDE.adoc](../VIBECODING_USAGE_GUIDE.adoc) pour les options complètes.

## Tâches disponibles

| Tâche | Groupe | Description |
|------|--------|-------------|
| `collectFromCodebase`      | collect   | Contexte augmenté par borough (EAGER/RAG/Graphify) |
| `collectCompositeContext`  | collect   | Composite niveau workspace depuis tous les boroughs |
| `generateCompositeContext` | generate  | Composite N1+N2 (codex + training) → JSON |
| `generatePlan`             | generate  | Planification augmentée — classer l'intention → EPICs/US/Tasks |
| `vibecode`                 | generate  | Boucle autonome Koog (contexte → plan → exécution → audit) |
| `sessionProtocolDaemon`    | generate  | stdin JSON-lines SessionPrompt → stdout SessionResponse |
| `ingestGovernance`         | generate  | Ingérer les fichiers EAGER (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) |
| `vibecodingDashboard`      | tracking  | Résumé de session, coûts tokens, filtres confidentialité |
| `qualityGate`              | validate  | Contrôles sentiment + hors-sujet + PII résiduel |
| `ocrDocument`              | collect   | OCR via Gemini Vision → AsciiDoc |
| `ocrIngest`                | collect   | Ingérer la sortie OCR dans pgvector |
| `exposeExperts`            | generate  | Manifeste experts JSON pour slider/plantuml/bakery |
| `endSessionBlog`           | generate  | Extraire les sessions → articles blog AsciiDoc |

## DSL d'extension

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

## Prérequis

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **PostgreSQL** 15+ avec l'extension `pgvector`
- **Docker** (pour Testcontainers)

## Build et tests

```bash
./gradlew build                    # build complet
./gradlew testFast                 # Cucumber rapide (≤ 8 min)
./gradlew testAll                  # suite complète (JUnit5 + tout Cucumber)
./gradlew testEpics               # tout Cucumber BDD EPICs
./gradlew testHelp                 # lister toutes les tâches de test
./gradlew validateDependencies    # audit CVE + validation dépendances
./gradlew publishToMavenLocal      # publier localement
```

## Dépannage

| Symptôme | Solution |
|----------|----------|
| `Java heap space`        | `export GRADLE_OPTS="-Xmx2g"` |
| Conteneur Postgres bloqué | `docker rm -f postgres-*` puis réessayer |
| Timeout de test           | vérifier `docker ps`, augmenter le heap, vérifier la latence LLM API |

Voir [BUILDING.md](../codebase-plugin/BUILDING.md) pour les détails.

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._