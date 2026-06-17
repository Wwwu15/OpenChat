package com.example.aiassistant.data.openai

object OpenAiUrls {
    fun models(baseUrl: String): String = endpoint(baseUrl, "models")

    fun chatCompletions(baseUrl: String): String = endpoint(baseUrl, "chat/completions")

    fun responses(baseUrl: String): String = endpoint(baseUrl, "responses")

    private fun endpoint(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        val versioned = if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
        return "$versioned/$path"
    }
}
