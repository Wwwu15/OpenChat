package com.example.aiassistant.domain.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBudgeterTest {
    @Test
    fun resolverFallsBackTo256kForUnknownModels() {
        val resolver = ModelContextResolver()

        assertEquals(256000, resolver.resolve("unknown-model", apiReported = null, manual = null))
    }

    @Test
    fun resolverDoesNotDisplayGuessedModelLimits() {
        val resolver = ModelContextResolver()

        assertEquals(null, resolver.displayLimit(apiReported = null, manual = null))
    }

    @Test
    fun resolverPrefersApiReportedContext() {
        val resolver = ModelContextResolver()

        assertEquals(128000, resolver.resolve("unknown", apiReported = 128000, manual = 32000))
    }

    @Test
    fun resolverUsesManualWhenModelIsUnknown() {
        val resolver = ModelContextResolver()

        assertEquals(12000, resolver.resolve("private-model", apiReported = null, manual = 12000))
        assertEquals(12000, resolver.displayLimit(apiReported = null, manual = 12000))
    }

    @Test
    fun budgeterPreservesNewestMessages() {
        val messages = listOf(
            BudgetMessage("user", "old ".repeat(5000)),
            BudgetMessage("assistant", "middle"),
            BudgetMessage("user", "new")
        )

        val result = ContextBudgeter(TokenEstimator()).fit(messages, maxTokens = 50)

        assertEquals("new", result.last().content)
        assertTrue(result.none { it.content.startsWith("old") })
    }
}


