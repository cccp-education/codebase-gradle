package codebase.scenarios.ocr

import codebase.koog.llm.pool.GeminiKeyPool
import codebase.koog.llm.pool.GeminiMultiAccountPool
import codebase.koog.llm.pool.GeminiPoolFactory
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Ocr1PoolSteps {

    private var pool: GeminiKeyPool? = null
    private val instancesById = mutableMapOf<String, LlmInstance>()

    @Given("a Gemini key pool with {int} keys {string}")
    fun geminiKeyPoolWithKeys(count: Int, keysCsv: String) {
        val ids = keysCsv.split(",").map { it.trim() }
        assertEquals(count, ids.size, "Key count mismatch")
        instancesById.clear()
        val instances = ids.map { id ->
            val inst = LlmInstance(
                id = id,
                baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$id",
                model = "gemini-2.5-flash",
                quota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER)
            )
            instancesById[id] = inst
            inst
        }
        pool = GeminiKeyPool(instances, rotationStrategy = RotationStrategy.ROUND_ROBIN)
    }

    @When("key {string} receives HTTP 429")
    fun keyReceivesHttp429(keyId: String) {
        val inst = instancesById[keyId] ?: error("Unknown key: $keyId")
        pool!!.markRateLimited(inst)
    }

    @When("the Gemini key pool usage is reset")
    fun geminiKeyPoolUsageReset() {
        pool!!.resetUsage()
    }

    @Then("the pool marks {string} as rate-limited")
    fun poolMarksKeyAsRateLimited(keyId: String) {
        val inst = instancesById[keyId] ?: error("Unknown key: $keyId")
        assertTrue(pool!!.isRateLimited(inst), "Key $keyId should be rate-limited")
    }

    @Then("the pool does not mark {string} as rate-limited")
    fun poolDoesNotMarkKeyAsRateLimited(keyId: String) {
        val inst = instancesById[keyId] ?: error("Unknown key: $keyId")
        assertFalse(pool!!.isRateLimited(inst), "Key $keyId should NOT be rate-limited")
    }

    @Then("the next available key is {string}")
    fun nextAvailableKeyIs(expectedId: String) {
        val next = pool!!.nextInstance()
        assertEquals(expectedId, next.id)
    }

    @Then("the next available key is one of {string}")
    fun nextAvailableKeyIsOneOf(csv: String) {
        val candidates = csv.split(",").map { it.trim() }.toSet()
        val next = pool!!.nextInstance()
        assertTrue(next.id in candidates, "Expected next key in $candidates, got ${next.id}")
    }

    private var envMap: MutableMap<String, String> = mutableMapOf()

    @Given("env vars with GEMINI_API_KEY_1={string} and GEMINI_API_KEY_2={string}")
    fun envVarsWithTwoKeys(key1: String, key2: String) {
        envMap.clear()
        envMap["GEMINI_API_KEY_1"] = key1
        envMap["GEMINI_API_KEY_2"] = key2
    }

    @Given("env vars with GEMINI_API_KEY_1={string} and GEMINI_API_KEY_2={string} and GEMINI_API_KEY_3={string}")
    fun envVarsWithThreeKeys(key1: String, key2: String, key3: String) {
        envMap.clear()
        envMap["GEMINI_API_KEY_1"] = key1
        envMap["GEMINI_API_KEY_2"] = key2
        envMap["GEMINI_API_KEY_3"] = key3
    }

    @Given("env vars with GEMINI_ACCOUNT_{int}_API_KEY_{int}={string} and GEMINI_ACCOUNT_{int}_API_KEY_{int}={string} and GEMINI_ACCOUNT_{int}_API_KEY_{int}={string}")
    fun envVarsWithMultiAccountKeys(
        acct1: Int, key1Idx: Int, key1: String,
        acct2: Int, key2Idx: Int, key2: String,
        acct3: Int, key3Idx: Int, key3: String
    ) {
        envMap.clear()
        envMap["GEMINI_ACCOUNT_${acct1}_API_KEY_${key1Idx}"] = key1
        envMap["GEMINI_ACCOUNT_${acct2}_API_KEY_${key2Idx}"] = key2
        envMap["GEMINI_ACCOUNT_${acct3}_API_KEY_${key3Idx}"] = key3
    }

    @When("I build a Gemini pool from env vars")
    fun buildGeminiPoolFromEnvVars() {
        pool = GeminiPoolFactory.fromEnvVars(envMap)
    }

    @Then("the pool has {int} instances")
    fun poolHasInstances(count: Int) {
        assertEquals(count, pool!!.size(), "Pool size should be $count, got ${pool!!.size()}")
    }

    @Then("the first instance key is {string}")
    fun firstInstanceKeyIs(expectedKey: String) {
        val inst = pool!!.instances()[0]
        val key = inst.baseUrl.substringAfter("key=")
        assertEquals(expectedKey, key, "First instance key mismatch")
    }

    @Then("the second instance key is {string}")
    fun secondInstanceKeyIs(expectedKey: String) {
        val inst = pool!!.instances()[1]
        val key = inst.baseUrl.substringAfter("key=")
        assertEquals(expectedKey, key, "Second instance key mismatch")
    }

    private var multiPool: GeminiMultiAccountPool? = null
    private var multiRateLimitedId: String? = null
    private var collectedMultiInstances: List<LlmInstance> = emptyList()

    @Given("a multi-account Gemini pool with 2 accounts and keys {string} and {string}")
    fun multiAccountPoolWithKeys(account1Csv: String, account2Csv: String) {
        val account1Keys = account1Csv.substringAfter(":").split(",").map { it.trim() }
        val account2Keys = account2Csv.substringAfter(":").split(",").map { it.trim() }
        multiPool = GeminiMultiAccountPool(mapOf(
            "account-1" to GeminiPoolFactory.fromKeys(account1Keys, idPrefix = "gemini-acct1"),
            "account-2" to GeminiPoolFactory.fromKeys(account2Keys, idPrefix = "gemini-acct2")
        ))
    }

    @Then("the multi-account pool has {int} accounts")
    fun multiAccountPoolHasAccounts(count: Int) {
        assertEquals(count, multiPool!!.accountCount())
    }

    @Then("the multi-account pool has {int} total instances")
    fun multiAccountPoolHasTotalInstances(count: Int) {
        assertEquals(count, multiPool!!.totalSize())
    }

    @When("I get {int} instances from the multi-account pool")
    fun getInstancesFromMultiPool(count: Int) {
        collectedMultiInstances = (1..count).map { multiPool!!.nextInstance() }
    }

    @Then("all {int} instances should have distinct ids")
    fun allInstancesDistinct(count: Int) {
        val ids = collectedMultiInstances.map { it.id }
        assertEquals(count, ids.toSet().size, "Instances should be distinct: $ids")
    }

    @When("the first instance is marked rate-limited")
    fun firstInstanceMarkedRateLimited() {
        val first = multiPool!!.nextInstance()
        multiRateLimitedId = first.id
        multiPool!!.markRateLimited(first)
    }

    @Then("the next instance from the multi-account pool is not the rate-limited one")
    fun nextInstanceNotRateLimited() {
        val next = multiPool!!.nextInstance()
        assertTrue(next.id != multiRateLimitedId, "Next instance ${next.id} should differ from rate-limited ${multiRateLimitedId}")
    }

    @When("I build a multi-account Gemini pool from env vars")
    fun buildMultiAccountPoolFromEnvVars() {
        multiPool = GeminiMultiAccountPool.fromEnvVars(envMap)
    }
}