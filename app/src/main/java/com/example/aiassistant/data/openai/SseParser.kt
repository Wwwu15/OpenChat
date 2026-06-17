package com.example.aiassistant.data.openai

object SseParser {
    fun parseLines(lines: List<String>): List<String> {
        return lines
            .map { it.trim() }
            .filter { it.startsWith("data:") }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotEmpty() && it != "[DONE]" }
    }
}
