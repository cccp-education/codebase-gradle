package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC corpus-ingest US-1 (CB-FPA-RAG historic id) — corpus chunks file loading (pure DDD, no
 * Gradle, no pgvector). The codex chunks.json format matches the
 * [DocumentChunk] type (compatibility confirmed US-0 S-213); the loader
 * maps the JSON array into typed chunks for the ingestion run.
 */
class CorpusChunksLoaderTest {

    @TempDir
    lateinit var tmp: java.nio.file.Path

    @Test
    fun `loads a codex chunks json file into typed DocumentChunks`() {
        val file = tmp.resolve("chunks.json").toFile()
        file.writeText(
            """
            [
              {
                "id": "chk-24d4da70bf379994",
                "sourceDocument": "book",
                "sectionPath": "Devenir Auteur > Content II",
                "headingLevel": 1,
                "content": "# Devenir Auteur",
                "license": "PROPRIETARY"
              },
              {
                "id": "chk-898506d284409c66",
                "sourceDocument": "book",
                "sectionPath": "Chapter",
                "headingLevel": 2,
                "content": "page [ILLISIBLE]",
                "overlapNext": "continuite",
                "license": "PROPRIETARY"
              }
            ]
            """.trimIndent()
        )

        val chunks = CorpusChunksLoader.load(file)

        assertEquals(2, chunks.size)
        assertEquals("book", chunks[0].sourceDocument)
        assertEquals("# Devenir Auteur", chunks[0].content)
        assertEquals("PROPRIETARY", chunks[0].license)
        assertEquals("continuite", chunks[1].overlapNext)
    }

    @Test
    fun `loaded chunks feed DoubtPolicy mark end to end`() {
        val file = tmp.resolve("chunks.json").toFile()
        file.writeText(
            """
            [
              {
                "id": "chk-24d4da70bf379994",
                "sourceDocument": "book",
                "sectionPath": "Chapter",
                "headingLevel": 1,
                "content": "clean",
                "license": "PROPRIETARY"
              },
              {
                "id": "chk-898506d284409c66",
                "sourceDocument": "book",
                "sectionPath": "Chapter",
                "headingLevel": 2,
                "content": "page [ILLISIBLE]",
                "license": "PROPRIETARY"
              }
            ]
            """.trimIndent()
        )

        val marked = DoubtPolicy.mark(CorpusChunksLoader.load(file))
        assertEquals(2, marked.size)
        assertTrue(marked[1].doubt.doubtful)
    }
}