package com.example.aiassistant.data.repository

import com.example.aiassistant.data.db.AttachmentEntity
import com.example.aiassistant.data.db.ConversationDao
import com.example.aiassistant.data.db.ConversationEntity
import com.example.aiassistant.data.db.MessageEntity
import com.example.aiassistant.data.db.MessageWithAttachments
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentType
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ConversationRepository(private val dao: ConversationDao) {
    fun observeConversations(): Flow<List<ConversationEntity>> = dao.observeConversations()

    fun observeMessages(conversationId: String): Flow<List<MessageWithAttachments>> = dao.observeMessagesWithAttachments(conversationId)

    suspend fun ensureConversation(id: String, title: String, model: String, profileId: String) {
        val now = System.currentTimeMillis()
        dao.upsertConversation(
            ConversationEntity(
                id = id,
                title = title.ifBlank { "新对话" },
                model = model,
                apiProfileId = profileId,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        model: String?,
        attachments: List<AttachmentPayload> = emptyList()
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            model = model,
            createdAt = now
        )
        dao.insertMessage(entity)
        attachments.forEach { attachment ->
            dao.insertAttachment(
                AttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    messageId = entity.id,
                    name = attachment.name,
                    mimeType = attachment.mimeType(),
                    kind = attachment.kindName(),
                    textContent = attachment.text,
                    dataUrl = attachment.dataUrl.takeUnless { attachment.type == AttachmentType.Document },
                    createdAt = now
                )
            )
        }
        dao.touchConversation(conversationId, now)
        return entity
    }

    suspend fun clearHistory() {
        dao.clearAttachments()
        dao.clearMessages()
        dao.clearConversations()
    }

    suspend fun deleteConversation(conversationId: String) {
        dao.deleteAttachmentsForConversation(conversationId)
        dao.deleteMessagesForConversation(conversationId)
        dao.deleteConversation(conversationId)
    }
}

private fun AttachmentPayload.kindName(): String = when (type) {
    AttachmentType.Text -> "text"
    AttachmentType.Image -> "image"
    AttachmentType.Document -> "document"
    AttachmentType.Unsupported -> "unsupported"
}

private fun AttachmentPayload.mimeType(): String = when (type) {
    AttachmentType.Text -> "text/plain"
    AttachmentType.Image -> dataUrl?.substringAfter("data:", "")?.substringBefore(";")?.takeIf { it.isNotBlank() }
        ?: "image/*"
    AttachmentType.Document -> mimeType ?: dataUrl?.substringAfter("data:", "")?.substringBefore(";")?.takeIf { it.isNotBlank() }
        ?: "application/octet-stream"
    AttachmentType.Unsupported -> "application/octet-stream"
}
