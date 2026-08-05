package codebase.koog.llm.service

import codebase.koog.llm.LlmProvider
import codebase.koog.llm.LlmProviderResolver

/**
 * Pure resolver wrapping [LlmProviderResolver] so it can be unit-tested
 * without a Gradle scope.
 *
 * [LlmBuildService] delegates to [resolveProvider] — the BuildService only
 * reads its Gradle parameters and forwards them here. This split keeps the
 * resolution logic testable in isolation (no Gradle runtime required).
 *
 * Scope (US-8.1): codebase only, additive.
 */
object LlmServiceResolver {

    /**
     * Resolves an [LlmProvider] for the given [model].
     *
     * Blank model falls back to Ollama (matches [LlmProviderResolver]
     * semantics — "ollama", "" → pool-backed Gemma4 Cloud).
     */
    fun resolveProvider(model: String): LlmProvider =
        LlmProviderResolver.resolve(model)
}