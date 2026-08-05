package codebase.scenarios

import codebase.koog.llm.LlmProvider
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.Project

/**
 * World object shared between [LlmServiceSteps] steps — holds the state
 * accumulated across Given/When/Then for a single scenario.
 *
 * Pattern aligned with [PoolWorld] (PicoContainer DI, one instance per
 * scenario).
 */
class LlmServiceWorld {
    lateinit var project: Project
    var serviceProvider: Provider<out BuildService<*>>? = null
    var provider: LlmProvider? = null
}