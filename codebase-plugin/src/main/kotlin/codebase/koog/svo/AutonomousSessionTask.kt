package codebase.koog.svo

import codebase.koog.KoogAugmentedContextGraph
import codebase.koog.VibecodingGraph
import codebase.koog.agentic.GovernanceEnforcementWirer
import codebase.koog.discovery.GradleTaskArgsMapper
import codebase.koog.discovery.SchemaCache
import codebase.koog.discovery.TaskDiscoveryRegistrar
import codebase.koog.discovery.TaskSchemaScanner
import contracts.vibecoding.registry.ToolInfo
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.slf4j.LoggerFactory
import java.io.File

/**
 * EPIC SVO-4 (D6) — Gradle task `autonomousSession`: chains augmented-context
 * vibecoding sessions until the borough BACKLOG is complete, bounded by
 * `--maxChainedSessions` (D9, default 3).
 *
 * Pipeline (D6): resolve prompt (`--prompt` > borough default via
 * [SessionPromptResolver]) → plan via the wired
 * [codebase.koog.plannerport.PlannerPort] (adapter branché par wiring Gradle
 * côté planner, SVO-3) → vibecoding loop (`VibecodingGraph.execute`) →
 * session state (audit JSONL) → resumption prompt (SVO-2 planner) written
 * under `build/vibecoding/` → conditional chain-up: complete backlog →
 * BRAINSTORMING session (D8).
 *
 * Usage:
 * ```
 * ./gradlew autonomousSession --prompt="Work EPIC SVO-4" --maxChainedSessions=3
 * ./gradlew autonomousSession   # borough default prompt (PROMPT_REPRISE + backlog)
 * ```
 */
@DisableCachingByDefault(because = "Vibecoding LLM agent — non-deterministic LLM calls, non-cacheable")
abstract class AutonomousSessionTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(AutonomousSessionTask::class.java)

    @get:Input
    @get:Optional
    @get:Option(option = "prompt", description = "Session prompt transferred from the CLI — blank means borough default")
    abstract val prompt: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "borough", description = "Borough name used for the default prompt and brainstorming intention")
    abstract val borough: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "maxChainedSessions", description = "Anti-infinite-loop guard — maximum chained sessions (default 3, D9)")
    abstract val maxChainedSessions: Property<Int>

    @get:Input
    @get:Optional
    @get:Option(option = "maxActions", description = "Maximum actions per vibecoding session")
    abstract val maxActions: Property<Int>

    @get:Input
    @get:Optional
    @get:Option(option = "model", description = "LLM model for the loop (reserved for LlmProviderResolver wiring)")
    abstract val model: Property<String>

    @get:Internal
    abstract val workspaceRoot: DirectoryProperty

    init {
        group = "generate"
        description = "Autonomous vibecoding session — chains augmented-context sessions until the borough backlog is complete (bounded by maxChainedSessions)"
        prompt.convention("")
        borough.convention("")
        maxChainedSessions.convention(3)
        maxActions.convention(10)
        model.convention("")
    }

    @TaskAction
    fun executeAutonomousSession() {
        val root = workspaceRoot.asFile.getOrElse(project.rootDir)
        val boroughName = borough.getOrElse("").takeIf { it.isNotBlank() } ?: root.name
        val cliPrompt = prompt.getOrElse("").takeIf { it.isNotBlank() }
        val loop = buildVibecodingLoop(root)

        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = boroughName,
            prompt = cliPrompt,
            projectDir = root,
            maxChainedSessions = maxChainedSessions.get(),
            maxActionsPerSession = maxActions.get(),
            runSession = { state ->
                val result = loop.execute(state)
                if (result.error != null) throw RuntimeException("Vibecoding failed: ${result.error}")
                "finished=${result.finished} executed=${result.executedTasks.joinToString(",")}"
            },
            auditAppend = { line -> appendAudit(root, line) },
        )

        logger.lifecycle("[autonomousSession] borough={} sessions={} stop={}", report.borough, report.sessionsExecuted, report.stopReason)
        report.brainstormingIntention?.let {
            logger.lifecycle("[autonomousSession] Brainstorming chain-up intention: {}", it)
        }
        logger.lifecycle("[autonomousSession] Resumption prompt written to {}", report.resumptionPromptPath)
    }

    /**
     * Builds the vibecoding loop exactly like [codebase.koog.VibecodingTask]:
     * auto-registered gradle_* tools + structured args mapping + governance hook
     * + planner branché via `KoogAugmentedContextGraph` (port résolu au wiring).
     */
    private fun buildVibecodingLoop(root: File): VibecodingGraph {
        val toolRegistry = GovernanceEnforcementWirer.wire(ToolRegistry(), root)

        val scanner = TaskSchemaScanner(project)
        TaskDiscoveryRegistrar(scanner, toolRegistry).registerAll()

        val cache = SchemaCache(project)
        val schemas = cache.schemas()
        val argsMapper = GradleTaskArgsMapper(schemas, root.absolutePath)
        for (schema in schemas) {
            toolRegistry.registerHandler("gradle_${schema.name}") { toolName, arguments, workspaceRootArg ->
                argsMapper.execute(toolName, arguments, workspaceRootArg)
            }
        }
        toolRegistry.register(
            ToolInfo(
                "list_tasks",
                "List available Gradle tasks with descriptions and options. Use 'group' arg to filter by task group, 'keyword' arg to search by name or description.",
            )
        )
        toolRegistry.registerHandler("list_tasks") { _, arguments, _ ->
            val group = arguments["group"].takeUnless { it.isNullOrBlank() }
            val keyword = arguments["keyword"].takeUnless { it.isNullOrBlank() }
            codebase.koog.discovery.TaskListFormatter.format(cache.schemas(), group = group, keyword = keyword)
        }

        log.info("[autonomousSession] vibecoding loop built — {} gradle_* tools", schemas.size)

        return VibecodingGraph(
            augmentedGraph = KoogAugmentedContextGraph(),
            toolRegistry = toolRegistry,
            llmProvider = null,
        )
    }

    private fun appendAudit(root: File, line: String) {
        val auditDir = File(root, "build/vibecoding")
        auditDir.mkdirs()
        File(auditDir, "audit.jsonl").appendText(line)
    }
}
