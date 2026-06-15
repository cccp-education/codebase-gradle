package codebase.koog.autofocus

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutofocusStackTest {

    @Test
    fun `new stack is empty`() {
        val stack = AutofocusStack()
        assertTrue(stack.isEmpty())
        assertEquals(0, stack.size())
    }

    @Test
    fun `push adds level and stack is not empty`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        assertFalse(stack.isEmpty())
        assertEquals(1, stack.size())
    }

    @Test
    fun `currentLevel returns last pushed level`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.currentLevel())
    }

    @Test
    fun `currentLevel returns null on empty stack`() {
        val stack = AutofocusStack()
        assertNull(stack.currentLevel())
    }

    @Test
    fun `pop returns last pushed level`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        val popped = stack.pop()
        assertEquals(AutofocusLevel.BIG_PICTURE, popped)
        assertTrue(stack.isEmpty())
    }

    @Test
    fun `push and pop LIFO order`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.push(AutofocusLevel.ARCHITECTURE)
        stack.push(AutofocusLevel.MODULE)
        stack.push(AutofocusLevel.IMPLEMENTATION)

        assertEquals(AutofocusLevel.IMPLEMENTATION, stack.pop())
        assertEquals(AutofocusLevel.MODULE, stack.pop())
        assertEquals(AutofocusLevel.ARCHITECTURE, stack.pop())
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.pop())
        assertTrue(stack.isEmpty())
    }

    @Test
    fun `pop on empty stack throws IllegalStateException`() {
        val stack = AutofocusStack()
        val exception = assertThrows<IllegalStateException> { stack.pop() }
        assertTrue(exception.message!!.contains("underflow"))
    }

    @Test
    fun `currentLevel reflects top after multiple pushes`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.currentLevel())

        stack.push(AutofocusLevel.IMPLEMENTATION)
        assertEquals(AutofocusLevel.IMPLEMENTATION, stack.currentLevel())

        stack.pop()
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.currentLevel())
    }

    @Test
    fun `size tracks correctly through push and pop`() {
        val stack = AutofocusStack()
        assertEquals(0, stack.size())

        stack.push(AutofocusLevel.BIG_PICTURE)
        assertEquals(1, stack.size())

        stack.push(AutofocusLevel.MODULE)
        assertEquals(2, stack.size())

        stack.pop()
        assertEquals(1, stack.size())

        stack.pop()
        assertEquals(0, stack.size())
    }

    @Test
    fun `clear empties the stack`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.push(AutofocusLevel.ARCHITECTURE)
        stack.push(AutofocusLevel.MODULE)

        stack.clear()
        assertTrue(stack.isEmpty())
        assertEquals(0, stack.size())
        assertNull(stack.currentLevel())
    }

    @Test
    fun `clear then push works`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.clear()
        stack.push(AutofocusLevel.IMPLEMENTATION)

        assertEquals(1, stack.size())
        assertEquals(AutofocusLevel.IMPLEMENTATION, stack.currentLevel())
    }

    @Test
    fun `pop after clear throws underflow`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.clear()
        assertThrows<IllegalStateException> { stack.pop() }
    }

    @Test
    fun `multiple push pop cycles work`() {
        val stack = AutofocusStack()

        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.push(AutofocusLevel.IMPLEMENTATION)
        stack.pop()
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.currentLevel())

        stack.push(AutofocusLevel.MODULE)
        stack.push(AutofocusLevel.IMPLEMENTATION)
        stack.pop()
        assertEquals(AutofocusLevel.MODULE, stack.currentLevel())

        stack.pop()
        assertEquals(AutofocusLevel.BIG_PICTURE, stack.currentLevel())
    }

    @Test
    fun `isEmpty returns true after all pops`() {
        val stack = AutofocusStack()
        stack.push(AutofocusLevel.BIG_PICTURE)
        stack.push(AutofocusLevel.MODULE)
        stack.pop()
        assertFalse(stack.isEmpty())
        stack.pop()
        assertTrue(stack.isEmpty())
    }
}
