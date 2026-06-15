package codebase.koog.autofocus

import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextZoomerTest {

    private val zoomer = ContextZoomer()

    private fun makeContext(
        eager: String = "eager content ".repeat(100),
        rag: String = "rag content ".repeat(100),
        graphify: String = "graphify content ".repeat(100),
        docs: String = "docs content ".repeat(50)
    ) = CompositeContext(
        eagerSection = eager.trim(),
        ragSection = rag.trim(),
        graphifySection = graphify.trim(),
        docsSection = docs.trim(),
        config = CompositeContextConfig()
    )

    @Test
    fun `zoom to BIG_PICTURE returns context unchanged`() {
        val context = makeContext()
        val result = zoomer.zoom(AutofocusLevel.BIG_PICTURE, context)
        assertEquals(context.eagerSection, result.eagerSection)
        assertEquals(context.ragSection, result.ragSection)
        assertEquals(context.graphifySection, result.graphifySection)
        assertEquals(context.docsSection, result.docsSection)
    }

    @Test
    fun `zoom to ARCHITECTURE truncates sections to budget`() {
        val context = makeContext()
        val result = zoomer.zoom(AutofocusLevel.ARCHITECTURE, context)
        val budget = AutofocusLevel.ARCHITECTURE.tokenBudget
        assertTrue(result.eagerSection.length <= budget / 2 * 4 + 50)
        assertTrue(result.ragSection.length <= budget / 4 * 4 + 50)
        assertTrue(result.graphifySection.length <= budget / 4 * 4 + 50)
        assertEquals("", result.docsSection)
    }

    @Test
    fun `zoom to MODULE truncates sections to budget`() {
        val context = makeContext()
        val result = zoomer.zoom(AutofocusLevel.MODULE, context)
        val budget = AutofocusLevel.MODULE.tokenBudget
        assertTrue(result.eagerSection.length <= budget / 3 * 4 + 50)
        assertTrue(result.ragSection.length <= budget / 3 * 4 + 50)
        assertTrue(result.graphifySection.length <= budget / 3 * 4 + 50)
        assertEquals("", result.docsSection)
    }

    @Test
    fun `zoom to IMPLEMENTATION keeps only ragSection`() {
        val context = makeContext()
        val result = zoomer.zoom(AutofocusLevel.IMPLEMENTATION, context)
        assertEquals("", result.eagerSection)
        assertEquals("", result.graphifySection)
        assertEquals("", result.docsSection)
        assertTrue(result.ragSection.isNotBlank())
    }

    @Test
    fun `zoom to IMPLEMENTATION truncates ragSection to 500 tokens`() {
        val context = makeContext(rag = "x".repeat(10000))
        val result = zoomer.zoom(AutofocusLevel.IMPLEMENTATION, context)
        val budget = AutofocusLevel.IMPLEMENTATION.tokenBudget
        assertTrue(result.ragSection.length <= budget * 4 + 50)
        assertTrue(result.ragSection.contains("[...truncated"))
    }

    @Test
    fun `zoom preserves config`() {
        val config = CompositeContextConfig(
            totalTokenBudget = 4000,
            budgetEagerLazy = 0.25,
            budgetRag = 0.25,
            budgetGraphify = 0.25,
            budgetDocs = 0.25
        )
        val context = CompositeContext(
            eagerSection = "eager",
            ragSection = "rag",
            graphifySection = "graphify",
            docsSection = "docs",
            config = config
        )
        val result = zoomer.zoom(AutofocusLevel.BIG_PICTURE, context)
        assertEquals(config, result.config)
    }

    @Test
    fun `zoom to ARCHITECTURE with small content does not truncate`() {
        val context = makeContext(
            eager = "short",
            rag = "short",
            graphify = "short"
        )
        val result = zoomer.zoom(AutofocusLevel.ARCHITECTURE, context)
        assertEquals("short", result.eagerSection)
        assertEquals("short", result.ragSection)
        assertEquals("short", result.graphifySection)
    }

    @Test
    fun `zoom to MODULE with small content does not truncate`() {
        val context = makeContext(
            eager = "short",
            rag = "short",
            graphify = "short"
        )
        val result = zoomer.zoom(AutofocusLevel.MODULE, context)
        assertEquals("short", result.eagerSection)
        assertEquals("short", result.ragSection)
        assertEquals("short", result.graphifySection)
    }

    @Test
    fun `zoom to IMPLEMENTATION with blank ragSection returns empty`() {
        val context = CompositeContext(
            eagerSection = "eager",
            ragSection = "",
            graphifySection = "graphify",
            docsSection = "docs",
            config = CompositeContextConfig()
        )
        val result = zoomer.zoom(AutofocusLevel.IMPLEMENTATION, context)
        assertEquals("", result.ragSection)
    }

    @Test
    fun `zoom to IMPLEMENTATION with whitespace ragSection preserves it`() {
        val context = CompositeContext(
            eagerSection = "eager",
            ragSection = "   ",
            graphifySection = "graphify",
            docsSection = "docs",
            config = CompositeContextConfig()
        )
        val result = zoomer.zoom(AutofocusLevel.IMPLEMENTATION, context)
        assertEquals("   ", result.ragSection)
    }

    @Test
    fun `zoom to ARCHITECTURE docsSection is always empty`() {
        val context = makeContext(docs = "some docs content")
        val result = zoomer.zoom(AutofocusLevel.ARCHITECTURE, context)
        assertEquals("", result.docsSection)
    }

    @Test
    fun `zoom to MODULE docsSection is always empty`() {
        val context = makeContext(docs = "some docs content")
        val result = zoomer.zoom(AutofocusLevel.MODULE, context)
        assertEquals("", result.docsSection)
    }

    @Test
    fun `zoom to IMPLEMENTATION docsSection is always empty`() {
        val context = makeContext(docs = "some docs content")
        val result = zoomer.zoom(AutofocusLevel.IMPLEMENTATION, context)
        assertEquals("", result.docsSection)
    }

    @Test
    fun `zoom to BIG_PICTURE preserves docsSection`() {
        val context = makeContext(docs = "some docs content")
        val result = zoomer.zoom(AutofocusLevel.BIG_PICTURE, context)
        assertEquals("some docs content", result.docsSection)
    }
}
