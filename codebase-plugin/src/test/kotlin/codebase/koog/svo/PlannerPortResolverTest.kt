package codebase.koog.svo

import codebase.koog.plannerport.PlannerPort
import codebase.koog.planning.PlanState
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlannerPortResolverTest {

    private class RecordingPort : PlannerPort {
        val calls = mutableListOf<Pair<String, String>>()
        override fun plan(intention: String, context: contracts.context.CompositeContext): PlanState {
            calls += intention to context.eagerSection
            return PlanState(intention = intention)
        }
    }

    /** Fake BuildService exposing the port() method (mirror planner's PlannerPortBuildService). */
    private abstract class FakePortService : BuildService<FakePortService.Params> {
        interface Params : BuildServiceParameters
        abstract fun port(): PlannerPort
    }

    private fun portService(port: PlannerPort): BuildService<*> = object : FakePortService() {
        override fun port(): PlannerPort = port
        override fun getParameters(): Params = object : Params {}
    }

    private val project = org.gradle.testfixtures.ProjectBuilder.builder().build()
    private val propertyFactory = project.objects

    @Suppress("UNCHECKED_CAST")
    private fun registration(
        name: String,
        service: BuildService<*>?,
    ): BuildServiceRegistration<*, *> {
        val maxParallel: org.gradle.api.provider.Property<Int> = propertyFactory.property(Int::class.javaObjectType)
        val serviceProperty: org.gradle.api.provider.Property<BuildService<*>> =
            propertyFactory.property(BuildService::class.java)
        serviceProperty.set(service)
        return java.lang.reflect.Proxy.newProxyInstance(
            BuildServiceRegistration::class.java.classLoader,
            arrayOf(BuildServiceRegistration::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getName" -> name
                "getParameters" -> object : BuildServiceParameters {}
                "getMaxParallelUsages" -> maxParallel
                "getService" -> serviceProperty
                else -> throw UnsupportedOperationException("test fake: ${method.name}")
            }
        } as BuildServiceRegistration<*, *>
    }

    @Test
    fun `resolves by canonical service name when service is present`() {
        val port = RecordingPort()
        val resolved = PlannerPortResolver.resolve(
            registrations = listOf(
                registration("unrelated", null),
                registration(PlannerPortResolver.CANONICAL_SERVICE_NAME, portService(port)),
            ),
        )
        assertSame(port, resolved, "wired adapter must win")
    }

    @Test
    fun `missing service falls back to NoOp`() {
        val resolved = PlannerPortResolver.resolve(emptyList())
        assertSame(PlannerPort.NoOp, resolved, "no adapter → NoOp degraded fallback")
    }

    @Test
    fun `service not exposing port falls back to NoOp`() {
        val resolved = PlannerPortResolver.resolve(
            registrations = listOf(
                registration(
                    PlannerPortResolver.CANONICAL_SERVICE_NAME,
                    object : BuildService<BuildServiceParameters> {
                        override fun getParameters(): BuildServiceParameters =
                            throw UnsupportedOperationException("test fake — port-less service")
                    },
                ),
            ),
        )
        assertSame(PlannerPort.NoOp, resolved)
    }

    @Test
    fun `canonical name is the planner cross-borough contract`() {
        assertEquals("plannerPortService", PlannerPortResolver.CANONICAL_SERVICE_NAME)
    }

    @Test
    fun `resolved NoOp port fails cleanly without crashing`() {
        val resolved = PlannerPortResolver.resolve(emptyList())
        val planState = resolved.plan("anything", contracts.context.CompositeContext("", "", "", config = contracts.context.CompositeContextConfig()))
        assertTrue(planState.error != null, "NoOp must return a failed PlanState, never crash")
    }
}
