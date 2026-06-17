package com.example.aiassistant.domain.context

import kotlin.math.ceil

class TokenEstimator {
    fun estimate(message: BudgetMessage): Int {
        return estimateText(message.content) + PerMessageOverhead
    }

    fun estimateText(text: String): Int {
        if (text.isBlank()) return 0
        return ceil(text.length / CharsPerToken).toInt()
    }

    companion object {
        private const val CharsPerToken = 3.5
        private const val PerMessageOverhead = 8
    }
}
