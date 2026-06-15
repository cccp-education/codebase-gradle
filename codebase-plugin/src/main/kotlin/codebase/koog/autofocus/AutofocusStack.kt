package codebase.koog.autofocus

class AutofocusStack {
    private val stack = ArrayDeque<AutofocusLevel>()

    fun push(level: AutofocusLevel) {
        stack.addLast(level)
    }

    fun pop(): AutofocusLevel {
        if (stack.isEmpty()) throw IllegalStateException("AutofocusStack underflow: pop() called on empty stack")
        return stack.removeLast()
    }

    fun currentLevel(): AutofocusLevel? = stack.lastOrNull()

    fun isEmpty(): Boolean = stack.isEmpty()

    fun size(): Int = stack.size

    fun clear() {
        stack.clear()
    }
}
