package codebase.koog.discovery

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class TaskDiscoveryRegistrarAllowlistTest {

    @Test
    fun `validateTaskName accepts safe task build`() {
        TaskDiscoveryRegistrar.validateTaskName("build")
    }

    @Test
    fun `validateTaskName accepts safe task test`() {
        TaskDiscoveryRegistrar.validateTaskName("test")
    }

    @Test
    fun `validateTaskName accepts safe task check`() {
        TaskDiscoveryRegistrar.validateTaskName("check")
    }

    @Test
    fun `validateTaskName accepts safe task compileKotlin`() {
        TaskDiscoveryRegistrar.validateTaskName("compileKotlin")
    }

    @Test
    fun `validateTaskName accepts safe task assemble`() {
        TaskDiscoveryRegistrar.validateTaskName("assemble")
    }

    @Test
    fun `validateTaskName accepts safe task jar`() {
        TaskDiscoveryRegistrar.validateTaskName("jar")
    }

    @Test
    fun `validateTaskName accepts safe task publishToMavenLocal`() {
        TaskDiscoveryRegistrar.validateTaskName("publishToMavenLocal")
    }

    @Test
    fun `validateTaskName accepts safe task tasks`() {
        TaskDiscoveryRegistrar.validateTaskName("tasks")
    }

    @Test
    fun `validateTaskName accepts safe task help`() {
        TaskDiscoveryRegistrar.validateTaskName("help")
    }

    @Test
    fun `validateTaskName accepts project task form`() {
        TaskDiscoveryRegistrar.validateTaskName(":codebase-plugin:build")
    }

    @Test
    fun `validateTaskName rejects unknown task deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("vibecode")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects publishToMavenCentral deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("publishToMavenCentral")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects publishAggregationToCentralPortal deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("publishAggregationToCentralPortal")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects clean deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("clean")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects purge deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("purge")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects wrapper deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("wrapper")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects bootstrap deny-by-default`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName("bootstrap")
        }
        assertTrue(ex.message!!.contains("not in allowlist"))
    }

    @Test
    fun `validateTaskName rejects project publishToMavenCentral via deny-list second ride`() {
        val ex = assertThrows<SecurityException> {
            TaskDiscoveryRegistrar.validateTaskName(":plugin:publishToMavenCentral")
        }
        assertTrue(ex.message!!.contains("denied pattern"))
    }
}