package codebase.scenarios

import codebase.infrastructure.PostgresFixture
import codebase.koog.session.SessionRecord
import codebase.koog.session.SessionRepository
import vibecoding.contracts.state.VibecodingState
import java.time.Instant

class SessionRepositoryWorld {

    companion object {
        private var sharedConnectionFactory: io.r2dbc.spi.ConnectionFactory? = null
        private var sharedRepository: SessionRepository? = null

        @Synchronized
        fun ensureStarted() {
            if (sharedConnectionFactory == null) {
                val config = io.r2dbc.postgresql.PostgresqlConnectionConfiguration.builder()
                    .host(PostgresFixture.host)
                    .port(PostgresFixture.port)
                    .database(PostgresFixture.databaseName)
                    .username(PostgresFixture.username)
                    .password(PostgresFixture.password)
                    .build()
                sharedConnectionFactory = io.r2dbc.postgresql.PostgresqlConnectionFactory(config)
                sharedRepository = SessionRepository(sharedConnectionFactory!!)
            }
        }
    }

    val connectionFactory: io.r2dbc.spi.ConnectionFactory
        get() {
            ensureStarted()
            return sharedConnectionFactory!!
        }

    val repository: SessionRepository
        get() {
            ensureStarted()
            return sharedRepository!!
        }

    var lastCreatedSessionId: String? = null
    var createdSessionIds: MutableList<String> = mutableListOf()
    var lastGetResult: SessionRecord? = null
    var listedSessions: List<SessionRecord> = emptyList()
    var dashboardSummary: codebase.koog.tracking.DashboardSummary? = null
    var confidentialityCosts: Map<String, Double> = emptyMap()
    var stepCount: Int = 0
    var deletionResult: Boolean = false
    var lastError: String? = null

    fun reset() {
        lastCreatedSessionId = null
        createdSessionIds.clear()
        lastGetResult = null
        listedSessions = emptyList()
        dashboardSummary = null
        confidentialityCosts = emptyMap()
        stepCount = 0
        deletionResult = false
        lastError = null
    }
}
