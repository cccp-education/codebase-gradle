package codebase.koog.svo

import codebase.koog.plannerport.PlannerPort
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceRegistration

/**
 * Runtime resolution of the [PlannerPort] wired by the planner borough
 * (EPIC SVO-4, D3/D5 continuation of SVO-1/SVO-3).
 *
 * Cross-borough contract: the consumer build applies both plugins
 * (`education.cccp.codebase` + `education.cccp.planner`); `PlanningPlugin`
 * registers the port adapter under the canonical shared-services name
 * [CANONICAL_SERVICE_NAME] (`plannerPortService` — planner-side contract,
 * `PlannerPortBuildService.SERVICE_NAME`, breaking if renamed). codebase
 * resolves the service by that exact name from the build-scoped
 * registrations and falls back to [codebase.koog.plannerport.PlannerPort.NoOp]
 * when nothing is wired — degraded, never crashes, testable without LLM.
 *
 * Pure function on a [List] of [BuildServiceRegistration] (the Gradle
 * shape `gradle.sharedServices.registrations`); no Gradle instance needed
 * in unit tests (pattern `SessionPromptResolver`).
 */
object PlannerPortResolver {

    /** Canonical shared-services name — mirrors `planning.adapter.PlannerPortBuildService.SERVICE_NAME`. */
    const val CANONICAL_SERVICE_NAME: String = "plannerPortService"

    fun resolve(
        registrations: List<BuildServiceRegistration<*, *>>,
    ): PlannerPort {
        val matching = registrations
            .filter { it.name == CANONICAL_SERVICE_NAME }
            .mapNotNull { registration -> extractPort(registration) }
        return matching.firstOrNull() ?: PlannerPort.NoOp
    }

    /**
     * Extracts the [PlannerPort] from a BuildService exposing `port()` —
     * duck-typed to keep zero `planning.*` import on the N1 side: unknown
     * shapes (plain BuildService, absent instance) degrade to null → NoOp.
     */
    private fun extractPort(registration: BuildServiceRegistration<*, *>): PlannerPort? = try {
        val service = registration.getService().get() ?: return null
        val portMethod = service.javaClass.methods.firstOrNull { m -> m.name == "port" && m.parameterCount == 0 }
            ?: return null
        (portMethod.invoke(service) as? PlannerPort)
    } catch (_: Exception) {
        null
    }
}
