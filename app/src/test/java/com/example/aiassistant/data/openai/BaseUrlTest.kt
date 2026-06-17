package com.example.aiassistant.data.openai

import org.junit.Assert.assertEquals
import org.junit.Test

class BaseUrlTest {
    @Test
    fun modelsUrlHandlesRootUrl() {
        assertEquals("https://example.com/v1/models", OpenAiUrls.models("https://example.com"))
    }

    @Test
    fun modelsUrlHandlesVersionedUrl() {
        assertEquals("https://example.com/v1/models", OpenAiUrls.models("https://example.com/v1"))
    }

    @Test
    fun chatUrlHandlesTrailingSlash() {
        assertEquals("https://example.com/v1/chat/completions", OpenAiUrls.chatCompletions("https://example.com/v1/"))
    }
}
