package codebase.koog

import codebase.koog.discovery.TaskListFormatter
import codebase.koog.discovery.TaskSchema
import codebase.koog.llm.LlmProvider
import codebase.koog.planning.RollbackStrategy
import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.StepVerifier
import codebase.koog.planning.TaskResult
import codebase.koog.planning.TaskResultVerifier
import codebase.koog.planning.TaskVerdict
import codebase.koog.planning.VibecodingPlan
import codebase.koog.planning.VibecodingStep
import codebase.koog.session.SessionRecord
import codebase.koog.session.SessionRepository
import codebase.koog.tracking.TokenTracker
import contracts.vibecoding.registry.ToolRegistry
import io.r2dbc.spi.ConnectionFactory
import vibecoding.contracts.state.AugmentedState
import vibecoding.contracts.state.VibecodingState
import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Graphe koog d'exécution vibecoding — pipeline autonome.
 *
 * Architecture (pattern koog+langchain4j) — V-5 enriched :
 * ```
 * buildContext → callLLM → executeTools → persistState → checkProgress ─┬─→ callLLM (↺)
 *                                                                        └─→ finish
 * ```
 *
 * - **buildContext** : appelle KoogAugmentedContextGraph (RAG/pgvector + classification + plan)
 * - **callLLM** : décision LLM — quelle tâche exécuter (Gemini/Ollama/Fake)
 * - **executeTools** : exécute l'action décidée via ToolRegistry
 * - **persistState** : sauvegarde l'état via SessionRepository
 * - **checkProgress** : vérifie si fini (maxActions, erreur, plan vide) ou continue la boucle
 *
 * koog orchestre, langchain4j exécute (RAG/LLM). ToolRegistry pour les actions filesystem/shell.
 *
 * Rétrocompatibilité : si llmProvider est null → pas de callLLM (mode déterministe comme avant V-4).
 * Si sessionRepository ET connectionFactory sont null → pas de persistance.
 * Si connectionFactory est fourni → création automatique d'un SessionRepository interne.
 *
 * === EPIC X Audit (Session 101 — X-0) : Points d'insertion verify→adapt ===
 *
 * Flux actuel (linéaire, une action à la fois) :
 *   buildContext → [callLLM | executeTools] → persistState → checkProgress → (↺)
 *
 * Gaps identifiés pour le pattern multi-step verify→adapt :
 *
 * [INSERT-X1] VibecodingPlan structuré :
 *   Remplacer `Plan` (Epic→UserStory→GradleTask plat) par `VibecodingPlan`
 *   (steps ordonnés avec expectedOutput, maxRetries, verifyHook, dépendances).
 *   Point d'insertion : buildContextNode() — le plan généré doit être un VibecodingPlan.
 *   Impact : VibecodingState.plan passe de `Plan?` à `VibecodingPlan?`.
 *
 * [INSERT-X2] TaskResultVerifier :
 *   Après executeToolsNode() (ligne ~166), insérer verifyStepNode() qui parse
 *   la sortie stdout/stderr et détermine SUCCESS/FAILED/BLOCKED/UNKNOWN.
 *   Actuellement : seule l'erreur (exception) est captée, pas le résultat sémantique.
 *
 * [INSERT-X3] Boucle verify→adapt :
 *   Remplacer la V-6 feedback loop (error→replan→retry, lignes 174-207) par
 *   une boucle structurée : executeStep → verifyResult → [nextStep | adapt | rollback].
 *   Le LLM reçoit le résultat brut + les fichiers modifiés + le plan initial.
 *
 * [INSERT-X4] RollbackStrategy :
 *   checkProgressNode() (ligne 451) — ajouter STOP_ON_ERROR / REVERT_AND_CONTINUE /
 *   MARK_SKIPPED / FALLBACK_HUMAN. Actuellement : seule erreur fatale (finished=true).
 *
 * [INSERT-X5] Intégration W (TaskSchemaScanner) :
 *   callLLMNode() (ligne 297) — le LLM utilise le catalogue list_tasks (W-4)
 *   pour construire son plan multi-step avant d'exécuter.
 *
 * [INSERT-X6] Tests Cucumber :
 *   Scénario "agent planifie, échoue, adapte, réussit" avec projet test buggé.
 */
class VibecodingGraph(
    val augmentedGraph: KoogAugmentedContextGraph? = null,
    val toolRegistry: ToolRegistry = ToolRegistry(),
    val llmProvider: LlmProvider? = null,
    val sessionRepository: SessionRepository? = null,
    val connectionFactory: ConnectionFactory? = null,
    val tokenTracker: TokenTracker = TokenTracker(),
    val stepVerifier: StepVerifier = StepVerifier(llmProvider, tokenTracker),
    val rollbackExecutor: RollbackStrategyExecutor? = null,
    val taskSchemas: List<TaskSchema> = emptyList()
) {

    private val log = LoggerFactory.getLogger(VibecodingGraph::class.java)

    /** SessionRepository effectif : priorité injection explicite, sinon création depuis ConnectionFactory */
    private val effectiveSessionRepository: SessionRepository? by lazy {
        sessionRepository ?: connectionFactory?.let { SessionRepository(it) }
    }

    private var sessionId: String? = null

    /**
     * Graphe koog déclaratif — 5 nœuds (V-5 : callLLM + persistState ajoutés).
     */
    val graph: AIAgentGraphStrategy<VibecodingState, VibecodingState> = strategy<VibecodingState, VibecodingState>(
        name = "vibecoding",
        toolSelectionStrategy = ToolSelectionStrategy.NONE
    ) {
        val buildContext by node<VibecodingState, VibecodingState> { state ->
            buildContextNode(state)
        }

        val callLLM by node<VibecodingState, VibecodingState> { state ->
            callLLMNode(state)
        }

        val executeTools by node<VibecodingState, VibecodingState> { state ->
            executeToolsNode(state)
        }

        val persistState by node<VibecodingState, VibecodingState> { state ->
            persistStateNode(state)
        }

        val checkProgress by node<VibecodingState, VibecodingState> { state ->
            checkProgressNode(state)
        }

        edge(nodeStart forwardTo buildContext onCondition { _ -> true } transformed { it })
        edge(buildContext forwardTo callLLM onCondition { _ -> true } transformed { it })
        edge(callLLM forwardTo executeTools onCondition { _ -> true } transformed { it })
        edge(executeTools forwardTo persistState onCondition { _ -> true } transformed { it })
        edge(persistState forwardTo checkProgress onCondition { _ -> true } transformed { it })
        edge(checkProgress forwardTo callLLM onCondition { state ->
            !state.finished && !state.isFinal && state.error == null
        } transformed { it })
        edge(checkProgress forwardTo nodeFinish onCondition { state ->
            state.finished || state.isFinal || state.error != null
        } transformed { it })
    }

    /**
     * Point d'entrée principal — pipeline complet (V-5 : callLLM + persistState).
     * Résilient : chaque étape catch ses erreurs.
     * Si maxActions=0 (ou déjà isFinal), retourne immédiatement.
     */
    fun execute(initialState: VibecodingState): VibecodingState {
        // Court-circuit : si déjà final, retour immédiat
        if (initialState.isFinal) {
            log.info("[VibecodingGraph] initialState is already final (iteration=${initialState.iteration}, maxActions=${initialState.maxActions})")
            return initialState.finish()
        }

        // Vérification timeout avant de démarrer
        if (isTimedOut(initialState)) {
            log.warn("[VibecodingGraph] Session timed out before starting (elapsed=${elapsedSeconds(initialState)}s, timeout=${initialState.sessionTimeoutSeconds}s)")
            return initialState.withError("Timeout: session exceeded ${initialState.sessionTimeoutSeconds}s (elapsed ${elapsedSeconds(initialState)}s)")
        }

        // Persistance initiale — crée la session avant la boucle
        var state = initialState
        sessionId = try {
            runBlocking { effectiveSessionRepository?.createSession(state) }?.also {
                log.info("[VibecodingGraph] Session created: id={}", it)
            }
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] createSession failed: {}", e.message)
            null
        }

        // Étape 1 : buildContext
        state = try {
            buildContextNode(state)
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] buildContext failed: {}", e.message)
            state.withError("BuildContextFailed: ${e.message}")
        }
        if (state.error != null) return state
        if (state.isFinal) return state.finish()

        // Étape 2 : boucle d'exécution V-6 (callLLM + executeTools + persistState + feedback loop)
        while (!state.finished && !state.isFinal) {
            // Vérification timeout à chaque itération (erreur fatale)
            if (isTimedOut(state)) {
                log.warn("[VibecodingGraph] Session timed out during loop (elapsed=${elapsedSeconds(state)}s > timeout=${state.sessionTimeoutSeconds}s, iteration=${state.iteration})")
                state = state.withError("Timeout: session exceeded ${state.sessionTimeoutSeconds}s at iteration ${state.iteration}")
                return state
            }

            // S'il n'y a pas de plan, le LLM décide (ou on itère)
            val plan = state.plan
            if (plan == null || plan.epics.isEmpty()) {
                // Mode LLM : décision autonome
                if (llmProvider != null) {
                    state = try {
                        callLLMNode(state)
                    } catch (e: Exception) {
                        log.warn("[VibecodingGraph] callLLM failed: {}", e.message)
                        state.withError("CallLLMFailed: ${e.message}")
                    }
                    if (state.error != null) return state
                } else {
                    // Mode déterministe : pas de plan = itère
                    log.info("[VibecodingGraph] No plan, no LLM — iteration ${state.iteration + 1}/${state.maxActions}")
                    state = state.nextIteration()
                }
            } else {
                // Mode plan déterministe : executeTools sur les tâches du plan
                state = try {
                    executeToolsNode(state)
                } catch (e: Exception) {
                    log.warn("[VibecodingGraph] executeTools failed: {}", e.message)
                    state.withError("ExecuteToolsFailed: ${e.message}")
                }

                // X-3 verify→adapt : StepVerifier parse le résultat et gère le verdict
                if (state.error == null && state.lastToolResult.isNotBlank()
                    && !state.lastToolResult.startsWith("Replan:")
                    && !state.lastToolResult.startsWith("All plan tasks executed")
                    && !state.lastToolResult.startsWith("SKIPPED:")
                    && !state.lastToolResult.startsWith("REVERT_AND_CONTINUE:")) {
                    state = verifyStepNode(state)
                }

                // V-6 Error Recovery : si StepVerifier a détecté FAILED/BLOCKED/UNKNOWN
                // ou si executeTools a lancé une exception
                if (state.error != null) {
                    if (state.retryCount < state.maxRetries) {
                        log.info("[VibecodingGraph] Retry ${state.retryCount}/${state.maxRetries} after: {}", state.error)
                        if (llmProvider != null) {
                            val replanPrompt = buildReplanPrompt(state)
                            tokenTracker.trackPrompt(replanPrompt)
                            try {
                                val replanResponse = runBlocking { llmProvider.call(replanPrompt) }
                                tokenTracker.trackCompletion(replanResponse)
                                log.info("[VibecodingGraph] Replan response: {} chars", replanResponse.length)
                                state = state.clearError().incrementRetry().nextIteration().copy(
                                    lastToolResult = "Replan: $replanResponse"
                                )
                            } catch (e: Exception) {
                                log.warn("[VibecodingGraph] Replan LLM call failed: {}", e.message)
                                state = state.incrementRetry()
                            }
                        } else {
                            log.info("[VibecodingGraph] No LLM — retry ${state.retryCount}/${state.maxRetries}, continuing")
                            state = state.clearError().incrementRetry().nextIteration()
                        }
                        if (state.error != null && state.retryCount >= state.maxRetries) {
                            log.warn("[VibecodingGraph] maxRetries (${state.maxRetries}) exhausted")
                            state = executeRollbackStrategy(state)
                            if (state.finished || state.error != null) return state
                        }
                    } else {
                        log.warn("[VibecodingGraph] maxRetries (${state.maxRetries}) exhausted, delegating to rollback strategy")
                        state = executeRollbackStrategy(state)
                        if (state.finished || state.error != null) return state
                    }
                }
            }

            // persistState après chaque itération
            state = try {
                persistStateNode(state)
            } catch (e: Exception) {
                log.warn("[VibecodingGraph] persistState failed: {}", e.message)
                state // ne casse pas la boucle sur erreur de persistence
            }

            state = checkProgressNode(state)
        }

        return if (state.isFinal) state.finish() else state
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }

    // === Companion — helpers statiques ===

    companion object {
        /**
         * Reconstruit un [VibecodingState] depuis un [SessionRecord]
         * pour reprendre une session interrompue (--resume).
         *
         * Le state reprend à iteration=0 (le graphe redémarre) mais conserve
         * le plan, la classification, le workspaceRoot et l'intention.
         * Si la session était finie, le state est marqué finished.
         */
        fun resumeSession(record: SessionRecord): VibecodingState {
            val intentionWithId = "[Resume ${record.id}] ${record.intention}"
            return VibecodingState(
                intention = intentionWithId,
                workspaceRoot = record.workspaceRoot,
                dryRun = record.dryRun,
                maxActions = record.maxActions,
                iteration = 0,
                planJson = record.planJson ?: "",
                plan = null, // sera reconstruit par buildContext si nécessaire
                classification = record.classification,
                finished = record.finished,
                error = record.error
            )
        }
    }

    // === Méthodes privées — partagées entre le graphe koog et execute() ===

    /**
     * Nœud 1 : buildContext — pipeline KoogAugmentedContextGraph (optionnel).
     * Si aucun graphe augmenté n'est fourni, ou si pgvector est down,
     * le contexte est vide mais le pipeline continue (mode résilient).
     */
    private fun buildContextNode(state: VibecodingState): VibecodingState {
        // Si le state a déjà un plan (injecté directement), on le conserve
        if (state.plan != null) {
            log.info("[VibecodingGraph] State already has a plan — skipping buildContext")
            return state
        }
        if (augmentedGraph == null) {
            log.info("[VibecodingGraph] No augmented graph — skipping buildContext")
            return state.withPlan(planJson = "", plan = null, classification = "simple")
        }
        val augmentedState = AugmentedState(
            intention = state.intention,
            workspaceRoot = state.workspaceRoot
        )
        return try {
            val result = augmentedGraph.execute(augmentedState)
            if (result.error != null) {
                log.info("[VibecodingGraph] buildContext returned error: {}", result.error)
                state.withPlan(planJson = "", plan = null, classification = "simple")
            } else {
                state.withPlan(
                    planJson = result.planJson,
                    plan = result.plan,
                    classification = result.classification
                )
            }
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] buildContext exception: {}", e.message)
            state.withPlan(planJson = "", plan = null, classification = "simple")
        }
    }

    /**
     * Nœud 2 (V-5) : callLLM — le LLM décide de la prochaine action.
     * Si llmProvider est null → identique au mode déterministe pré-V-5.
     */
    private fun callLLMNode(state: VibecodingState): VibecodingState {
        if (llmProvider == null) {
            return state // pas de LLM = on continue déterministe
        }

        val prompt = buildPromptForIteration(state)
        tokenTracker.trackPrompt(prompt)

        return try {
            val response = runBlocking { llmProvider.call(prompt) }
            tokenTracker.trackCompletion(response)
            log.info("[VibecodingGraph] LLM response: {} chars, first 80: {}", response.length, response.take(80))

            state.nextIteration().copy(
                lastToolResult = "LLM decided: $response"
            )
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] LLM call failed: {}", e.message)
            state.withError("LLMCallFailed: ${e.message}")
        }
    }

    private fun buildPromptForIteration(state: VibecodingState): String {
        val statusLine = state.error?.let { "ERROR: $it" } ?: "OK"
        return buildString {
            appendLine("Vibecoding session — iteration ${state.iteration + 1}/${state.maxActions}")
            appendLine("Intention: ${state.intention}")
            appendLine("Workspace: ${state.workspaceRoot}")
            appendLine("Dry run: ${state.dryRun}")
            appendLine("Status: $statusLine")
            if (state.executedTasks.isNotEmpty()) {
                appendLine("Tasks done: ${state.executedTasks.joinToString(", ")}")
            }
            state.plan?.let { plan ->
                appendLine("Plan remaining tasks: ${plan.epics.sumOf { it.userStories.sumOf { s -> s.tasks.size } } - state.executedTasks.size}")
            }
            appendLine()
            appendLine("What should be the next action? Respond with a single tool name and parameters, or 'DONE' if finished.")
        }
    }

    /**
     * V-6 Feedback Loop : prompt de replanification après erreur.
     * Le LLM reçoit le contexte de l'erreur et doit proposer une approche alternative.
     */
    internal fun buildReplanPrompt(state: VibecodingState): String {
        return buildString {
            appendLine("Vibecoding error recovery — retry ${state.retryCount}/${state.maxRetries}")
            appendLine("Intention: ${state.intention}")
            appendLine("Current task: ${state.currentTaskDescription}")
            appendLine("Last tool result: ${state.lastToolResult}")
            appendLine("Error: ${state.error}")
            if (state.executedTasks.isNotEmpty()) {
                appendLine("Already executed: ${state.executedTasks.joinToString(", ")}")
            }
            if (taskSchemas.isNotEmpty()) {
                appendLine()
                appendLine("Available Gradle tasks (use exec_gradle with task name):")
                appendLine(TaskListFormatter.format(taskSchemas))
            }
            appendLine()
            appendLine("The previous action failed. Propose an alternative approach to recover.")
            appendLine("Suggest a different Gradle task, file edit, or approach. Keep it short.")
        }
    }

    /**
     * Nœud 3 : executeTools — exécute une action via ToolRegistry.
     * Alias de executeActionNode (rétrocompatibilité pré-V-5).
     * En dryRun : ne fait qu'incrémenter le compteur.
     * Sans plan : incrémente et continue (mode résilient).
     */
    private fun executeToolsNode(state: VibecodingState): VibecodingState {
        // dryRun : parcourt les tâches du plan mais sans exécution réelle
        if (state.dryRun) {
            return executePlanTasks(state, realExecution = false)
        }
        return executePlanTasks(state, realExecution = true)
    }

    /**
     * Nœud 4 (V-5) : persistState — sauvegarde l'état courant via SessionRepository.
     * Ne casse pas le pipeline en cas d'erreur de persistance.
     * Résilient : si sessionRepository est null ou si la session n'a pas été créée,
     * on passe simplement (mode sans persistance).
     */
    private fun persistStateNode(state: VibecodingState): VibecodingState {
        val repo = effectiveSessionRepository ?: return state
        val sid = sessionId ?: return state
        return try {
            val tracker = tokenTracker
            runBlocking {
                repo.updateSession(sid, state, "unknown", tracker)
            }
            log.debug("[VibecodingGraph] Session {} updated: iteration={}, promptTokens={}, cost={}", sessionId, state.iteration, tracker.promptTokens, tracker.estimatedCost("deepseek-v4-pro"))
            state
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] persistState failed: {}", e.message)
            state
        }
    }

    private fun executePlanTasks(state: VibecodingState, realExecution: Boolean): VibecodingState {
        // Cherche la prochaine tâche à exécuter dans le plan
        val plan = state.plan
        // Si pas de plan ou plan vide, on itère simplement (mode résilient)
        if (plan == null || plan.epics.isEmpty()) {
            log.info("[VibecodingGraph] No plan to execute, iteration ${state.iteration + 1}/${state.maxActions}")
            return state.nextIteration().copy(
                lastToolResult = "No plan tasks to execute"
            )
        }

        // Exécute la prochaine tâche non faite
        val allTasks = plan.epics.flatMap { epic ->
            epic.userStories.flatMap { story -> story.tasks.map { task ->
                Triple(epic.name, story.description, task)
            }}
        }

        val nextIndex = state.executedTasks.size
        if (nextIndex >= allTasks.size) {
            // Toutes les tâches sont exécutées
            return state.copy(finished = true, lastToolResult = "All plan tasks executed")
        }

        val (epicName, storyDesc, task) = allTasks[nextIndex]
        log.info("[VibecodingGraph] Executing task: ${task.description} (${task.gradleTask})")

        return try {
            val result = toolRegistry.execute(
                toolName = "exec_gradle",
                arguments = mapOf("task" to task.gradleTask),
                workspaceRoot = state.workspaceRoot,
                dryRun = state.dryRun
            )
            val isFailure = result.contains("BUILD FAILED") || result.contains("FAILED")
            if (isFailure) {
                log.warn("[VibecodingGraph] Task '{}' returned failure: {}", task.description, result.take(200))
                state.nextIteration().copy(
                    lastToolResult = result,
                    currentTaskDescription = task.description,
                    error = "TaskFailed: ${result.take(200)}",
                    retryCount = state.retryCount + 1,
                    finished = false
                )
            } else {
                state.nextIteration().copy(
                    executedTasks = state.executedTasks + task.description,
                    lastToolResult = result,
                    currentTaskDescription = task.description
                )
            }
        } catch (e: Exception) {
            log.warn("[VibecodingGraph] Task execution failed: {}", e.message)
            state.nextIteration().copy(
                executedTasks = state.executedTasks + task.description,
                lastToolResult = "Failed: ${e.message}",
                currentTaskDescription = task.description,
                // V-6: erreur récupérable — ne termine pas la session
                error = "TaskFailed: ${e.message}",
                retryCount = state.retryCount + 1,
                finished = false
            )
        }
    }

    /**
     * Nœud 3 : checkProgress — vérifie si le travail est terminé.
     * V-6 : une erreur récupérable (retryCount < maxRetries) ne termine pas la session.
     */
    private fun checkProgressNode(state: VibecodingState): VibecodingState {
        // V-6 : erreur avec retries restants → ne pas finir, laisser la boucle retry
        if (state.error != null && state.retryCount >= state.maxRetries) return state.finish()
        if (state.error != null && state.retryCount < state.maxRetries) return state
        if (state.iteration >= state.maxActions) return state.finish()
        if (state.finished) return state

        // Vérifie si toutes les tâches du plan sont exécutées
        val allTasks = state.plan?.epics?.flatMap { epic ->
            epic.userStories.flatMap { story -> story.tasks }
        } ?: emptyList()
        if (allTasks.isNotEmpty() && state.executedTasks.size >= allTasks.size && state.iteration > 0) {
            return state.finish()
        }

        // Vérifie si le plan est vide (rien à faire)
        val noWorkRemaining = state.plan?.epics?.isEmpty() ?: true
        if (noWorkRemaining && state.iteration > 0) return state.finish()

        return state
    }

    // ── X-3 verify→adapt helpers ──

    /**
     * Vérifie la sortie d'une tâche exécutée via StepVerifier.
     * Extrait le step courant du plan et parse le résultat.
     */
    private fun verifyStepNode(state: VibecodingState): VibecodingState {
        val step = extractCurrentStep(state) ?: return state
        return stepVerifier.verifyAndAdapt(state, step)
    }

    /**
     * Extrait le [VibecodingStep] correspondant à la tâche courante du plan.
     * Retourne null si pas de plan ou toutes les tâches sont faites.
     */
    private fun extractCurrentStep(state: VibecodingState): VibecodingStep? {
        val plan = state.plan ?: return null
        val allTasks = plan.epics.flatMap { epic ->
            epic.userStories.flatMap { story -> story.tasks }
        }
        val nextIndex = state.executedTasks.size
        if (nextIndex >= allTasks.size || nextIndex < 0) return null
        val task = allTasks[nextIndex]
        return VibecodingStep(
            description = task.description,
            gradleTask = task.gradleTask,
            expectedOutput = "BUILD SUCCESSFUL"
        )
    }

    // ── X-4 Rollback Strategy ──

    private fun executeRollbackStrategy(state: VibecodingState): VibecodingState {
        val executor = rollbackExecutor ?: run {
            log.warn("[VibecodingGraph] No rollback executor — defaulting to STOP_ON_ERROR")
            return state.copy(
                finished = true,
                error = "MaxRetriesExhausted: ${state.error}"
            )
        }

        val step = extractCurrentStep(state) ?: VibecodingStep(
            description = state.currentTaskDescription.ifBlank { "unknown" },
            gradleTask = "unknown",
            expectedOutput = "BUILD SUCCESSFUL"
        )

        val strategy = try {
            RollbackStrategy.valueOf(state.rollbackStrategy)
        } catch (e: IllegalArgumentException) {
            RollbackStrategy.STOP_ON_ERROR
        }

        val plan = VibecodingPlan(
            steps = listOf(step),
            rollbackStrategy = strategy
        )

        log.info("[VibecodingGraph] Executing rollback strategy: {} for step '{}'", strategy, step.description)
        return executor.execute(state, plan, step)
    }

    // ── Timeout helpers ──

    private fun isTimedOut(state: VibecodingState): Boolean {
        return elapsedSeconds(state) > state.sessionTimeoutSeconds
    }

    private fun elapsedSeconds(state: VibecodingState): Long {
        return (System.currentTimeMillis() - state.sessionStartTimeMs) / 1000
    }
}
