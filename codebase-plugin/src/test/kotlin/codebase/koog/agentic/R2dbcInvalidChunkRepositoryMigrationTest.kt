package codebase.koog.agentic

import codebase.infrastructure.PostgresFixture
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

class R2dbcInvalidChunkRepositoryMigrationTest {

    companion object {
        private lateinit var connectionFactory: PostgresqlConnectionFactory

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
        }
    }

    @Test
    fun `migration creates invalid_chunks table`() {
        val repository = R2dbcInvalidChunkRepository(connectionFactory)
        runBlocking { repository.initSchema() }

        val tables = runBlocking {
            val conn = Mono.from(connectionFactory.create()).awaitSingle()
            try {
                Mono.from(
                    conn.createStatement(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                    ).execute()
                ).flatMapMany { result ->
                    result.map { row, _ -> row.get("table_name", String::class.java)!! }
                }.collectList().awaitSingle()
            } finally {
                Mono.from(conn.close()).subscribe()
            }
        }
        assertTrue(tables.contains("invalid_chunks"), "Expected invalid_chunks table, found $tables")
    }
}
