package codebase.koog.expert

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ExpertRegistryTest {

    private lateinit var registry: ExpertRegistry
    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")
    private val generalDomain = ExpertDomain("general", "Generalist fallback")

    @BeforeEach
    fun setUp() {
        registry = ExpertRegistry()
    }

    @Test
    fun `empty registry has size zero`() {
        assertEquals(0, registry.size())
        assertTrue(registry.listDomains().isEmpty())
    }

    @Test
    fun `register single expert`() {
        val reg = ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud")
        registry.register(reg)

        assertEquals(1, registry.size())
        assertEquals(listOf(kotlinDomain), registry.listDomains())
    }

    @Test
    fun `register multiple experts`() {
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"),
            ExpertRegistration(docsDomain, "gpt-oss:20b-cloud"),
            ExpertRegistration(generalDomain, "deepseek-v4-pro")
        ))

        assertEquals(3, registry.size())
        assertEquals(3, registry.listDomains().size)
    }

    @Test
    fun `resolve returns correct registration`() {
        val reg = ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11438", 90)
        registry.register(reg)

        val resolved = registry.resolve(kotlinDomain)
        assertNotNull(resolved)
        assertEquals("gpt-oss:120b-cloud", resolved!!.modelName)
        assertEquals("http://localhost:11438", resolved.baseUrl)
        assertEquals(90, resolved.timeoutSeconds)
    }

    @Test
    fun `resolve returns null for unknown domain`() {
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"))

        assertNull(registry.resolve(docsDomain))
    }

    @Test
    fun `resolveByName works`() {
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"))

        val resolved = registry.resolveByName("kotlin")
        assertNotNull(resolved)
        assertEquals(kotlinDomain, resolved!!.domain)

        assertNull(registry.resolveByName("unknown"))
    }

    @Test
    fun `register overwrites existing domain`() {
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"))
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:20b-cloud"))

        assertEquals(1, registry.size())
        assertEquals("gpt-oss:20b-cloud", registry.resolve(kotlinDomain)!!.modelName)
    }

    @Test
    fun `clear empties registry`() {
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"),
            ExpertRegistration(docsDomain, "gpt-oss:20b-cloud")
        ))
        assertEquals(2, registry.size())

        registry.clear()
        assertEquals(0, registry.size())
        assertTrue(registry.listDomains().isEmpty())
    }

    @Test
    fun `ExpertRegistration defaults`() {
        val reg = ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud")
        assertEquals("http://localhost:11437", reg.baseUrl)
        assertEquals(120, reg.timeoutSeconds)
    }
}
