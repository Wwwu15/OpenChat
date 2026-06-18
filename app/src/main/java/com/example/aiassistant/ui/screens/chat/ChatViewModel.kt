package com.example.aiassistant.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.db.MessageEntity
import com.example.aiassistant.data.db.MessageWithAttachments
import com.example.aiassistant.data.repository.ApiProfileRepository
import com.example.aiassistant.data.repository.ChatRepository
import com.example.aiassistant.data.repository.ConversationRepository
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentPrompt
import com.example.aiassistant.domain.attachments.AttachmentType
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiMessage(val role: String, val content: String)

data class ChatUiAttachment(
    val messageId: String,
    val name: String,
    val mimeType: String,
    val kind: String,
    val textContent: String?,
    val dataUrl: String?
)

data class ChatUiState(
    val input: String = "",
    val messages: List<ChatUiMessage> = emptyList(),
    val messageAttachments: Map<Int, List<ChatUiAttachment>> = emptyMap(),
    val attachments: List<AttachmentPayload> = emptyList(),
    val isSending: Boolean = false,
    val error: ChatUiError? = null
)

class ChatViewModel(
    private val apiProfiles: ApiProfileRepository,
    private val conversations: ConversationRepository,
    private val chat: ChatRepository,
    initialConversationId: String? = null
) : ViewModel() {
    private var conversationId = initialConversationId ?: UUID.randomUUID().toString()
    private var lastAttempt: PendingSend? = null
    private var lastStableMessages: List<ChatUiMessage> = emptyList()
    private var lastStableAttachments: Map<Int, List<ChatUiAttachment>> = emptyMap()
    private var observedConversationId: String? = null
    private var messagesJob: Job? = null
    private var sendJob: Job? = null
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        observeConversation(initialConversationId)
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
    }

    fun newChat() {
        sendJob?.cancel()
        conversationId = UUID.randomUUID().toString()
        observeConversation(null)
        _uiState.value = ChatUiState(messages = emptyList(), messageAttachments = emptyMap())
    }

    fun addAttachment(payload: AttachmentPayload) {
        val profile = apiProfiles.currentProfile()
        val warning = if (payload.type == AttachmentType.Image && !profile.modelId.maySupportImages()) {
            ChatUiError.imageMayBeUnsupported(profile.modelId)
        } else {
            null
        }
        _uiState.update { it.copy(attachments = it.attachments + payload, error = warning) }
    }

    fun removeAttachment(name: String) {
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments.filterNot { it.name == name },
                error = null
            )
        }
    }

    fun showAttachmentError(message: String) {
        _uiState.update { it.copy(error = ChatUiError.unsupportedAttachment(message)) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retryLastSend() {
        val attempt = lastAttempt ?: return
        _uiState.update { it.copy(input = attempt.input, attachments = attempt.attachments, error = null) }
        send()
    }

    fun send() {
        val current = _uiState.value
        if (current.isSending) return
        val originalInput = current.input
        val text = originalInput.trim()
        val attachments = current.attachments
        if (text.isBlank() && attachments.isEmpty()) return

        val profile = apiProfiles.currentProfile()
        val key = apiProfiles.apiKey()
        if (key.isBlank()) {
            lastAttempt = PendingSend(originalInput, attachments)
            _uiState.update { it.copy(error = ChatUiError.missingApiKey()) }
            return
        }

        lastAttempt = PendingSend(originalInput, attachments)
        lastStableMessages = current.messages
        lastStableAttachments = current.messageAttachments
        val messageText = AttachmentPrompt.build(text, attachments)
        val prior = current.messages.mapIndexed { index, message ->
            MessageEntity(index.toString(), conversationId, message.role, message.content, profile.modelId, index.toLong())
        }

        sendJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    input = "",
                    attachments = emptyList(),
                    isSending = true,
                    error = null,
                    messages = it.messages + ChatUiMessage("user", messageText) + ChatUiMessage("assistant", ""),
                    messageAttachments = if (attachments.isEmpty()) {
                        it.messageAttachments
                    } else {
                        it.messageAttachments + (it.messages.size to attachments.map { attachment ->
                            ChatUiAttachment(
                                messageId = conversationId,
                                name = attachment.name,
                                mimeType = attachment.mimeTypeValue(),
                                kind = attachment.type.name.lowercase(),
                                textContent = attachment.text,
                                dataUrl = attachment.dataUrl.takeUnless { attachment.type == AttachmentType.Document }
                            )
                        })
                    }
                )
            }

            val assistant = StringBuilder()
            try {
                conversations.ensureConversation(conversationId, text.take(20), profile.modelId, profile.id)
                conversations.addMessage(conversationId, "user", messageText, profile.modelId, attachments)
                chat.send(profile, key, prior, messageText, attachments).collect { delta ->
                    assistant.append(delta)
                    _uiState.update { state ->
                        state.copy(messages = state.messages.dropLast(1) + ChatUiMessage("assistant", assistant.toString()))
                    }
                }
                conversations.addMessage(conversationId, "assistant", assistant.toString(), profile.modelId)
            } catch (cancelled: CancellationException) {
                if (assistant.isNotBlank()) {
                    withContext(NonCancellable) {
                        conversations.addMessage(conversationId, "assistant", assistant.toString(), profile.modelId)
                    }
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        messages = lastStableMessages,
                        messageAttachments = lastStableAttachments,
                        input = originalInput,
                        attachments = attachments,
                        error = ChatUiError.requestFailed(
                            if (attachments.any { attachment -> attachment.type == AttachmentType.Document }) {
                                "文件直传失败：${error.readableMessage()}"
                            } else {
                                error.message ?: "请求失败"
                            }
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isSending = false) }
                sendJob = null
            }
        }
    }

    fun stopReceiving() {
        sendJob?.cancel()
    }

    private fun observeConversation(id: String?) {
        observedConversationId = id
        messagesJob?.cancel()
        messagesJob = id?.let { conversationToObserve ->
            viewModelScope.launch {
                conversations.observeMessages(conversationToObserve).collect { messages ->
                    if (observedConversationId != conversationToObserve) return@collect
                    _uiState.update { state ->
                        if (state.isSending) state else state.withMessages(messages)
                    }
                }
            }
        }
    }
}

private data class PendingSend(
    val input: String,
    val attachments: List<AttachmentPayload>
)

private fun Throwable.readableMessage(): String {
    return listOfNotNull(
        this::class.simpleName,
        message?.takeIf { it.isNotBlank() }
    ).joinToString("：").ifBlank {
        "请确认当前接口支持 Responses 文件输入后重试"
    }
}

private fun AttachmentPayload.mimeTypeValue(): String = when (type) {
    AttachmentType.Text -> "text/plain"
    AttachmentType.Image -> dataUrl?.substringAfter("data:", "")?.substringBefore(";").takeIf { !it.isNullOrBlank() } ?: "image/*"
    AttachmentType.Document -> mimeType ?: dataUrl?.substringAfter("data:", "")?.substringBefore(";").takeIf { !it.isNullOrBlank() } ?: "application/octet-stream"
    AttachmentType.Unsupported -> "application/octet-stream"
}

private fun ChatUiState.withMessages(messages: List<MessageWithAttachments>): ChatUiState {
    return copy(
        messages = messages.map { ChatUiMessage(it.message.role, it.message.content) },
        messageAttachments = messages.mapIndexedNotNull { index, message ->
            val items = message.attachments.map {
                ChatUiAttachment(
                    messageId = message.message.id,
                    name = it.name,
                    mimeType = it.mimeType,
                    kind = it.kind,
                    textContent = it.textContent,
                    dataUrl = it.dataUrl
                )
            }
            if (items.isEmpty()) null else index to items
        }.toMap()
    )
}

private fun String.maySupportImages(): Boolean {
    val normalized = lowercase()
    return listOf("vision", "image", "visual", "vl", "4o", "gpt-5", "gemini").any { normalized.contains(it) }
}
