package com.example.aiassistant.data.repository

import com.example.aiassistant.data.db.MessageEntity
import com.example.aiassistant.data.openai.ChatCompletionRequest
import com.example.aiassistant.data.openai.ChatMessageDto
import com.example.aiassistant.data.openai.OpenAiApi
import com.example.aiassistant.data.openai.OpenAiFileAttachment
import com.example.aiassistant.data.openai.OpenAiImageAttachment
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentType
import java.util.concurrent.CancellationException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val client: OpenAiApi
) {
    fun send(
        profile: ApiProfile,
        apiKey: String,
        priorMessages: List<MessageEntity>,
        userText: String,
        attachments: List<AttachmentPayload> = emptyList()
    ): Flow<String> = flow {
        val documents = attachments
            .filter { it.type == AttachmentType.Document && !it.dataUrl.isNullOrBlank() }
            .mapIndexed { index, attachment ->
                OpenAiFileAttachment(
                    filename = attachment.safeUploadFilename(index),
                    dataUrl = attachment.dataUrl.orEmpty()
                )
            }
        if (documents.isNotEmpty()) {
            val response = withContext(Dispatchers.IO) {
                client.completeResponseWithFiles(profile.baseUrl, apiKey, profile.modelId, userText, documents)
            }
            emit(response)
            return@flow
        }

        val allMessages = priorMessages.map { ChatMessageDto(it.role, it.content) } + ChatMessageDto("user", userText)
        val request = ChatCompletionRequest(
            model = profile.modelId,
            messages = allMessages,
            stream = true
        )
        val images = attachments
            .filter { it.type == AttachmentType.Image && !it.dataUrl.isNullOrBlank() }
            .map { OpenAiImageAttachment(it.dataUrl.orEmpty()) }

        var emitted = false
        var streamFailure: Throwable? = null
        try {
            client.streamChat(profile.baseUrl, apiKey, request, images).collect {
                emitted = true
                emit(it)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            if (emitted) throw failure
            streamFailure = failure
        }

        if (!emitted) {
            try {
                val response = withContext(Dispatchers.IO) {
                    client.completeChat(profile.baseUrl, apiKey, request.copy(stream = false), images)
                }
                emit(response)
            } catch (failure: Throwable) {
                val original = streamFailure
                if (original != null) {
                    throw IOException(
                        "Streaming failed: " + original.readableMessage() + "; fallback failed: " + failure.readableMessage(),
                        failure
                    )
                }
                throw failure
            }
        }
    }
}

@Suppress("UNUSED")
private fun Throwable.readableMessage(): String {
    val type = this::class.simpleName ?: "Error"
    val detail = message?.takeIf { it.isNotBlank() }
    return if (detail == null) type else type + ": " + detail
}

private fun AttachmentPayload.safeUploadFilename(index: Int): String {
    val extension = name.substringAfterLast(".", missingDelimiterValue = "")
        .lowercase()
        .takeIf { it in setOf("pdf", "doc", "docx") }
        ?: mimeType?.documentExtension()
        ?: "pdf"
    return "attachment-" + (index + 1).toString() + "." + extension
}

private fun String.documentExtension(): String? = when (lowercase()) {
    "application/pdf" -> "pdf"
    "application/msword" -> "doc"
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
    else -> null
}
