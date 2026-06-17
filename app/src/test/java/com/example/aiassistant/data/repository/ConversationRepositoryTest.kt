package com.example.aiassistant.data.repository

import com.example.aiassistant.data.db.AttachmentEntity
import com.example.aiassistant.data.db.ConversationDao
import com.example.aiassistant.data.db.ConversationEntity
import com.example.aiassistant.data.db.MessageEntity
import com.example.aiassistant.data.db.MessageWithAttachments
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationRepositoryTest {
    @Test
    fun addMessagePersistsAttachmentMetadata() = runTest {
        val dao = RecordingConversationDao()
        val repository = ConversationRepository(dao)

        val message = repository.addMessage(
            conversationId = "conversation-1",
            role = "user",
            content = "read these",
            model = "model-1",
            attachments = listOf(
                AttachmentPayload("note.txt", AttachmentType.Text, text = "hello"),
                AttachmentPayload("photo.jpg", AttachmentType.Image, dataUrl = "data:image/jpeg;base64,abc")
            )
        )

        assertEquals(2, dao.attachments.size)
        assertEquals(listOf(message.id, message.id), dao.attachments.map { it.messageId })
        assertEquals("note.txt", dao.attachments[0].name)
        assertEquals("text", dao.attachments[0].kind)
        assertEquals("text/plain", dao.attachments[0].mimeType)
        assertEquals("hello", dao.attachments[0].textContent)
        assertEquals(null, dao.attachments[0].dataUrl)
        assertEquals("photo.jpg", dao.attachments[1].name)
        assertEquals("image", dao.attachments[1].kind)
        assertEquals("image/jpeg", dao.attachments[1].mimeType)
        assertEquals(null, dao.attachments[1].textContent)
        assertEquals("data:image/jpeg;base64,abc", dao.attachments[1].dataUrl)
    }

    @Test
    fun deleteConversationRemovesAttachmentsMessagesAndConversationInOrder() = runTest {
        val dao = RecordingConversationDao()
        val repository = ConversationRepository(dao)

        repository.deleteConversation("conversation-9")

        assertEquals(
            listOf(
                "attachments:conversation-9",
                "messages:conversation-9",
                "conversation:conversation-9"
            ),
            dao.operations
        )
    }
}

private class RecordingConversationDao : ConversationDao {
    val attachments = mutableListOf<AttachmentEntity>()
    val operations = mutableListOf<String>()
    private val conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    private val messages = MutableStateFlow<List<MessageEntity>>(emptyList())

    override fun observeConversations(): Flow<List<ConversationEntity>> = conversations

    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = messages

    override fun observeMessagesWithAttachments(conversationId: String): Flow<List<MessageWithAttachments>> =
        MutableStateFlow(emptyList())

    override suspend fun upsertConversation(conversation: ConversationEntity) {
        conversations.value = conversations.value.filterNot { it.id == conversation.id } + conversation
    }

    override suspend fun insertMessage(message: MessageEntity) {
        messages.value = messages.value + message
    }

    override suspend fun touchConversation(conversationId: String, updatedAt: Long) = Unit

    override suspend fun insertAttachment(attachment: AttachmentEntity) {
        attachments += attachment
    }

    override suspend fun deleteMessagesForConversation(conversationId: String) {
        operations += "messages:$conversationId"
    }

    override suspend fun deleteAttachmentsForConversation(conversationId: String) {
        operations += "attachments:$conversationId"
    }

    override suspend fun deleteConversation(conversationId: String) {
        operations += "conversation:$conversationId"
    }

    override suspend fun clearConversations() {
        conversations.value = emptyList()
    }

    override suspend fun clearMessages() {
        messages.value = emptyList()
    }

    override suspend fun clearAttachments() {
        attachments.clear()
    }
}
