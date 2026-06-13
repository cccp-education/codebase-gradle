package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgenticChunkerTest {

    private val chunker = AgenticChunker()

    @Test
    fun `should chunk a rule from AGENT adoc`() {
        val content = """
            = AGENT.adoc — Directives Agent
            :date: 2026-05-19

            == Regles Absolues

            === Git

            **INTERDICTION FORMELLE** de commit/push/merge sans permission explicite de l'utilisateur.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val rules = chunks.filter { it.chunkType == ChunkType.RULE }
        assertTrue(rules.isNotEmpty(), "Should detect at least one RULE chunk")
        val gitRule = rules.first { it.content.contains("INTERDICTION FORMELLE") }
        assertEquals(ChunkType.RULE, gitRule.chunkType)
        assertEquals(TaxonomyVerb.INTERDIRE, gitRule.verb)
        assertEquals("AGENT.adoc", gitRule.sourceFile)
        assertTrue(gitRule.sourceLines.isNotBlank(), "Should have source line range")
        assertTrue(gitRule.weight > 0.5, "Rule with FORMELLE should have high weight")
    }

    @Test
    fun `should chunk a concept from section header`() {
        val content = """
            = TAXONOMIE_WORKSPACE — Ontologie
            :date: 2026-05-18

            == Principes Fondateurs

            . *Le verbe dit pourquoi, le complement dit quoi* — `generateSPG` vs `collectFromCorpus`.
               La taxonomie encode l'intention. Zero ambiguite.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "TAXONOMIE_WORKSPACE.adoc")

        val concepts = chunks.filter { it.chunkType == ChunkType.CONCEPT }
        assertTrue(concepts.isNotEmpty(), "Should detect CONCEPT chunks from section headers")
        val principe = concepts.first { it.content.contains("Le verbe dit pourquoi") }
        assertEquals(ChunkType.CONCEPT, principe.chunkType)
        assertEquals("TAXONOMIE_WORKSPACE.adoc", principe.sourceFile)
    }

    @Test
    fun `should chunk a procedure from numbered steps`() {
        val content = """
            = SESSION_CHECKLIST.adoc

            == Hook d'Ouverture

            . Lire AGENT.adoc — regles absolues
            . Lire INDEX.adoc — roadmap
            . Verifier git status
            . Verifier que le build compile
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "SESSION_CHECKLIST.adoc")

        val procedures = chunks.filter { it.chunkType == ChunkType.PROCEDURE }
        assertTrue(procedures.isNotEmpty(), "Should detect PROCEDURE chunks")
        val hook = procedures.first { it.content.contains("Hook d'Ouverture") }
        assertEquals(ChunkType.PROCEDURE, hook.chunkType)
        assertEquals(TaxonomyVerb.VALIDER, hook.verb)
        assertTrue(hook.content.contains("Lire AGENT.adoc"))
    }

    @Test
    fun `should chunk metadata from document header`() {
        val content = """
            = INDEX — Codebase Gradle
            :toc:
            :date: 2026-06-11
            :session-en-cours: 092

            _Derniere mise a jour_ : 2026-06-11 (Session 091)
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "INDEX.adoc")

        val metadata = chunks.filter { it.chunkType == ChunkType.METADATA }
        assertTrue(metadata.isNotEmpty(), "Should detect METADATA chunks")
        val dateMeta = metadata.first { it.content.contains("2026-06-11") }
        assertEquals(ChunkType.METADATA, dateMeta.chunkType)
        assertEquals("INDEX.adoc", dateMeta.sourceFile)
    }

    @Test
    fun `should chunk a constraint from technical limit`() {
        val content = """
            = AGENT.adoc

            == Contexte

            * **1 fichier a la fois** : Modifier un seul fichier avant de passer au suivant
            * **Contexte leger** : Maximum 50k tokens EAGER (~3000 lignes)
            * **Regle d'or** : Mieux vaut 5 sessions de 20 minutes qu'une session de 2 heures
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val constraints = chunks.filter { it.chunkType == ChunkType.CONSTRAINT }
        assertTrue(constraints.isNotEmpty(), "Should detect CONSTRAINT chunks")
        val tokenLimit = constraints.first { it.content.contains("50k tokens") }
        assertEquals(ChunkType.CONSTRAINT, tokenLimit.chunkType)
        assertEquals(TaxonomyVerb.VALIDER, tokenLimit.verb)
    }

    @Test
    fun `should assign correct domain from source file path`() {
        val content = """
            = AGENT.adoc — Codebase
            == Regles
            **INTERDICTION** de commit sans permission.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "codebase-plugin/AGENT.adoc")

        val rule = chunks.first { it.chunkType == ChunkType.RULE }
        assertEquals("codebase", rule.domain)
    }

    @Test
    fun `should assign correct DAG level from content patterns`() {
        val content = """
            = AGENT.adoc
            == Projet
            **Nom** : codebase-gradle — Proprietaire du fine-tuning LLM, N1.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val concept = chunks.first { it.chunkType == ChunkType.CONCEPT && it.content.contains("N1") }
        assertEquals(DagLevel.N1, concept.dagLevel)
    }

    @Test
    fun `should assign correct circle from content patterns`() {
        val content = """
            = AGENT.adoc
            Ce projet vit dans `foundry/codebase-gradle/` → *cercle 4 (public, Apache 2.0)*.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val concept = chunks.first { it.chunkType == ChunkType.CONCEPT }
        assertEquals(4, concept.circle)
    }

    @Test
    fun `should generate SHA-256 checksum for each chunk`() {
        val content = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        for (chunk in chunks) {
            assertTrue(chunk.checksum.length == 64, "SHA-256 checksum should be 64 hex chars")
            assertTrue(chunk.checksum.matches(Regex("[0-9a-f]+")), "Checksum should be hex")
        }
    }

    @Test
    fun `should generate unique id per chunk`() {
        val content = """
            = AGENT.adoc
            == Regle 1
            **INTERDICTION** A.
            == Regle 2
            **INTERDICTION** B.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val ids = chunks.map { it.id }.toSet()
        assertEquals(chunks.size, ids.size, "All chunk IDs should be unique")
    }

    @Test
    fun `should handle empty content gracefully`() {
        val chunks = chunker.chunk("", sourceFile = "empty.adoc")
        assertTrue(chunks.isEmpty(), "Empty content should produce no chunks")
    }

    @Test
    fun `should handle content with only whitespace`() {
        val chunks = chunker.chunk("   \n  \n   ", sourceFile = "blank.adoc")
        assertTrue(chunks.isEmpty(), "Whitespace-only content should produce no chunks")
    }

    @Test
    fun `should extract verb INTERDIRE from interdiction patterns`() {
        val content = """
            **INTERDICTION FORMELLE** de commit/push/merge sans permission.
            **NE JAMAIS** lancer de tests sans permission.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val rules = chunks.filter { it.chunkType == ChunkType.RULE }
        assertEquals(2, rules.size, "Should detect two RULE chunks")
        assertTrue(rules.all { it.verb == TaxonomyVerb.INTERDIRE })
    }

    @Test
    fun `should extract verb GENERER from generation patterns`() {
        val content = """
            == Pipeline
            La tache `generateSPG` produit le scenario pedagogique global.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "BACKLOG.adoc")

        val generate = chunks.first { it.verb == TaxonomyVerb.GENERER }
        assertEquals(ChunkType.CONCEPT, generate.chunkType)
    }

    @Test
    fun `should extract verb COLLECTER from collection patterns`() {
        val content = """
            == Acquisition
            La tache `collectFromCorpus` importe les donnees AFNOR/REAC.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "BACKLOG.adoc")

        val collect = chunks.first { it.verb == TaxonomyVerb.COLLECTER }
        assertEquals(ChunkType.CONCEPT, collect.chunkType)
    }

    @Test
    fun `should extract verb TRANSFORMER from transformation patterns`() {
        val content = """
            == Conversion
            La tache `transformToPdf` convertit l'AsciiDoc en PDF.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "BACKLOG.adoc")

        val transform = chunks.first { it.verb == TaxonomyVerb.TRANSFORMER }
        assertEquals(ChunkType.CONCEPT, transform.chunkType)
    }

    @Test
    fun `should extract verb DEPLOYER from deployment patterns`() {
        val content = """
            == Publication
            La tache `deployToGhPages` publie le site statique.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "BACKLOG.adoc")

        val deploy = chunks.first { it.verb == TaxonomyVerb.DEPLOYER }
        assertEquals(ChunkType.CONCEPT, deploy.chunkType)
    }

    @Test
    fun `should assign weight 1_0 for CRITIQUE or BLOQUANT keywords`() {
        val content = """
            **CRITIQUE** : Cette regle est absolue.
            **BLOQUANT** : Ne pas ignorer.
            Simple remarque.
        """.trimIndent()

        val chunks = chunker.chunk(content, sourceFile = "AGENT.adoc")

        val critique = chunks.first { it.content.contains("CRITIQUE") }
        assertEquals(1.0, critique.weight, 0.01)

        val bloquant = chunks.first { it.content.contains("BLOQUANT") }
        assertEquals(1.0, bloquant.weight, 0.01)

        val simple = chunks.first { it.content.contains("Simple remarque") }
        assertTrue(simple.weight < 1.0, "Non-critical chunk should have weight < 1.0")
    }
}
