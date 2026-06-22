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
                        email.set("cccp.edu@gmail.com")
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

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
