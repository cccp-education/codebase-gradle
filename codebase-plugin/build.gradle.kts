import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import java.time.Duration

fun isCI() = System.getenv("CI") == "true"

plugins {
    signing
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    // Gradle 9.5.1 : alias(libs.plugins.kotlin.jvm) et publish hors scope dans plugins {} d'un sous-projet
    // Workaround : versions explicites
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("com.gradle.plugin-publish") version "2.1.0"
}

kotlin.jvmToolchain(24)

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.bundles.langchain4j.rag)
    implementation(libs.googleAiGemini)
    implementation(libs.bundles.r2dbc)
    implementation(libs.codex.plugin)
    implementation(libs.planner.plugin)

    // N0 codebase contracts — source unique de vérité (ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig)
    implementation(libs.codebase.contracts)
    // N0 agent contracts — Epic, UserStory, GradleTask, AgentState (partagés cross-borough)
    implementation(libs.agent.contracts)
    // N0 vibecoding contracts — ToolRegistry, ExecShellTool, ExecGradleTool, ToolkitIsMissingException
    implementation(libs.vibecoding.contracts)
    // N0 llm-pool contracts — LlmInstancePool, LlmInstance, QuotaConfig, RotationStrategy (shared N1→N2)
    implementation(libs.llm.pool.contracts)
    // N0 opencode-session contracts — SessionPrompt, SessionResponse, AgentContext, SessionStatus, TokenUsage, ToolCallRecord
    implementation(libs.opencode.session.contracts)
    implementation(libs.bundles.arrow)
    implementation(libs.koog.agents) {
        // Exclusion nécessaire : koog 26.0.2-1 conflict with Kotlin embedded 13.0
        // quand codebase-plugin est appliqué comme plugin par codex-gradle
        exclude(group = "org.jetbrains", module = "annotations")
    }
    // ── Résolution conflit annotations ──────────────────────────────────────────────
    // Kotlin 2.3.20 pinne annotations:13.0 (strictly) dans le classpath Gradle.
    // koog-agents 0.8.0 → koog-utils-jvm → annotations:26.0.2-1.
    // L'exclusion ci-dessus bloque le chemin direct koog-agents, mais annotations
    // revient par d'autres transitives koog (prompt-llm, http-client-core, etc.)
    // ET par kotlin-stdlib (13.0) + kotlinx-coroutines (23.0.0) + flexmark (24.0.1).
    // Solution : contrainte globale → toutes les transitives forcées à 13.0.
    // Publiée dans le .module Gradle Metadata, respectée par tous les consommateurs N2.
    constraints {
        implementation("org.jetbrains:annotations:13.0") {
            because("Kotlin 2.3.20 embed — évite conflit koog-agents 26.0.2-1 dans les plugins N2 consommateurs")
        }
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
    useJUnitPlatform()
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(gradlePlugin.plugins.getByName("codebase").displayName)
                description.set(gradlePlugin.plugins.getByName("codebase").description)
                url.set(gradlePlugin.website.get())
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.education@gmail.com")
                    }
                }
                scm {
                    connection.set(gradlePlugin.vcsUrl.get())
                    developerConnection.set(gradlePlugin.vcsUrl.get())
                    url.set(gradlePlugin.vcsUrl.get())
                }
                project.findProperty("relocationGroup")?.let { targetGroup ->
                    withXml {
                        val pom = asElement()
                        val doc = pom.ownerDocument
                        val distMgmt = doc.createElement("distributionManagement")
                        val relocation = doc.createElement("relocation")
                        relocation.appendChild(doc.createElement("groupId")).also { it.textContent = targetGroup.toString() }
                        relocation.appendChild(doc.createElement("artifactId")).also { it.textContent = project.name }
                        distMgmt.appendChild(relocation)
                        pom.appendChild(distMgmt)
                    }
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

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
                minBound(80)
            }
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
        require(annotationsVersion == "13.0") {
            "Annotations version mismatch: expected 13.0, got $annotationsVersion. " +
            "Check for koog-agents upgrade conflicts."
        }

        val cveVulnerable = resolved.filter {
            it.moduleVersion.id.group == "commons-collections" &&
            it.moduleVersion.id.name == "commons-collections" &&
            it.moduleVersion.id.version.startsWith("3.")
        }
        require(cveVulnerable.isEmpty()) {
            "CVE-2015-6420: commons-collections 3.x detected in classpath: " +
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

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
