package codebase.koog.llm.service

import codebase.koog.llm.LlmProvider
import codebase.koog.llm.service.LlmServiceResolver.resolveProvider
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Gradle [BuildService] that exposes an [LlmProvider] as an injectable
 * Gradle-managed service.
 *
 * This is the bridge between codebase's N1 LLM pool ([LlmProviderResolver])
 * and N2 consumers (slider being the first). The service is registered via
 * `gradle.sharedServices.registerIfAbsent` and injected into tasks through
 * `Provider<LlmBuildService>`.
 *
 * ## DI native Gradle (Decision 001 — slider)
 *
 * No DI container (PicoContainer, Koin) — Gradle's own `BuildService` +
 * `Provider<T>` covers the need. The service lifecycle is Gradle-scoped:
 * instantiated once per build, shared by all tasks that declare it, closed
 * at the end of the build.
 *
 * ## Parameters
 *   | Parameter        | Default  | Notes |
 *   |------------------|----------|-------|
 *   | model            | ""       | "ollama" / "gemini" / custom model name |
 *   | ollamaPoolPorts  | (env)    | Comma-separated ports (11437-11465) |
 *
 * ## Scope (US-8.1)
 *
 * Additive — zero modification of existing codebase or slider code. The
 * service delegates resolution to [LlmServiceResolver] (pure, testable).
 */
abstract class LlmBuildService : BuildService<LlmBuildService.Params> {

    interface Params : BuildServiceParameters {
        /** LLM model identifier — "ollama", "gemini", or a custom model name. */
        val model: Property<String>

        /** Comma-separated Ollama pool ports (e.g. "11437,11438,11439"). */
        val ollamaPoolPorts: Property<String>
    }

    /**
     * Returns the resolved [LlmProvider] for the configured model.
     *
     * Delegates to [LlmServiceResolver] (pure function, unit-tested). The
     * BuildService only reads its Gradle parameters — no resolution logic
     * lives here.
     */
    fun provider(): LlmProvider = resolveProvider(parameters.model.get())
}
