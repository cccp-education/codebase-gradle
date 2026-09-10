package codebase.scenarios

import codebase.koog.svo.AutonomousSessionOrchestrator
import codebase.koog.svo.SessionPromptResolver
import codebase.koog.svo.SessionPromptSource
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@autonomous-session` scenarios (EPIC SVO-4/5).
 *
 * Steps are prefixed "autonomous session" to avoid glue collisions
 * (pattern S-088). Fakes in-memory: the vibecoding loop is a lambda
 * recording the executed intentions — no Gradle task, no network, no LLM.
 */
class AutonomousSessionSteps(private val world: AutonomousSessionWorld) {

    @Given("an autonomous session world is initialized")
    fun `autonomous session world initialized`() {
        world.ensureInitialized()
        assertNotNull(world, "AutonomousSessionWorld should be instantiated by PicoContainer")
    }

    @Given("a borough dir without a backlog file")
    fun `borough dir without backlog`() {
        world.boroughDir = java.nio.file.Files.createTempDirectory("svo-no-backlog")
        world.executedIntentions.clear()
    }

    @Given("a borough dir with a PROMPT_REPRISE mentioning {string}")
    fun `borough dir with prompt reprise`(mention: String) {
        world.boroughDir = java.nio.file.Files.createTempDirectory("svo-prompt-reprise")
        world.boroughDir.resolve("PROMPT_REPRISE.adoc").toFile().writeText("= Prompt de Reprise — $mention")
        world.executedIntentions.clear()
    }

    @Given("a borough dir with a never-ending BACKLOG")
    fun `borough dir with never ending backlog`() {
        world.boroughDir = java.nio.file.Files.createTempDirectory("svo-loop")
        world.boroughDir.resolve("BACKLOG.adoc").toFile().writeText(
            "== EPIC Loop 🟡 EN COURS\n* [ ] endless item 0"
        )
        world.executedIntentions.clear()
    }

    @Given("a borough dir whose BACKLOG is already complete")
    fun `borough dir with complete backlog`() {
        world.boroughDir = java.nio.file.Files.createTempDirectory("svo-complete")
        world.boroughDir.resolve("BACKLOG.adoc").toFile().writeText(
            "== EPIC Done ✅ TERMINÉ\n* [x] everything is done"
        )
        world.executedIntentions.clear()
    }

    @When("the autonomous session is orchestrated with transferred prompt {string}")
    fun `orchestrated with transferred prompt`(transferredPrompt: String) {
        orchestrate(transferredPrompt = transferredPrompt, borough = "codebase", neverEnding = false)
    }

    @When("the autonomous session is orchestrated without transferred prompt")
    fun `orchestrated without transferred prompt`() {
        orchestrate(borough = "codebase", neverEnding = isNeverEndingWorld())
    }

    private fun isNeverEndingWorld(): Boolean =
        world.boroughDir.resolve("BACKLOG.adoc").toFile().takeIf { it.isFile }
            ?.readText()?.contains("EPIC Loop") == true

    private fun orchestrate(borough: String, neverEnding: Boolean, transferredPrompt: String? = null) {
        val dir = world.boroughDir.toFile()
        var counter = 0
        world.report = AutonomousSessionOrchestrator.orchestrate(
            borough = borough,
            prompt = transferredPrompt,
            projectDir = dir,
            maxChainedSessions = 3,
            maxActionsPerSession = 2,
            runSession = { state ->
                world.executedIntentions += state.intention
                if (neverEnding) {
                    counter++
                    dir.resolve("BACKLOG.adoc").writeText(
                        "== EPIC Loop 🟡 EN COURS\n* [ ] endless item $counter"
                    )
                }
                "done ${world.executedIntentions.size}"
            },
            auditAppend = {},
        )
        world.resumptionContent = world.report?.resumptionPrompt
    }

    @Then("one session consumed the transferred prompt")
    fun `one session consumed transferred prompt`() {
        val executed = world.executedIntentions
        assertEquals(2, executed.size, "mission + brainstorming chain-up (D8)")
        assertTrue(
            executed.first().contains("Work EPIC SVO-4 now"),
            "transferred prompt must feed the first loop",
        )
    }

    @Then("the brainstorming session ran the intention {string}")
    fun `brainstorming session ran intention`(expected: String) {
        val last = world.executedIntentions.lastOrNull().orEmpty()
        assertTrue(last.contains(expected), "second session intention must be the brainstorming one")
    }

    @Then("a resumption prompt was written under the vibecoding build dir")
    fun `resumption prompt written`() {
        val path = world.report?.resumptionPromptPath.orEmpty()
        assertTrue(path.contains("build${File.separatorChar}vibecoding") || path.contains("build/vibecoding"), "path must live under build/vibecoding/")
        assertTrue(File(path).exists(), "resumption prompt must be physically written")
    }

    @Then("the first session consumed the borough default prompt")
    fun `first session consumed borough default`() {
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = null,
            borough = "codebase",
            projectDir = world.boroughDir.toFile(),
        )
        assertEquals(SessionPromptSource.BOROUGH_DEFAULT, resolved.source)
        world.lastResolvedPrompt = resolved.prompt
    }

    @Then("the borough default prompt carries the backlog open items")
    fun `borough default carries open items`() {
        val prompt = world.lastResolvedPrompt.orEmpty()
        assertTrue(
            prompt.contains("work the backlog, first open item first"),
            "default prompt orients on the backlog",
        )
    }

    @Then("three chained sessions executed")
    fun `three chained sessions`() {
        assertEquals(3, world.report?.sessionsExecuted, "D9 bound reached")
    }

    @Then("the chain stopped with reason {string}")
    fun `chain stopped with reason`(reason: String) {
        assertEquals(reason, world.report?.stopReason)
    }

    @Then("the resumption prompt orients on the remaining backlog")
    fun `resumption prompt orients remaining backlog`() {
        assertTrue(
            world.resumptionContent.orEmpty().contains("endless item"),
            "remaining backlog item feeds the resumption prompt",
        )
    }

    @Then("two sessions executed including the brainstorming chain-up")
    fun `two sessions including brainstorming`() {
        assertEquals(2, world.report?.sessionsExecuted, "mission + brainstorming chain-up")
    }

    @Then("the brainstorming intention names the borough")
    fun `brainstorming intention names borough`() {
        assertTrue(
            world.report?.brainstormingIntention?.contains("codebase") == true,
            "brainstorm intention must name the borough",
        )
    }
}
