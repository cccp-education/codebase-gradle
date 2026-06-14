package codebase.scenarios

import codebase.koog.planning.RollbackStrategy
import codebase.koog.planning.VibecodingStep
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VibecodingPlanSteps(private val world: VibecodingPlanWorld) {

    @Before("@epic_x_1")
    fun reset() {
        world.reset()
    }

    @Given("an empty VibecodingPlan builder")
    fun `empty builder`() {
        world.reset()
    }

    @When("I add step {string} with gradle task {string} and expected output {string}")
    fun `add step`(description: String, gradleTask: String, expectedOutput: String) {
        world.steps.add(VibecodingStep(description, gradleTask, expectedOutput))
    }

    @When("I add step {string} with gradle task {string} expected output {string} and maxRetries {int}")
    fun `add step with maxRetries`(description: String, gradleTask: String, expectedOutput: String, maxRetries: Int) {
        world.steps.add(VibecodingStep(description, gradleTask, expectedOutput, maxRetries = maxRetries))
    }

    @When("I add step {string} with gradle task {string} expected output {string} and verifyHook {string}")
    fun `add step with verifyHook`(description: String, gradleTask: String, expectedOutput: String, verifyHook: String) {
        world.steps.add(VibecodingStep(description, gradleTask, expectedOutput, verifyHook = verifyHook))
    }

    @When("I set rollback strategy to {word}")
    fun `set strategy`(strategyName: String) {
        world.strategy = RollbackStrategy.valueOf(strategyName)
    }

    @Then("the plan has {int} steps")
    fun `plan has N steps`(count: Int) {
        world.build()
        assertNotNull(world.plan)
        assertEquals(count, world.plan!!.steps.size)
    }

    @Then("step {int} has description {string}")
    fun `step N has description`(index: Int, description: String) {
        assertNotNull(world.plan)
        assertEquals(description, world.plan!!.steps[index - 1].description)
    }

    @Then("the plan has strategy {word}")
    fun `plan has strategy`(strategyName: String) {
        world.build()
        assertNotNull(world.plan)
        assertEquals(RollbackStrategy.valueOf(strategyName), world.plan!!.rollbackStrategy)
    }

    @Then("step {string} has maxRetries {int}")
    fun `step has maxRetries`(description: String, maxRetries: Int) {
        world.build()
        assertNotNull(world.plan)
        val step = world.plan!!.steps.find { it.description == description }
        assertNotNull(step, "Step '$description' not found")
        assertEquals(maxRetries, step.maxRetries)
    }

    @Then("step {string} has verifyHook {string}")
    fun `step has verifyHook`(description: String, verifyHook: String) {
        world.build()
        assertNotNull(world.plan)
        val step = world.plan!!.steps.find { it.description == description }
        assertNotNull(step, "Step '$description' not found")
        assertEquals(verifyHook, step.verifyHook)
    }

    @Then("step {string} has no verifyHook")
    fun `step has no verifyHook`(description: String) {
        world.build()
        assertNotNull(world.plan)
        val step = world.plan!!.steps.find { it.description == description }
        assertNotNull(step, "Step '$description' not found")
        assertNull(step.verifyHook)
    }
}
