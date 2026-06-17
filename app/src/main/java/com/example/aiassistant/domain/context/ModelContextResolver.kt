package com.example.aiassistant.domain.context

class ModelContextResolver {
    private val fallbackLimits = mapOf(
        "deepseek-v4" to 1_000_000,
        "gpt-5.4" to 1_000_000,
        "gpt-5.5" to 1_000_000,
        "gpt-5.4-mini" to 400_000
    )

    fun resolve(modelId: String, apiReported: Int?, manual: Int?): Int {
        if (apiReported != null && apiReported > 0) return apiReported
        if (manual != null && manual > 0) return manual
        return fallbackForModel(modelId)
    }

    fun displayLimit(apiReported: Int?, manual: Int?): Int? {
        if (apiReported != null && apiReported > 0) return apiReported
        if (manual != null && manual > 0) return manual
        return null
    }

    private fun fallbackForModel(modelId: String): Int {
        val normalized = modelId.lowercase()
        for ((key, limit) in fallbackLimits) {
            if (normalized.contains(key)) return limit
        }
        return 256_000
    }

    companion object {
        const val DefaultContextLimit = 8192
    }
}
