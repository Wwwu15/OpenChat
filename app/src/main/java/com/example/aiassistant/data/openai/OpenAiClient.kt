package com.example.aiassistant.data.openai

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class OpenAiImageAttachment(val dataUrl: String)
data class OpenAiFileAttachment(
    val filename: String,
    val dataUrl: String
)

interface OpenAiApi {
    fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto>

    fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment> = emptyList()
    ): Flow<String>

    fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment> = emptyList()
    ): String

    fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String
}

class OpenAiClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    moshi: Moshi = Moshi.Builder().build()
) : OpenAiApi {
    private val modelsAdapter = moshi.adapter(ModelListResponse::class.java)
    private val chatResponseAdapter = moshi.adapter(ChatCompletionResponse::class.java)
    private val responsesAdapter = moshi.adapter(ResponsesResponse::class.java)

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> {
        val request = Request.Builder()
            .url(OpenAiUrls.models(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .providerHeaders(baseUrl)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(response.readableError("Model request", body))
            return modelsAdapter.fromJson(body)?.data.orEmpty().map { it.toModelDto() }
        }
    }

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = flow {
        val requestJson = chatRequestJson(requestBody.copy(stream = true), images)
        val request = Request.Builder()
            .url(OpenAiUrls.chatCompletions(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .providerHeaders(baseUrl)
            .post(requestJson.toRequestBody(JsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                throw IOException(response.readableError("Chat request", body))
            }
            val source = response.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                for (event in SseParser.parseLines(listOf(line))) {
                    val chunk = chatResponseAdapter.fromJson(event)
                    val delta = chunk?.choices?.firstOrNull()?.delta?.content
                    if (!delta.isNullOrEmpty()) emit(delta)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String {
        val requestJson = chatRequestJson(requestBody.copy(stream = false), images)
        val request = Request.Builder()
            .url(OpenAiUrls.chatCompletions(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .providerHeaders(baseUrl)
            .post(requestJson.toRequestBody(JsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(response.readableError("Chat request", body))
            return chatResponseAdapter.fromJson(body)
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                .orEmpty()
        }
    }

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String {
        val requestJson = responsesFileRequestJson(model, inputText, files)
        val request = Request.Builder()
            .url(OpenAiUrls.responses(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .providerHeaders(baseUrl)
            .post(requestJson.toRequestBody(JsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(response.readableError("File upload request", body))
            }
            val parsed = responsesAdapter.fromJson(body)
            return parsed?.outputText?.takeIf { it.isNotBlank() }
                ?: parsed?.output
                    ?.flatMap { it.content }
                    ?.mapNotNull { it.text }
                    ?.joinToString("\n")
                    .orEmpty()
        }
    }

    private fun chatRequestJson(request: ChatCompletionRequest, images: List<OpenAiImageAttachment>): String {
        val messagesJson = request.messages.mapIndexed { index, message ->
            val isLastUser = index == request.messages.lastIndex && message.role == "user" && images.isNotEmpty()
            if (isLastUser) {
                val parts = buildString {
                    append("""{"type":"text","text":${jsonString(message.content)}}""")
                    images.forEach { image ->
                        append(""",{"type":"image_url","image_url":{"url":${jsonString(image.dataUrl)}}}""")
                    }
                }
                """{"role":${jsonString(message.role)},"content":[$parts]}"""
            } else {
                """{"role":${jsonString(message.role)},"content":${jsonString(message.content)}}"""
            }
        }.joinToString(",")
        return """{"model":${jsonString(request.model)},"messages":[$messagesJson],"stream":${request.stream}}"""
    }

    private fun responsesFileRequestJson(model: String, inputText: String, files: List<OpenAiFileAttachment>): String {
        val contentParts = buildString {
            files.forEachIndexed { index, file ->
                if (index > 0) append(',')
                append("""{"type":"input_file","filename":${jsonString(file.filename)},"file_data":${jsonString(file.dataUrl)}}""")
            }
            if (isNotEmpty()) append(',')
            append("""{"type":"input_text","text":${jsonString(inputText)}}""")
        }
        return """{"model":${jsonString(model)},"input":[{"role":"user","content":[$contentParts]}],"store":false}"""
    }

    private fun jsonString(value: String): String {
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    private companion object {
        val JsonMediaType = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .callTimeout(240, TimeUnit.SECONDS)
                .build()
        }
    }
}

private fun Request.Builder.providerHeaders(baseUrl: String): Request.Builder {
    if (baseUrl.contains("openrouter.ai", ignoreCase = true)) {
        header("HTTP-Referer", "https://local.aiassistant")
        header("X-OpenRouter-Title", "AI Assistant")
    }
    return this
}

private fun okhttp3.Response.readableError(operation: String, body: String): String {
    val detail = body
        .lineSequence()
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .take(400)
        .takeIf { it.isNotBlank() }
    return buildString {
        append(operation)
        append(" failed: HTTP ")
        append(code)
        if (detail != null) {
            append(", ")
            append(detail)
        }
    }
}
