package com.example.aiassistant.ui.screens.chat

import com.example.aiassistant.data.db.AttachmentEntity
import com.example.aiassistant.data.db.ConversationDao
import com.example.aiassistant.data.db.ConversationEntity
import com.example.aiassistant.data.db.MessageEntity
import com.example.aiassistant.data.db.MessageWithAttachments
import com.example.aiassistant.data.openai.ChatCompletionRequest
import com.example.aiassistant.data.openai.ModelDto
import com.example.aiassistant.data.openai.OpenAiApi
import com.example.aiassistant.data.openai.OpenAiClient
import com.example.aiassistant.data.openai.OpenAiFileAttachment
import com.example.aiassistant.data.openai.OpenAiImageAttachment
import com.example.aiassistant.data.repository.ApiProfileRepository
import com.example.aiassistant.data.repository.ChatRepository
import com.example.aiassistant.data.repository.ConversationRepository
import com.example.aiassistant.data.security.ApiKeyStorage
import com.example.aiassistant.domain.attachments.AttachmentPayload
import com.example.aiassistant.domain.attachments.AttachmentType
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun stopReceivingCancelsStreamAndKeepsPartialAssistantMessage() = runTest {
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
            conversations = ConversationRepository(FakeConversationDao()),
            chat = ChatRepository(BlockingStreamApi())
        )

        vm.updateInput("hello")
        vm.send()
        runCurrent()

        assertTrue(vm.uiState.value.isSending)
        assertEquals("partial", vm.uiState.value.messages.last().content)

        vm.stopReceiving()
        assertFalse(vm.uiState.value.isSending)
        assertTrue(vm.uiState.value.outputStopped)
        assertEquals(null, vm.uiState.value.error)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSending)
        assertEquals("partial", vm.uiState.value.messages.last().content)
        assertTrue(vm.uiState.value.outputStopped)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun deletedCurrentConversationResetsChatToBlankState() = runTest {
        val dao = FakeConversationDao(
            initialMessages = listOf(
                MessageWithAttachments(
                    message = MessageEntity(
                        id = "m-1",
                        conversationId = "conversation-1",
                        role = "user",
                        content = "hello",
                        model = "model-a",
                        createdAt = 1L
                    ),
                    attachments = emptyList()
                )
            )
        )
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
            conversations = ConversationRepository(dao),
            chat = ChatRepository(BlockingStreamApi()),
            initialConversationId = "conversation-1"
        )

        runCurrent()
        assertEquals(1, vm.uiState.value.messages.size)

        dao.emitMessages(emptyList())
        runCurrent()

        assertEquals(emptyList<ChatUiMessage>(), vm.uiState.value.messages)
        assertEquals(emptyMap<Int, List<ChatUiAttachment>>(), vm.uiState.value.messageAttachments)
        assertFalse(vm.uiState.value.isSending)
    }

    @Test
    fun newChatIgnoresOldConversationEmissions() = runTest {
        val dao = FakeConversationDao(
            initialMessages = listOf(
                MessageWithAttachments(
                    message = MessageEntity(
                        id = "m-1",
                        conversationId = "conversation-1",
                        role = "user",
                        content = "hello",
                        model = "model-a",
                        createdAt = 1L
                    ),
                    attachments = emptyList()
                )
            )
        )
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
            conversations = ConversationRepository(dao),
            chat = ChatRepository(BlockingStreamApi()),
            initialConversationId = "conversation-1"
        )

        runCurrent()
        assertEquals(1, vm.uiState.value.messages.size)

        vm.newChat()
        runCurrent()
        assertEquals(emptyList<ChatUiMessage>(), vm.uiState.value.messages)

        dao.emitMessages(
            listOf(
                MessageWithAttachments(
                    message = MessageEntity(
                        id = "m-2",
                        conversationId = "conversation-1",
                        role = "assistant",
                        content = "old conversation update",
                        model = "model-a",
                        createdAt = 2L
                    ),
                    attachments = emptyList()
                )
            )
        )
        runCurrent()

        assertEquals(emptyList<ChatUiMessage>(), vm.uiState.value.messages)
        assertEquals(emptyMap<Int, List<ChatUiAttachment>>(), vm.uiState.value.messageAttachments)
    }

    @Test
    fun attachmentOnlySendStartsRequestAndClearsComposerState() = runTest {
        val api = RecordingSingleResponseApi("done")
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), api),
            conversations = ConversationRepository(FakeConversationDao()),
            chat = ChatRepository(api)
        )
        val attachment = AttachmentPayload(
            name = "notes.md",
            type = AttachmentType.Text,
            text = "hello from attachment"
        )

        vm.addAttachment(attachment)
        vm.send()
        runCurrent()

        assertTrue(api.streamRequests.isNotEmpty())
        assertEquals("", vm.uiState.value.input)
        assertTrue(vm.uiState.value.attachments.isEmpty())
        assertTrue(vm.uiState.value.isSending)
        assertTrue(api.streamRequests.single().requestBody.messages.last().content.contains("hello from attachment"))
        assertEquals(listOf(attachment.name), vm.uiState.value.messageAttachments[0]?.map { it.name })
    }

    @Test
    fun failedAttachmentOnlySendRestoresPendingAttachments() = runTest {
        val api = EmittingThenFailingStreamApi(IOException("boom"))
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), api),
            conversations = ConversationRepository(FakeConversationDao()),
            chat = ChatRepository(api)
        )
        val attachment = AttachmentPayload(
            name = "notes.md",
            type = AttachmentType.Text,
            text = "hello from attachment"
        )

        vm.addAttachment(attachment)
        vm.send()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSending)
        assertEquals("", vm.uiState.value.input)
        assertEquals(listOf(attachment), vm.uiState.value.attachments)
        assertEquals(emptyList<ChatUiMessage>(), vm.uiState.value.messages)
        assertEquals(emptyMap<Int, List<ChatUiAttachment>>(), vm.uiState.value.messageAttachments)
        assertEquals(ChatErrorAction.Retry, vm.uiState.value.error?.action)
    }

    @Test
    fun removeAttachmentRemovesOnlyFirstMatchingPendingItem() = runTest {
        val vm = ChatViewModel(
            apiProfiles = ApiProfileRepository(FakeApiKeyStorage("key"), OpenAiClient()),
            conversations = ConversationRepository(FakeConversationDao()),
            chat = ChatRepository(BlockingStreamApi())
        )

        vm.addAttachment(
            AttachmentPayload(
                name = "same.pdf",
                type = AttachmentType.Document,
                dataUrl = "data:application/pdf;base64,1",
                mimeType = "application/pdf"
            )
        )
        vm.addAttachment(
            AttachmentPayload(
                name = "same.pdf",
                type = AttachmentType.Document,
                dataUrl = "data:application/pdf;base64,2",
                mimeType = "application/pdf"
            )
        )
        vm.addAttachment(
            AttachmentPayload(
                name = "other.jpg",
                type = AttachmentType.Image,
                dataUrl = "data:image/jpeg;base64,3"
            )
        )

        vm.removeAttachment("same.pdf")

        assertEquals(
            listOf("same.pdf", "other.jpg"),
            vm.uiState.value.attachments.map { it.name }
        )
        assertEquals(
            listOf("data:application/pdf;base64,2", "data:image/jpeg;base64,3"),
            vm.uiState.value.attachments.map { it.dataUrl }
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeApiKeyStorage(private val key: String) : ApiKeyStorage {
    override fun saveApiKey(profileId: String, apiKey: String) = Unit
    override fun getApiKey(profileId: String): String = key
    override fun deleteApiKey(profileId: String) = Unit
}

private class BlockingStreamApi : OpenAiApi {
    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = flow {
        emit("partial")
        kotlinx.coroutines.awaitCancellation()
    }

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String = "fallback"

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String = error("Responses should not be used")
}

private class RecordingSingleResponseApi(
    private val response: String
) : OpenAiApi {
    data class StreamRequest(
        val requestBody: ChatCompletionRequest,
        val images: List<OpenAiImageAttachment>
    )

    val streamRequests = mutableListOf<StreamRequest>()

    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = flow {
        streamRequests += StreamRequest(requestBody, images)
        emit(response)
        kotlinx.coroutines.awaitCancellation()
    }

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String = error("Fallback should not be used")

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String = error("Responses should not be used")
}

private class FailingStreamApi(
    private val failure: Throwable
) : OpenAiApi {
    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = flow {
        throw failure
    }

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String = throw failure

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String = throw failure
}

private class EmittingThenFailingStreamApi(
    private val failure: Throwable
) : OpenAiApi {
    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = emptyList()

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = flow {
        emit("partial")
        throw failure
    }

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String = error("Fallback should not be used")

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String = error("Responses should not be used")
}

private class FakeConversationDao(
    initialMessages: List<MessageWithAttachments> = emptyList()
) : ConversationDao {
    private val messagesWithAttachments = MutableStateFlow(initialMessages)

    fun emitMessages(messages: List<MessageWithAttachments>) {
        messagesWithAttachments.value = messages
    }

    override fun observeConversations(): Flow<List<ConversationEntity>> = emptyFlow()
    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = emptyFlow()
    override fun observeMessagesWithAttachments(conversationId: String): Flow<List<MessageWithAttachments>> = messagesWithAttachments
    override suspend fun upsertConversation(conversation: ConversationEntity) = Unit
    override suspend fun insertMessage(message: MessageEntity) = Unit
    override suspend fun touchConversation(conversationId: String, updatedAt: Long) = Unit
    override suspend fun insertAttachment(attachment: AttachmentEntity) = Unit
    override suspend fun deleteMessagesForConversation(conversationId: String) = Unit
    override suspend fun deleteAttachmentsForConversation(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun clearConversations() = Unit
    override suspend fun clearMessages() = Unit
    override suspend fun clearAttachments() = Unit
}
