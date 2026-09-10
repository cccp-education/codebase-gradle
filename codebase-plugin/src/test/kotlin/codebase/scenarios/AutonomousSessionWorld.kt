package codebase.scenarios

import codebase.koog.svo.AutonomousSessionReport
import java.io.File

/**
 * Shared world for `@autonomous-session` scenarios (EPIC SVO-4/5).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 * the temp borough dir, the executed-session trace, the last
 * [AutonomousSessionReport], and the resumption prompt content.
 * PicoContainer-scoped, one fresh instance per scenario via the
 * `@Given` init step (pattern S-082 — `CorpusIngestWorld`).
 * Pure BDD: no Gradle task, no LLM — the session loop is a fake.
 */
class AutonomousSessionWorld {

    var boroughDir: java.nio.file.Path = java.nio.file.Files.createTempDirectory("svo-world")
    var executedIntentions: MutableList<String> = mutableListOf()
    var report: AutonomousSessionReport? = null
    var resumptionContent: String? = null
    var lastResolvedPrompt: String? = null

    fun ensureInitialized() {
        // PicoContainer instantiates the world; nothing else to bootstrap.
    }
}
