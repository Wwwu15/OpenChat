package com.example.aiassistant.data.openai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModelListResponse(
    val data: List<ModelResponseDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelDto(
    val id: String,
    @Json(name = "context_length") val contextLength: Int? = null
)

@JsonClass(generateAdapter = true)
data class ModelResponseDto(
    val id: String,
    @Json(name = "context_length") val contextLength: Int? = null,
    @Json(name = "max_context_length") val maxContextLength: Int? = null,
    @Json(name = "top_provider") val topProvider: ModelTopProviderDto? = null
) {
    fun toModelDto(): ModelDto {
        return ModelDto(
            id = id,
            contextLength = contextLength ?: maxContextLength ?: topProvider?.contextLength
        )
    }
}

@JsonClass(generateAdapter = true)
data class ModelTopProviderDto(
    @Json(name = "context_length") val contextLength: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val choices: List<ChatChoiceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatChoiceDto(
    val message: ChatMessageDto? = null,
    val delta: ChatDeltaDto? = null
)

@JsonClass(generateAdapter = true)
data class ChatDeltaDto(
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponsesResponse(
    @Json(name = "output_text") val outputText: String? = null,
    val output: List<ResponsesOutputDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ResponsesOutputDto(
    val type: String? = null,
    val role: String? = null,
    val content: List<ResponsesContentDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ResponsesContentDto(
    val type: String? = null,
    val text: String? = null
)
