import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.publish.maven.MavenPublication
import java.time.Duration

fun isCI() = System.getenv("CI") == "true"

group = "education.cccp"
version = "0.0.8"

plugins {
    `java-library`
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("education.cccp.build.gradle-plugin") version "0.0.2"
    id("com.gradle.plugin-publish") version "2.1.0"
    id("education.cccp.build.publishing") version "0.0.2"
}

publishingConventions {
    publicationType = "PLUGIN"
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Codebase Plugin")
            description.set("Codebase RAG — indexes project source files into pgvector, exposes composite context augmentation, anonymization, benchmark, and STIMULUS cascade tasks.")
        }
    }
    repositories {
        maven {
            name = "GradlePluginPortal"
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

dependencies {
    implementation(platform(libs.workspace.bom))
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    api(libs.langchain4j)
    implementation(libs.langchain4j.pgvector)
    implementation(libs.langchain4j.minilm)
    implementation(libs.langchain4j.ollama)
    implementation(libs.googleAiGemini)
    implementation(libs.bundles.r2dbc)
    // EPIC CDX-RAG-4 FINAL (S-104): `libs.codex.plugin` REMOVED — the N1->N2
    // inversion is fully dead. The OCR boundary port (VisionOcrEngineAdapter)
    // now consumes the N0 `ocr-contracts` artifact (EPIC CDX-OCR-CONTRACTS
    // US-3): both codex (N2) and codebase (N1) depend on N0, no cycle.
    implementation("education.cccp:ocr-contracts:0.0.1")
    implementation(libs.planner.plugin)
    implementation(libs.graphify.plugin)

    // N0 codebase contracts — source unique de vérité (ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig)
    implementation(libs.codebase.contracts)
    // N0 agent contracts — Epic, UserStory, GradleTask, AgentState (partagés cross-borough)
    implementation(libs.agent.contracts)
    // N0 llm-pool contracts — LlmInstancePool, LlmInstance, QuotaConfig, RotationStrategy (shared N1→N2)
    implementation(libs.llm.pool.contracts)
    // N0 opencode-session contracts — AgentContext, SessionStatus (SessionProtocol, LiveContextInjector)
    implementation(libs.opencode.session.contracts)
    // N0 i18n contracts — SupportedLanguage, LanguageCatalog, I18nConfig (TranslationService)
    implementation(libs.i18n.contracts)
    implementation(libs.bundles.arrow)
    implementation(libs.koog.agents) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // vibecoding-contracts now lives in codebase source tree: cccp.vibecoding.contracts
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.testcontainers.postgresql)


    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertj.core)
    runtimeOnly(libs.logback.classic)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit5)
    testImplementation(libs.bundles.cucumber)
}

tasks.named("pluginUnderTestMetadata").configure { dependsOn("jar") }
tasks.named("validatePlugins").configure { dependsOn("jar") }

data class CucumberTaskSpec(
    val taskName: String,
    val description: String,
    val runnerClass: String? = null,
    val timeoutMinutes: Long = 15,
)

fun registerCucumberTask(spec: CucumberTaskSpec): TaskProvider<Test> {
    val maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)
    val adaptiveHeap = when {
        isCI() -> "2g"
        maxMemoryMB < 8192 -> "512m"
        else -> "1g"
    }

    return tasks.register<Test>(spec.taskName) {
        description = spec.description
        group = "verification"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = configurations.testRuntimeClasspath.get() +
            sourceSets.test.get().output +
            sourceSets.main.get().output +
            files(tasks.jar.get().archiveFile)

        dependsOn(tasks.classes)
        useJUnitPlatform { excludeEngines("junit-jupiter") }
        systemProperty("cucumber.junit-platform.naming-strategy", "long")
        maxHeapSize = adaptiveHeap
        maxParallelForks = 1
        forkEvery = 100
        timeout.set(Duration.ofMinutes(spec.timeoutMinutes))

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            exceptionFormat = FULL
        }
        outputs.upToDateWhen { false }

        spec.runnerClass?.let { filter { includeTestsMatching(it) } }
    }
}

val cucumberTaskSpecs = listOf(
    CucumberTaskSpec("cucumberTest", "Runs Cucumber BDD tests (EPIC 9 — pgvector infra)"),
    CucumberTaskSpec("cucumberTestEpicV6", "Runs Cucumber BDD tests — EPIC V-6 (Feedback Loop — error→replan→retry) only", "codebase.scenarios.EpicV6CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV7", "Runs Cucumber BDD tests — EPIC V-7 (Resume Session) only", "codebase.scenarios.EpicV7CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicL3", "Runs Cucumber BDD tests — EPIC L-3 (KoogAugmentedContextGraph) only", "codebase.scenarios.EpicL3CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV8", "Runs Cucumber BDD tests — EPIC V-8 (DashboardTask) only", "codebase.scenarios.EpicV8CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicVPool", "Runs Cucumber BDD tests — EPIC V-Pool (Ollama Pool GPT-OSS-120B rotation/quota/failover)", "codebase.scenarios.EpicVPoolCucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicOcr", "Runs Cucumber BDD tests — EPIC OCR (Gemini Vision) only", "codebase.scenarios.OcrCucumberRunner", 8),
    CucumberTaskSpec("cucumberTestEpicOcrIngest", "Runs Cucumber BDD tests — EPIC OCR-4 Ingest (chunk→embed→pgvector) only", "codebase.scenarios.OcrIngestCucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicOcr45", "Runs Cucumber BDD tests — EPIC OCR-4.5 (Métriques OCR) only", "codebase.scenarios.OcrMetricsCucumberRunner", 8),
    CucumberTaskSpec("cucumberTestEpicY3", "Runs Cucumber BDD tests — EPIC Y-3 (AgenticSchema pgvector) only", "codebase.scenarios.EpicY3CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicY4", "Runs Cucumber BDD tests — EPIC Y-4 (AgenticCompiler) only", "codebase.scenarios.EpicY4CucumberRunner", 8),
    CucumberTaskSpec("cucumberTestEpicY5", "Runs Cucumber BDD tests — EPIC Y-5 (AgenticIngestor) only", "codebase.scenarios.EpicY5CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicY6", "Runs Cucumber BDD tests — EPIC Y-6 (AgenticExternalImporter) only", "codebase.scenarios.EpicY6CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicY7", "Runs Cucumber BDD tests — EPIC Y-7 (AgenticChunkEnforcement) only", "codebase.scenarios.EpicY7CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicW4", "Runs Cucumber BDD tests — EPIC W-4 (List Tasks) only", "codebase.scenarios.EpicW4CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicX1", "Runs Cucumber BDD tests — EPIC X-1 (VibecodingPlan) only", "codebase.scenarios.EpicX1CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicX2", "Runs Cucumber BDD tests — EPIC X-2 (TaskResultVerifier) only", "codebase.scenarios.EpicX2CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicX4", "Runs Cucumber BDD tests — EPIC X-4 (RollbackStrategy) only", "codebase.scenarios.EpicX4CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicX5", "Runs Cucumber BDD tests — EPIC X-5 (Replan Catalog) only", "codebase.scenarios.EpicX5CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicX6", "Runs Cucumber BDD tests — EPIC X-6 (E2E Plan→Fail→Adapt→Succeed) only", "codebase.scenarios.EpicX6CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp1", "Runs Cucumber BDD tests — EPIC SP-1 (SessionProtocol) only", "codebase.scenarios.EpicSp1CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicZ7", "Runs Cucumber BDD tests — EPIC Z-7 (Cross-Borough Autofocus) only", "codebase.scenarios.EpicZ7CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpic12", "Runs Cucumber BDD tests — EPIC 12 (Blog Narration Publique) only", "codebase.scenarios.Epic12CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp2", "Runs Cucumber BDD tests — EPIC SP-2 (Session Protocol Server Daemon) only", "codebase.scenarios.EpicSp2CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp3", "Runs Cucumber BDD tests — EPIC SP-3 (Session Lifecycle) only", "codebase.scenarios.EpicSp3CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp4", "Runs Cucumber BDD tests — EPIC SP-4 (E2E opencode→runner→codebase→session) only", "codebase.scenarios.EpicSp4CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp5", "Runs Cucumber BDD tests — EPIC SP-5 (ToolEventStream) only", "codebase.scenarios.EpicSp5CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSp6", "Runs Cucumber BDD tests — EPIC SP-6 (LiveContextInjector) only", "codebase.scenarios.EpicSp6CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpic7", "Runs Cucumber BDD tests — EPIC 7 (Expert Dispatcher) only", "codebase.scenarios.Epic7CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpic8", "Runs Cucumber BDD tests — EPIC 8 (Expert Exposure via Ollama) only", "codebase.scenarios.Epic8CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicVLocal", "Runs Cucumber BDD tests — EPIC V-LOCAL (Governance fallback auto-loading) only", "codebase.scenarios.EpicVLocalCucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicTranslation", "Runs Cucumber BDD tests — EPIC TRAD (TranslationService cross-borough) only", "codebase.scenarios.EpicTranslationCucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV93", "Runs Cucumber BDD tests — EPIC V-9.3 (ScanAgent) only", "codebase.scenarios.EpicV93CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV95", "Runs Cucumber BDD tests — EPIC V-9.5 (ChunkValidator) only", "codebase.scenarios.EpicV95CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV97", "Runs Cucumber BDD tests — EPIC V-9.7 (GovernanceOntologizer) only", "codebase.scenarios.EpicV97CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV98", "Runs Cucumber BDD tests — EPIC V-9.8 (Governance ingestion report) only", "codebase.scenarios.EpicV98CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV99", "Runs Cucumber BDD tests — EPIC V-9.9 (ChunkValidationGate) only", "codebase.scenarios.EpicV99CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV910", "Runs Cucumber BDD tests — EPIC V-9.10 (ChunkValidationReport) only", "codebase.scenarios.EpicV910CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV911", "Runs Cucumber BDD tests — EPIC V-9.11 (AgenticCompiler) only", "codebase.scenarios.EpicV911CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV913", "Runs Cucumber BDD tests — EPIC V-9.13 (ToolRegistry enforcement hook) only", "codebase.scenarios.EpicV913CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV914", "Runs Cucumber BDD tests — EPIC V-9.14 (Auto-activation governance enforcement hook) only", "codebase.scenarios.EpicV914CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV915", "Runs Cucumber BDD tests — EPIC V-9.15 (Invalid chunk quarantine) only", "codebase.scenarios.EpicV915CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV916", "Runs Cucumber BDD tests — EPIC V-9.16 (Strict validation mode) only", "codebase.scenarios.EpicV916CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV917", "Runs Cucumber BDD tests — EPIC V-9.17 (Ingestion Summary DSL) only", "codebase.scenarios.EpicV917CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV918", "Runs Cucumber BDD tests — EPIC V-9.18 (Compile governance chunks to Gradle tasks) only", "codebase.scenarios.EpicV918CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV919", "Runs Cucumber BDD tests — EPIC V-9.19 (Auto-detection of new governance files) only", "codebase.scenarios.EpicV919CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicV920", "Runs Cucumber BDD tests — EPIC V-9.20 (Chunk diff incremental between sessions) only", "codebase.scenarios.EpicV920CucumberRunner"),
    CucumberTaskSpec("cucumberTestEpicSld8", "Runs Cucumber BDD tests — EPIC SLD-8 US-8.1 (LlmBuildService Gradle bridge) only", "codebase.scenarios.EpicSld8LlmServiceCucumberRunner"),
    CucumberTaskSpec("cucumberTestSubgraph", "Runs Cucumber BDD tests — EPIC SUBGRAPH (real Graphify subgraph in augmented context) only", "codebase.scenarios.SubgraphCucumberRunner"),
    CucumberTaskSpec("cucumberTestVibeHardening", "Runs Cucumber BDD tests — EPIC VIBE-HARDENING (allowlist deny-by-default, LLM timeout, single retry counter) only", "codebase.scenarios.VibeHardeningCucumberRunner"),
    CucumberTaskSpec("cucumberTestVibeHardening2", "Runs Cucumber BDD tests — EPIC VIBE-HARDENING-2 (write_file size guard, expectedOutput, flag+value, remaining coerce) only", "codebase.scenarios.VibeHardening2CucumberRunner"),
    CucumberTaskSpec("cucumberTestFineTuning", "Runs Cucumber BDD tests — EPIC FT-PIPELINE US-5 (fine-tuning N1 pipeline — dataset→GGUF→manifest, iterative convergence, degraded fallback, validation threshold) only", "codebase.scenarios.FineTuningCucumberRunner"),
    CucumberTaskSpec("cucumberTestRagSocle", "Runs Cucumber BDD tests — EPIC CDX-RAG-SOCLE US-5 (RagVectorStore socle + composite context Docs channel, fake in-memory) only", "codebase.scenarios.RagSocleCucumberRunner"),
)

val cucumberTasks = cucumberTaskSpecs.map { registerCucumberTask(it) }

tasks.register("testAll") {
    description = "Runs all tests (JUnit5 + Cucumber BDD)"
    group = "verification"
    dependsOn(tasks.test, cucumberTasks)
}

tasks.register("testEpics") {
    description = "Runs all EPIC Cucumber BDD tests"
    group = "verification"
    dependsOn(cucumberTasks)
}

tasks.register("testFast") {
    description = "Runs fast Cucumber BDD tests (timeout <= 8 min)"
    group = "verification"
    val fastSpecs = cucumberTaskSpecs.filter { it.timeoutMinutes <= 8 }
    val fastTasks = fastSpecs.map { spec ->
        cucumberTasks.find { it.name == spec.taskName } ?: registerCucumberTask(spec)
    }
    dependsOn(fastTasks)
}

tasks.register("testHelp") {
    description = "Lists all available test tasks with descriptions"
    group = "help"
    doLast {
        logger.lifecycle("=== Test Tasks ===")
        logger.lifecycle("")
        logger.lifecycle("testAll   — Runs all tests (JUnit5 + Cucumber BDD)")
        logger.lifecycle("testEpics — Runs all EPIC Cucumber BDD tests")
        logger.lifecycle("testFast  — Runs fast Cucumber BDD tests (timeout <= 8 min)")
        logger.lifecycle("test      — Runs JUnit5 unit tests only")
        logger.lifecycle("")
        logger.lifecycle("=== Cucumber EPIC Tasks ===")
        cucumberTaskSpecs.forEach { spec ->
            logger.lifecycle("${spec.taskName.padEnd(30)} — ${spec.description} (${spec.timeoutMinutes} min)")
        }
    }
}

tasks.withType<Test>().configureEach {
    ignoreFailures = !isCI()
    useJUnitPlatform {
        excludeTags("integration")
    }
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    if (isCI()) {
        jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-XX:+ParallelRefProcEnabled")
    } else {
        jvmArgs("-XX:+UseSerialGC", "-XX:TieredStopAtLevel=4")
    }
    jvmArgs("-XX:MaxMetaspaceSize=512m")
}

gradlePlugin {
    website.set("https://github.com/cccp-education/codebase-gradle/")
    vcsUrl.set("https://github.com/cccp-education/codebase-gradle.git")

    plugins {
        create("codebase") {
            id = libs.plugins.codebase.get().pluginId
            implementationClass = "codebase.CodebasePlugin"
            displayName = "Codebase Plugin"
            description = """
                Codebase RAG — indexes project source files into pgvector,
                exposes composite context augmentation, anonymization,
                benchmark, and STIMULUS cascade tasks.
            """.trimIndent()
            tags.set(listOf("rag", "pgvector", "langchain4j", "anonymization", "benchmark"))
        }
    }
}

tasks.register("validateDependencies") {
    description = "Validates dependency conflict resolution + CVE audit + heavy transitive report"
    group = "verification"
    doLast {
        val resolved = configurations.runtimeClasspath.get()
            .resolvedConfiguration
            .resolvedArtifacts

        val annotationsVersion = resolved
            .find { it.moduleVersion.id.name == "annotations" }
            ?.moduleVersion
            ?.id
            ?.version
        logger.lifecycle("annotations resolved: $annotationsVersion")

        val cveVulnerable = resolved.filter {
            it.moduleVersion.id.group == "commons-collections" &&
            it.moduleVersion.id.name == "commons-collections" &&
            it.moduleVersion.id.version.startsWith("3.")
        }
        require(cveVulnerable.isEmpty()) {
            "CVE-2015-7501: commons-collections 3.x (InvokerTransformer deserialization RCE) detected in classpath: " +
            cveVulnerable.joinToString { "${it.moduleVersion.id}" } +
            ". Replace with commons-collections4:4.4."
        }

        val heavyTransitives = resolved
            .filter { it.moduleVersion.id.group != "education.cccp" }
            .groupBy { it.moduleVersion.id.group }
            .mapValues { (_, artifacts) -> artifacts.distinctBy { it.moduleVersion.id.name }.size }
            .filter { it.value >= 5 }
        if (heavyTransitives.isNotEmpty()) {
            logger.lifecycle("Heavy transitive groups (>=5 modules):")
            heavyTransitives.forEach { (group, count) ->
                logger.lifecycle("  $group: $count modules")
            }
        }
    }
}

tasks.check { dependsOn("koverVerify", "validateDependencies") }

kover {
    currentProject {
        sources {
            excludedSourceSets.add("test")
        }
    }
    reports {
        total {
            html {
                onCheck.set(false)
                htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
            }
            xml {
                onCheck.set(false)
                xmlFile.set(layout.buildDirectory.file("reports/kover/xml/report.xml"))
            }
        }
        verify {
            rule {
                minBound(79)
            }
        }
    }
}

