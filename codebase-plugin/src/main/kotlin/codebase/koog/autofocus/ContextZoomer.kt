package codebase.koog.autofocus

import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig

class ContextZoomer {

    fun zoom(
        target: AutofocusLevel,
        context: CompositeContext
    ): CompositeContext {
        return when (target) {
            AutofocusLevel.BIG_PICTURE -> context
            AutofocusLevel.ARCHITECTURE -> filterArchitecture(context)
            AutofocusLevel.MODULE -> filterModule(context)
            AutofocusLevel.IMPLEMENTATION -> filterImplementation(context)
        }
    }

    private fun filterArchitecture(context: CompositeContext): CompositeContext {
        val budget = AutofocusLevel.ARCHITECTURE.tokenBudget
        return context.copy(
            eagerSection = truncateTokens(context.eagerSection, budget / 2),
            ragSection = truncateTokens(context.ragSection, budget / 4),
            graphifySection = truncateTokens(context.graphifySection, budget / 4),
            docsSection = "",
            config = context.config
        )
    }

    private fun filterModule(context: CompositeContext): CompositeContext {
        val budget = AutofocusLevel.MODULE.tokenBudget
        return context.copy(
            eagerSection = truncateTokens(context.eagerSection, budget / 3),
            ragSection = truncateTokens(context.ragSection, budget / 3),
            graphifySection = truncateTokens(context.graphifySection, budget / 3),
            docsSection = "",
            config = context.config
        )
    }

    private fun filterImplementation(context: CompositeContext): CompositeContext {
        val budget = AutofocusLevel.IMPLEMENTATION.tokenBudget
        return context.copy(
            eagerSection = "",
            ragSection = truncateTokens(context.ragSection, budget),
            graphifySection = "",
            docsSection = "",
            config = context.config
        )
    }

    private fun truncateTokens(text: String, maxTokens: Int): String {
        if (text.isBlank()) return text
        val estimatedTokens = text.length / 4
        if (estimatedTokens <= maxTokens) return text
        val charLimit = maxTokens * 4
        return text.take(charLimit) + "\n[...truncated to $maxTokens tokens]"
    }
}
