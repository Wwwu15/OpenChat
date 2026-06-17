package com.example.aiassistant.domain.context

data class BudgetMessage(
    val role: String,
    val content: String
)

class ContextBudgeter(
    private val estimator: TokenEstimator
) {
    fun fit(messages: List<BudgetMessage>, maxTokens: Int): List<BudgetMessage> {
        if (messages.isEmpty()) return emptyList()
        val newestFirst = messages.asReversed()
        val selected = mutableListOf<BudgetMessage>()
        var used = 0

        for (message in newestFirst) {
            val cost = estimator.estimate(message)
            if (selected.isEmpty() || used + cost <= maxTokens) {
                selected += message
                used += cost
            }
        }

        return selected.asReversed()
    }
}
