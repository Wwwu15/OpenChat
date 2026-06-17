package com.example.aiassistant.data.repository

import com.example.aiassistant.data.openai.ChatCompletionRequest
import com.example.aiassistant.data.openai.ModelDto
import com.example.aiassistant.data.openai.OpenAiApi
import com.example.aiassistant.data.openai.OpenAiFileAttachment
import com.example.aiassistant.data.openai.OpenAiImageAttachment
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlinx.coroutines.CancellationException

class ChatRepositoryTest {
    @Test
    fun requestContainsPriorMessagesPlusCurrentUserMessageOnce() = runTest {
        val api = CapturingApi()
        val repository = ChatRepository(api)

        repository.send(
            profile = ApiProfile(modelId = "test-model"),
            apiKey = "key",
            priorMessages = listOf(
                com.example.aiassistant.data.db.MessageEntity("1", "c1", "user", "old", "m", 1),
                com.example.aiassistant.data.db.MessageEntity("2", "c1", "assistant", "answer", "m", 2)
            ),
            userText = "current"
        ).toList()

        val sent = api.lastRequest?.messages.orEmpty()
        assertEquals(listOf("old", "answer", "current"), sent.map { it.content })
        assertEquals(0, api.responsesCalls)
    }

    @Test
    fun documentAttachmentsUseResponsesApi() = runTest {
        val api = CapturingApi(responseText = "document answer")
        val repository = ChatRepository(api)

        val result = repository.send(
            profile = ApiProfile(baseUrl = "https://example.test", modelId = "test-model"),
            apiKey = "key",
            priorMessages = listOf(
                com.example.aiassistant.data.db.MessageEntity("1", "c1", "user", "old", "m", 1)
            ),
            userText = "summarize",
            attachments = listOf(
                AttachmentPayload(
                    name = "paper.pdf",
                    type = AttachmentType.Document,
                    dataUrl = "data:application/pdf;base64,abc",
                    mimeType = "application/pdf"
                )
            )
        ).toList()

        assertEquals(listOf("document answer"), result)
        assertEquals(1, api.responsesCalls)
        assertEquals(0, api.streamCalls)
        assertEquals("summarize", api.lastResponseInputText)
        assertEquals(listOf("attachment-1.pdf"), api.lastResponseFiles.map { it.filename })
    }

    @Test
    fun fallsBackToNonStreamingCompletionWhenStreamFailsBeforeEmitting() = runTest {
        val api = FailingStreamApi(fallbackText = "fallback answer")
        val repository = ChatRepository(api)

        val result = repository.send(
            profile = ApiProfile(modelId = "test-model"),
            apiKey = "key",
            priorMessages = emptyList(),
            userText = "hello"
        ).toList()

        assertEquals(listOf("fallback answer"), result)
        assertEquals(1, api.completeCalls)
    }

    @Test
    fun doesNotFallbackWhenStreamFailsAfterPartialOutput() = runTest {
        val api = PartialThenFailingStreamApi()
        val repository = ChatRepository(api)

        val result = runCatching {
            repository.send(
                profile = ApiProfile(modelId = "test-model"),
                apiKey = "key",
                priorMessages = emptyList(),
                userText = "hello"
            ).toList()
        }

        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(0, api.completeCalls)
    }

    @Test
    fun doesNotFallbackWhenStreamIsCancelledBeforeOutput() = runTest {
        val api = CancelledStreamApi()
        val repository = ChatRepository(api)

        val result = runCatching {
            repository.send(
                profile = ApiProfile(modelId = "test-model"),
                apiKey = "key",
                priorMessages = emptyList(),
                userText = "hello"
            ).first()
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(0, api.completeCalls)
    }
}

private class CapturingApi(
    private val responseText: String = "responses"
) : OpenAiApi {
    var lastRequest: ChatCompletionRequest? = null
    var streamCalls = 0
    var responsesCalls = 0
    var lastResponseInputText: String? = null
    var lastResponseFiles: List<OpenAiFileAttachment> = emptyList()

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()
    override fun streamChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): Flow<String> = flow {
        streamCalls += 1
        lastRequest = requestBody
        emit("ok")
    }

    override fun completeChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): String = "fallback"

    override fun completeResponseWithFiles(baseUrl: String, apiKey: String, model: String, inputText: String, files: List<OpenAiFileAttachment>): String {
        responsesCalls += 1
        lastResponseInputText = inputText
        lastResponseFiles = files
        return responseText
    }
}

private class FailingStreamApi(
    private val fallbackText: String
) : OpenAiApi {
    var completeCalls = 0

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()
    override fun streamChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): Flow<String> = flow {
        throw IOException("stream unavailable")
    }

    override fun completeChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): String {
        completeCalls += 1
        return fallbackText
    }

    override fun completeResponseWithFiles(baseUrl: String, apiKey: String, model: String, inputText: String, files: List<OpenAiFileAttachment>): String = error("Responses should not be used")
}

private class PartialThenFailingStreamApi : OpenAiApi {
    var completeCalls = 0

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()
    override fun streamChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): Flow<String> = flow {
        emit("partial")
        throw IOException("stream interrupted")
    }

    override fun completeChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): String {
        completeCalls += 1
        return "fallback"
    }

    override fun completeResponseWithFiles(baseUrl: String, apiKey: String, model: String, inputText: String, files: List<OpenAiFileAttachment>): String = error("Responses should not be used")
}

private class CancelledStreamApi : OpenAiApi {
    var completeCalls = 0

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()
    override fun streamChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): Flow<String> = flow {
        throw CancellationException("stopped")
    }

    override fun completeChat(baseUrl: String, apiKey: String, requestBody: ChatCompletionRequest, images: List<OpenAiImageAttachment>): String {
        completeCalls += 1
        return "fallback"
    }

    override fun completeResponseWithFiles(baseUrl: String, apiKey: String, model: String, inputText: String, files: List<OpenAiFileAttachment>): String = error("Responses should not be used")
}