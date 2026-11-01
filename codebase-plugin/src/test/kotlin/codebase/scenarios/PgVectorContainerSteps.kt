package codebase.scenarios

import codebase.infrastructure.PostgresFixture
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.slf4j.LoggerFactory
import java.sql.DriverManager

class PgVectorContainerSteps {
    companion object {
        private val log = LoggerFactory.getLogger(PgVectorContainerSteps::class.java)
    }

    @When("I start a pgvector container")
    fun startAPgvectorContainer() {
        log.info("pgvector container ready (PostgresFixture)")
    }

    @When("I stop the container")
    fun stopTheContainer() {
        log.info("pgvector container managed by PostgresFixture (no-op)")
    }

    @Then("the container is running")
    fun theContainerIsRunning() {
        assert(PostgresFixture.container.isRunning) { "Container should be running" }
        log.info("Container is running: ${PostgresFixture.container.containerId}")
    }

    @Then("the container is not running")
    fun theContainerIsNotRunning() {
        assert(!PostgresFixture.container.isRunning) { "Container should not be running" }
        log.info("Container is stopped")
    }

    @Then("the vector extension is available")
    fun theVectorExtensionIsAvailable() {
        DriverManager.getConnection(PostgresFixture.jdbcUrl, PostgresFixture.username, PostgresFixture.password).use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT count(*) FROM pg_available_extensions WHERE name = 'vector'")
                rs.next()
                val count = rs.getInt(1)
                assert(count > 0) { "pgvector extension should be available, got count=$count" }
                log.info("pgvector extension is available")
            }
        }
    }

    @Then("I can create a table with a vector\\({int}) column")
    fun iCanCreateATableWithAVectorColumn(dimensions: Int) {
        DriverManager.getConnection(PostgresFixture.jdbcUrl, PostgresFixture.username, PostgresFixture.password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                stmt.execute("DROP TABLE IF EXISTS test_embeddings")
                stmt.execute("CREATE TABLE test_embeddings (id SERIAL PRIMARY KEY, embedding vector($dimensions))")
                log.info("Table test_embeddings created with vector($dimensions)")
            }
        }
    }
}
