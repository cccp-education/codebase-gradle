package codebase.koog.expert

import codebase.infrastructure.PostgresFixture
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

class ExpertCallRepositoryTest {

    companion object {
        lateinit var connectionFactory: ConnectionFactory
        lateinit var repository: ExpertCallRepository

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = PostgresqlConnectionConfiguration.builder()
                .host(PostgresFixture.host)
                .port(PostgresFixture.port)
                .database(PostgresFixture.databaseName)
                .username(PostgresFixture.username)
                .password(PostgresFixture.password)
                .build()
            connectionFactory = PostgresqlConnectionFactory(config)
            repository = ExpertCallRepository(connectionFactory)
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        runBlocking {
            val conn = Mono.from(connectionFactory.create()).awaitSingle()
            try {
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS expert_calls").execute())
                    .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L).awaitSingle()
            } finally {
                Mono.from(conn.close()).subscribe()
            }
        }
    }

    private fun createRecord(
        id: String = UUID.randomUUID().toString(),
        taskId: String = "task-001",
        domainName: String = "kotlin",
        subtaskType: String = "code_generation",
        prompt: String = "Write a plugin",
        anonymizedPrompt: String = "Write a plugin",
        output: String = "class MyPlugin {}",
        confidenceScore: Double = 0.95,
        promptTokens: Int = 100,
        completionTokens: Int = 200,
        validationPassed: Boolean = true,
        error: String? = null
    ) = ExpertCallRecord(
        id = id,
        taskId = taskId,
        domainName = domainName,
        subtaskType = subtaskType,
        prompt = prompt,
        anonymizedPrompt = anonymizedPrompt,
        output = output,
        confidenceScore = confidenceScore,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        validationPassed = validationPassed,
        error = error
    )

    @Test
    fun `initSchema creates table`() = runBlocking {
        repository.initSchema()

        val conn = Mono.from(connectionFactory.create()).awaitSingle()
        try {
            val tables = Mono.from(
                conn.createStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_name = 'expert_calls'"
                ).execute()
            ).flatMapMany { result ->
                result.map { row, _ -> row.get(0, String::class.java)!! }
            }.collectList().awaitSingle()

            assertTrue(tables.contains("expert_calls"))
        } finally {
            Mono.from(conn.close()).subscribe()
        }
    }

    @Test
    fun `saveCall persists record`() = runBlocking {
        repository.initSchema()
        val record = createRecord()

        val saved = repository.saveCall(record)
        assertTrue(saved)

        val retrieved = repository.getCall(record.id)
        assertNotNull(retrieved)
        assertEquals(record.id, retrieved!!.id)
        assertEquals(record.taskId, retrieved.taskId)
        assertEquals(record.domainName, retrieved.domainName)
        assertEquals(record.output, retrieved.output)
        assertEquals(record.confidenceScore, retrieved.confidenceScore)
        assertEquals(record.validationPassed, retrieved.validationPassed)
    }

    @Test
    fun `saveCall updates existing record`() = runBlocking {
        repository.initSchema()
        val record = createRecord()
        repository.saveCall(record)

        val updated = record.copy(output = "Updated output", confidenceScore = 0.8)
        repository.saveCall(updated)

        val retrieved = repository.getCall(record.id)
        assertNotNull(retrieved)
        assertEquals("Updated output", retrieved!!.output)
        assertEquals(0.8, retrieved.confidenceScore)
    }

    @Test
    fun `saveCalls persists multiple records`() = runBlocking {
        repository.initSchema()
        val records = listOf(
            createRecord(id = "call-1", domainName = "kotlin"),
            createRecord(id = "call-2", domainName = "docs"),
            createRecord(id = "call-3", domainName = "kotlin")
        )

        val saved = repository.saveCalls(records)
        assertEquals(3, saved)

        val count = repository.countCalls()
        assertEquals(3, count)
    }

    @Test
    fun `getCall returns null for unknown id`() = runBlocking {
        repository.initSchema()

        val result = repository.getCall("nonexistent")
        assertNull(result)
    }

    @Test
    fun `listCallsByTask filters by taskId`() = runBlocking {
        repository.initSchema()
        repository.saveCalls(listOf(
            createRecord(id = "call-1", taskId = "task-A"),
            createRecord(id = "call-2", taskId = "task-A"),
            createRecord(id = "call-3", taskId = "task-B")
        ))

        val taskACalls = repository.listCallsByTask("task-A")
        assertEquals(2, taskACalls.size)
        assertTrue(taskACalls.all { it.taskId == "task-A" })

        val taskBCalls = repository.listCallsByTask("task-B")
        assertEquals(1, taskBCalls.size)
    }

    @Test
    fun `listCallsByDomain filters by domain`() = runBlocking {
        repository.initSchema()
        repository.saveCalls(listOf(
            createRecord(id = "call-1", domainName = "kotlin"),
            createRecord(id = "call-2", domainName = "kotlin"),
            createRecord(id = "call-3", domainName = "docs")
        ))

        val kotlinCalls = repository.listCallsByDomain("kotlin")
        assertEquals(2, kotlinCalls.size)
        assertTrue(kotlinCalls.all { it.domainName == "kotlin" })

        val docsCalls = repository.listCallsByDomain("docs")
        assertEquals(1, docsCalls.size)
    }

    @Test
    fun `listCallsByDomain respects limit`() = runBlocking {
        repository.initSchema()
        val records = (1..10).map { i ->
            createRecord(id = "call-$i", domainName = "kotlin")
        }
        repository.saveCalls(records)

        val limited = repository.listCallsByDomain("kotlin", limit = 3)
        assertEquals(3, limited.size)
    }

    @Test
    fun `countCalls returns total`() = runBlocking {
        repository.initSchema()
        assertEquals(0, repository.countCalls())

        repository.saveCall(createRecord(id = "call-1"))
        assertEquals(1, repository.countCalls())

        repository.saveCall(createRecord(id = "call-2"))
        assertEquals(2, repository.countCalls())
    }

    @Test
    fun `countCallsByDomain groups by domain`() = runBlocking {
        repository.initSchema()
        repository.saveCalls(listOf(
            createRecord(id = "call-1", domainName = "kotlin"),
            createRecord(id = "call-2", domainName = "kotlin"),
            createRecord(id = "call-3", domainName = "docs"),
            createRecord(id = "call-4", domainName = "kotlin"),
            createRecord(id = "call-5", domainName = "docs")
        ))

        val counts = repository.countCallsByDomain()
        assertEquals(3, counts["kotlin"])
        assertEquals(2, counts["docs"])
    }

    @Test
    fun `callsSince filters by timestamp`() = runBlocking {
        repository.initSchema()
        val old = createRecord(id = "old-call")
        repository.saveCall(old)

        Thread.sleep(100)
        val cutoff = Instant.now()

        val recent = createRecord(id = "recent-call")
        repository.saveCall(recent)

        val sinceCutoff = repository.callsSince(cutoff)
        assertEquals(1, sinceCutoff.size)
        assertEquals("recent-call", sinceCutoff[0].id)
    }

    @Test
    fun `saveCall with error field`() = runBlocking {
        repository.initSchema()
        val record = createRecord(
            id = "failed-call",
            validationPassed = false,
            error = "Expert unavailable",
            output = ""
        )

        repository.saveCall(record)
        val retrieved = repository.getCall("failed-call")

        assertNotNull(retrieved)
        assertFalse(retrieved!!.validationPassed)
        assertEquals("Expert unavailable", retrieved.error)
        assertTrue(retrieved.output.isEmpty())
    }

    @Test
    fun `saveCall with anonymized prompt`() = runBlocking {
        repository.initSchema()
        val record = createRecord(
            id = "anon-call",
            prompt = "Use API key: sk-12345",
            anonymizedPrompt = "Use API key: ***"
        )

        repository.saveCall(record)
        val retrieved = repository.getCall("anon-call")

        assertNotNull(retrieved)
        assertEquals("Use API key: sk-12345", retrieved!!.prompt)
        assertEquals("Use API key: ***", retrieved.anonymizedPrompt)
    }
}
