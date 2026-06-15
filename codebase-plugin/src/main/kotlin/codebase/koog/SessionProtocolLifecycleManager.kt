package codebase.koog

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.UUID

class SessionProtocolLifecycleManager(
    private val storageDir: File
) {
    private val log = LoggerFactory.getLogger(SessionProtocolLifecycleManager::class.java)
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    private val sessionsFile: File
        get() = storageDir.resolve("session-lifecycle.json").also { it.parentFile.mkdirs() }

    private fun readAll(): MutableMap<String, SessionLifecycleState> {
        if (!sessionsFile.exists()) return mutableMapOf()
        return try {
            val type = mapper.typeFactory.constructMapType(
                MutableMap::class.java,
                String::class.java,
                SessionLifecycleState::class.java
            )
            mapper.readValue(sessionsFile, type)
        } catch (e: Exception) {
            log.warn("[Lifecycle] Failed to read sessions, starting fresh: {}", e.message)
            mutableMapOf()
        }
    }

    private fun writeAll(sessions: Map<String, SessionLifecycleState>) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(sessionsFile, sessions)
    }

    fun create(prompt: String, model: String? = null, sessionId: String? = null): SessionLifecycleState {
        val now = Instant.now()
        val state = SessionLifecycleState(
            sessionId = sessionId ?: UUID.randomUUID().toString(),
            prompt = prompt,
            model = model,
            status = LifecycleStatus.CREATED,
            parentSessionId = null,
            lastResponseJson = null,
            createdAt = now,
            updatedAt = now
        )
        val sessions = readAll()
        sessions[state.sessionId] = state
        writeAll(sessions)
        log.info("[Lifecycle] Created session {}: {}", state.sessionId, prompt)
        return state
    }

    fun resume(sessionId: String): SessionLifecycleState {
        val sessions = readAll()
        val existing = sessions[sessionId]
            ?: throw IllegalStateException("Session not found: $sessionId")

        val resumed = existing.copy(
            sessionId = UUID.randomUUID().toString(),
            parentSessionId = sessionId,
            status = LifecycleStatus.RUNNING,
            lastResponseJson = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        sessions[resumed.sessionId] = resumed
        writeAll(sessions)
        log.info("[Lifecycle] Resumed session {} as {}", sessionId, resumed.sessionId)
        return resumed
    }

    fun close(sessionId: String) {
        val sessions = readAll()
        val existing = sessions[sessionId]
            ?: throw IllegalStateException("Session not found: $sessionId")

        sessions[sessionId] = existing.copy(
            status = LifecycleStatus.CLOSED,
            updatedAt = Instant.now()
        )
        writeAll(sessions)
        log.info("[Lifecycle] Closed session {}", sessionId)
    }

    fun get(sessionId: String): SessionLifecycleState? {
        return readAll()[sessionId]
    }

    fun list(): List<SessionLifecycleState> {
        return readAll().values.sortedByDescending { it.createdAt }
    }

    fun listByStatus(status: LifecycleStatus): List<SessionLifecycleState> {
        return list().filter { it.status == status }
    }

    fun updateResponse(sessionId: String, responseJson: String) {
        val sessions = readAll()
        val existing = sessions[sessionId]
            ?: throw IllegalStateException("Session not found: $sessionId")

        val newStatus = if (existing.status == LifecycleStatus.CREATED) LifecycleStatus.RUNNING else existing.status
        sessions[sessionId] = existing.copy(
            lastResponseJson = responseJson,
            status = newStatus,
            updatedAt = Instant.now()
        )
        writeAll(sessions)
        log.debug("[Lifecycle] Updated response for session {}", sessionId)
    }

    fun delete(sessionId: String): Boolean {
        val sessions = readAll()
        val removed = sessions.remove(sessionId) != null
        if (removed) {
            writeAll(sessions)
            log.info("[Lifecycle] Deleted session {}", sessionId)
        }
        return removed
    }
}
