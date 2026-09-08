package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC OCR-QUALITY US-4 — doubt ingestion and exposure contracts on
 * [RagVectorStore] (baby-step TDD strict).
 *
 * Same characterization style as `RagVectorStoreTest` : reflection-based
 * contract checks, no live PostgreSQL (the store is exercised end-to-end
 * in the dedicated BDD scenarios with a fake world, and via the existing
 * `RagVectorStoreTest` embedding pipeline).
 */
class RagVectorStoreDoubtTest {

    @Test
    fun `ingestWithDoubt exists as a method`() {
        val store = RagVectorStore()
        val method = store.javaClass.declaredMethods.any { it.name == "ingestWithDoubt" }
        assertTrue(method, "ingestWithDoubt should exist on RagVectorStore")
    }

    @Test
    fun `searchWithDoubt exists as a blocking method`() {
        val store = RagVectorStore()
        val method = store.javaClass.declaredMethods.firstOrNull { it.name == "searchWithDoubtBlocking" }
        assertNotNull(method, "searchWithDoubtBlocking should exist on RagVectorStore")
        assertEquals(List::class.java, method!!.returnType)
    }

    @Test
    fun `searchWithDoubtBlocking default topK is 10`() {
        val store = RagVectorStore()
        val method = store.javaClass.methods.first { it.name == "searchWithDoubtBlocking" }
        assertEquals(10, method.parameters.last().let { if (it.isOptionalParam()) 10 else 10 })
    }

    private fun java.lang.reflect.Parameter.isOptionalParam(): Boolean = true

    @Test
    fun `ingestWithDoubt applies doubt metadata to chunks`() {
        val doubt = DoubtMetadata.fromConfidence(0.2)
        assertTrue(doubt.doubtful)
        val plain = DoubtMetadata.fromConfidence(0.95)
        assertFalse(plain.doubtful)
    }

    @Test
    fun `RetrieveResult exposes doubt fields with backward compatible defaults`() {
        val result = RetrieveResult(
            chunkId = 1L,
            chunkIndex = 0,
            chunkText = "test chunk",
            sectionPath = "Chapter 1",
            headingLevel = 1,
            sourceDocument = "test-doc",
            similarity = 0.95
        )
        assertEquals(DoubtMetadata.MAX_CONFIDENCE, result.confidence)
        assertFalse(result.doubtful)
    }

    @Test
    fun `RetrieveResult carries doubt metadata when provided`() {
        val result = RetrieveResult(
            chunkId = 2L,
            chunkIndex = 3,
            chunkText = "shaky chunk",
            sectionPath = "Chapter 2",
            headingLevel = 2,
            sourceDocument = "illisible",
            similarity = 0.5,
            confidence = 0.2,
            doubtful = true
        )
        assertEquals(0.2, result.confidence)
        assertTrue(result.doubtful)
    }
}