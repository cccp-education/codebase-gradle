package codebase.koog.expert

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface ExpertCallRepository {

    suspend fun initSchema()

    suspend fun saveCall(record: ExpertCallRecord): Boolean

    suspend fun saveCalls(records: List<ExpertCallRecord>): Int

    suspend fun getCall(id: String): ExpertCallRecord?

    suspend fun listCallsByTask(taskId: String): List<ExpertCallRecord>

    suspend fun listCallsByDomain(domainName: String, limit: Int = 50): List<ExpertCallRecord>

    suspend fun countCalls(): Long

    suspend fun countCallsByDomain(): Map<String, Long>

    suspend fun callsSince(since: Instant): List<ExpertCallRecord>

    companion object {
        operator fun invoke(connectionFactory: ConnectionFactory): ExpertCallRepository =
            R2dbcExpertCallRepository(connectionFactory)
    }
}

private class R2dbcExpertCallRepository(
    private val connectionFactory: ConnectionFactory
) : ExpertCallRepository {

    private suspend fun <R> withConnection(block: suspend (conn: Connection) -> R): R {
        val conn = Mono.from(connectionFactory.create()).awaitSingle()
        return try {
            block(conn)
        } finally {
            Mono.from(conn.close()).subscribe()
        }
    }

    override suspend fun initSchema() {
        withConnection { conn ->
            Mono.from(
                conn.createStatement("""
                    CREATE TABLE IF NOT EXISTS expert_calls (
                        id VARCHAR(64) PRIMARY KEY,
                        task_id VARCHAR(64) NOT NULL,
                        domain_name VARCHAR(64) NOT NULL,
                        subtask_type VARCHAR(64) NOT NULL,
                        prompt TEXT NOT NULL,
                        anonymized_prompt TEXT NOT NULL,
                        output TEXT NOT NULL,
                        confidence_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                        prompt_tokens INT NOT NULL DEFAULT 0,
                        completion_tokens INT NOT NULL DEFAULT 0,
                        validation_passed BOOLEAN NOT NULL DEFAULT false,
                        error TEXT,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                    )
                """.trimIndent()).execute()
            ).flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L).awaitSingle()
        }
    }

    override suspend fun saveCall(record: ExpertCallRecord): Boolean = withConnection { conn ->
        val s = conn.createStatement("""
            INSERT INTO expert_calls (id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at)
            VALUES (${'$'}1, ${'$'}2, ${'$'}3, ${'$'}4, ${'$'}5, ${'$'}6, ${'$'}7, ${'$'}8, ${'$'}9, ${'$'}10, ${'$'}11, ${'$'}12, ${'$'}13)
            ON CONFLICT (id) DO UPDATE SET
                output = EXCLUDED.output,
                confidence_score = EXCLUDED.confidence_score,
                prompt_tokens = EXCLUDED.prompt_tokens,
                completion_tokens = EXCLUDED.completion_tokens,
                validation_passed = EXCLUDED.validation_passed,
                error = EXCLUDED.error
        """.trimIndent())
        s.bind("${'$'}1", record.id)
        s.bind("${'$'}2", record.taskId)
        s.bind("${'$'}3", record.domainName)
        s.bind("${'$'}4", record.subtaskType)
        s.bind("${'$'}5", record.prompt)
        s.bind("${'$'}6", record.anonymizedPrompt)
        s.bind("${'$'}7", record.output)
        s.bind("${'$'}8", record.confidenceScore)
        s.bind("${'$'}9", record.promptTokens)
        s.bind("${'$'}10", record.completionTokens)
        s.bind("${'$'}11", record.validationPassed)
        if (record.error != null) s.bind("${'$'}12", record.error) else s.bindNull("${'$'}12", String::class.java)
        s.bind("${'$'}13", record.createdAt)
        val rows = Mono.from(s.execute())
            .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L).awaitSingle()
        rows > 0
    }

    override suspend fun saveCalls(records: List<ExpertCallRecord>): Int = withConnection { conn ->
        var saved = 0
        for (record in records) {
            val s = conn.createStatement("""
                INSERT INTO expert_calls (id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at)
                VALUES (${'$'}1, ${'$'}2, ${'$'}3, ${'$'}4, ${'$'}5, ${'$'}6, ${'$'}7, ${'$'}8, ${'$'}9, ${'$'}10, ${'$'}11, ${'$'}12, ${'$'}13)
                ON CONFLICT (id) DO UPDATE SET
                    output = EXCLUDED.output,
                    confidence_score = EXCLUDED.confidence_score,
                    prompt_tokens = EXCLUDED.prompt_tokens,
                    completion_tokens = EXCLUDED.completion_tokens,
                    validation_passed = EXCLUDED.validation_passed,
                    error = EXCLUDED.error
            """.trimIndent())
            s.bind("${'$'}1", record.id)
            s.bind("${'$'}2", record.taskId)
            s.bind("${'$'}3", record.domainName)
            s.bind("${'$'}4", record.subtaskType)
            s.bind("${'$'}5", record.prompt)
            s.bind("${'$'}6", record.anonymizedPrompt)
            s.bind("${'$'}7", record.output)
            s.bind("${'$'}8", record.confidenceScore)
            s.bind("${'$'}9", record.promptTokens)
            s.bind("${'$'}10", record.completionTokens)
            s.bind("${'$'}11", record.validationPassed)
            if (record.error != null) s.bind("${'$'}12", record.error) else s.bindNull("${'$'}12", String::class.java)
            s.bind("${'$'}13", record.createdAt)
            val rows = Mono.from(s.execute())
                .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L).awaitSingle()
            if (rows > 0) saved++
        }
        saved
    }

    override suspend fun getCall(id: String): ExpertCallRecord? = withConnection { conn ->
        val list: List<ExpertCallRecord> = Mono.from(
            conn.createStatement("""
                SELECT id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at
                FROM expert_calls WHERE id = ${'$'}1
            """.trimIndent())
                .bind("${'$'}1", id)
                .execute()
        ).flatMapMany { result ->
            result.map { row, _ ->
                ExpertCallRecord(
                    id = row.get("id", String::class.java)!!,
                    taskId = row.get("task_id", String::class.java)!!,
                    domainName = row.get("domain_name", String::class.java)!!,
                    subtaskType = row.get("subtask_type", String::class.java)!!,
                    prompt = row.get("prompt", String::class.java)!!,
                    anonymizedPrompt = row.get("anonymized_prompt", String::class.java)!!,
                    output = row.get("output", String::class.java)!!,
                    confidenceScore = row.get("confidence_score", Double::class.java)!!,
                    promptTokens = row.get("prompt_tokens", Int::class.java)!!,
                    completionTokens = row.get("completion_tokens", Int::class.java)!!,
                    validationPassed = row.get("validation_passed", Boolean::class.java)!!,
                    error = row.get("error", String::class.java),
                    createdAt = row.get("created_at", Instant::class.java)!!
                )
            }
        }.collectList().awaitSingle()
        list.firstOrNull()
    }

    override suspend fun listCallsByTask(taskId: String): List<ExpertCallRecord> = withConnection { conn ->
        Mono.from(
            conn.createStatement("""
                SELECT id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at
                FROM expert_calls WHERE task_id = ${'$'}1 ORDER BY created_at
            """.trimIndent())
                .bind("${'$'}1", taskId)
                .execute()
        ).flatMapMany { result ->
            result.map { row, _ ->
                ExpertCallRecord(
                    id = row.get("id", String::class.java)!!,
                    taskId = row.get("task_id", String::class.java)!!,
                    domainName = row.get("domain_name", String::class.java)!!,
                    subtaskType = row.get("subtask_type", String::class.java)!!,
                    prompt = row.get("prompt", String::class.java)!!,
                    anonymizedPrompt = row.get("anonymized_prompt", String::class.java)!!,
                    output = row.get("output", String::class.java)!!,
                    confidenceScore = row.get("confidence_score", Double::class.java)!!,
                    promptTokens = row.get("prompt_tokens", Int::class.java)!!,
                    completionTokens = row.get("completion_tokens", Int::class.java)!!,
                    validationPassed = row.get("validation_passed", Boolean::class.java)!!,
                    error = row.get("error", String::class.java),
                    createdAt = row.get("created_at", Instant::class.java)!!
                )
            }
        }.collectList().awaitSingle()
    }

    override suspend fun listCallsByDomain(domainName: String, limit: Int): List<ExpertCallRecord> = withConnection { conn ->
        Mono.from(
            conn.createStatement("""
                SELECT id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at
                FROM expert_calls WHERE domain_name = ${'$'}1 ORDER BY created_at DESC LIMIT ${'$'}2
            """.trimIndent())
                .bind("${'$'}1", domainName)
                .bind("${'$'}2", limit)
                .execute()
        ).flatMapMany { result ->
            result.map { row, _ ->
                ExpertCallRecord(
                    id = row.get("id", String::class.java)!!,
                    taskId = row.get("task_id", String::class.java)!!,
                    domainName = row.get("domain_name", String::class.java)!!,
                    subtaskType = row.get("subtask_type", String::class.java)!!,
                    prompt = row.get("prompt", String::class.java)!!,
                    anonymizedPrompt = row.get("anonymized_prompt", String::class.java)!!,
                    output = row.get("output", String::class.java)!!,
                    confidenceScore = row.get("confidence_score", Double::class.java)!!,
                    promptTokens = row.get("prompt_tokens", Int::class.java)!!,
                    completionTokens = row.get("completion_tokens", Int::class.java)!!,
                    validationPassed = row.get("validation_passed", Boolean::class.java)!!,
                    error = row.get("error", String::class.java),
                    createdAt = row.get("created_at", Instant::class.java)!!
                )
            }
        }.collectList().awaitSingle()
    }

    override suspend fun countCalls(): Long = withConnection { conn ->
        Mono.from(
            conn.createStatement("SELECT COUNT(*) FROM expert_calls").execute()
        ).flatMap { result ->
            Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
        }.awaitSingle()
    }

    override suspend fun countCallsByDomain(): Map<String, Long> = withConnection { conn ->
        Mono.from(
            conn.createStatement("""
                SELECT domain_name, COUNT(*) as cnt FROM expert_calls GROUP BY domain_name
            """.trimIndent()).execute()
        ).flatMapMany { result ->
            result.map { row, _ ->
                row.get("domain_name", String::class.java)!! to row.get("cnt", Long::class.java)!!
            }
        }.collectMap({ it.first }, { it.second }).awaitSingle()
    }

    override suspend fun callsSince(since: Instant): List<ExpertCallRecord> = withConnection { conn ->
        Mono.from(
            conn.createStatement("""
                SELECT id, task_id, domain_name, subtask_type, prompt, anonymized_prompt, output, confidence_score, prompt_tokens, completion_tokens, validation_passed, error, created_at
                FROM expert_calls WHERE created_at >= ${'$'}1 ORDER BY created_at DESC
            """.trimIndent())
                .bind("${'$'}1", since)
                .execute()
        ).flatMapMany { result ->
            result.map { row, _ ->
                ExpertCallRecord(
                    id = row.get("id", String::class.java)!!,
                    taskId = row.get("task_id", String::class.java)!!,
                    domainName = row.get("domain_name", String::class.java)!!,
                    subtaskType = row.get("subtask_type", String::class.java)!!,
                    prompt = row.get("prompt", String::class.java)!!,
                    anonymizedPrompt = row.get("anonymized_prompt", String::class.java)!!,
                    output = row.get("output", String::class.java)!!,
                    confidenceScore = row.get("confidence_score", Double::class.java)!!,
                    promptTokens = row.get("prompt_tokens", Int::class.java)!!,
                    completionTokens = row.get("completion_tokens", Int::class.java)!!,
                    validationPassed = row.get("validation_passed", Boolean::class.java)!!,
                    error = row.get("error", String::class.java),
                    createdAt = row.get("created_at", Instant::class.java)!!
                )
            }
        }.collectList().awaitSingle()
    }
}
