package com.example.aiassistant.data.openai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.IOException
import java.net.InetAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OpenAiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpenAiClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchModelsSendsBearerTokenAndParsesContextLength() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"m1","context_length":1234}]}"""))

        val models = client.fetchModels(server.url("/v1").toString(), "test-key")
        val request = server.takeRequest()

        assertEquals("/v1/models", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        assertEquals("m1", models.single().id)
        assertEquals(1234, models.single().contextLength)
    }

    @Test
    fun fetchModelsParsesOpenRouterTopProviderContextLength() {
        server.enqueue(
            MockResponse().setBody(
                """{"data":[{"id":"m1","top_provider":{"context_length":131072}}]}"""
            )
        )

        val models = client.fetchModels(server.url("/v1").toString(), "test-key")

        assertEquals(131072, models.single().contextLength)
    }

    @Test
    fun streamChatEmitsDeltasAndSendsStreamingRequest() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                data: {"choices":[{"delta":{"content":"he"}}]}

                data: {"choices":[{"delta":{"content":"llo"}}]}

                data: [DONE]
                """.trimIndent()
            )
        )

        val result = client.streamChat(
            baseUrl = server.url("/v1").toString(),
            apiKey = "key",
            requestBody = ChatCompletionRequest(
                model = "m1",
                messages = listOf(ChatMessageDto("user", "hi")),
                stream = true
            )
        ).toList()
        val body = server.takeRequest().body.readUtf8()

        assertEquals(listOf("he", "llo"), result)
        assertTrue(body.contains(""""stream":true"""))
    }

    @Test
    fun completeChatSendsImagesAsVisionContentParts() {
        server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}"""))

        val result = client.completeChat(
            baseUrl = server.url("/v1").toString(),
            apiKey = "key",
            requestBody = ChatCompletionRequest(
                model = "vision-model",
                messages = listOf(ChatMessageDto("user", "describe")),
                stream = false
            ),
            images = listOf(OpenAiImageAttachment("data:image/png;base64,abc"))
        )
        val body = server.takeRequest().body.readUtf8()

        assertEquals("ok", result)
        assertTrue(body.contains(""""content":[{"""))
        assertTrue(body.contains(""""type":"image_url""""))
        assertTrue(body.contains("data:image/png;base64,abc"))
    }

    @Test
    fun completeResponseWithFilesSendsInputFileParts() {
        server.enqueue(MockResponse().setBody("""{"output_text":"doc answer","output":[]}"""))

        val result = client.completeResponseWithFiles(
            baseUrl = server.url("/v1").toString(),
            apiKey = "key",
            model = "m1",
            inputText = "summarize",
            files = listOf(OpenAiFileAttachment("paper.pdf", "data:application/pdf;base64,abc"))
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("/v1/responses", request.path)
        assertEquals("doc answer", result)
        assertTrue(body.contains(""""store":false"""))
        assertTrue(body.contains(""""type":"input_file""""))
        assertTrue(body.contains(""""filename":"paper.pdf""""))
        assertTrue(body.contains("data:application/pdf;base64,abc"))
        assertTrue(body.contains(""""type":"input_text""""))
    }

    @Test
    fun openRouterRequestsIncludeCompatibilityHeaders() {
        client = OpenAiClient(
            OkHttpClient.Builder()
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return if (hostname == "openrouter.ai") {
                            listOf(InetAddress.getByName("127.0.0.1"))
                        } else {
                            Dns.SYSTEM.lookup(hostname)
                        }
                    }
                })
                .build()
        )
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"m1"}]}"""))

        client.fetchModels("http://openrouter.ai:${server.port}/api/v1", "key")
        val request = server.takeRequest()

        assertEquals("/api/v1/models", request.path)
        assertEquals("https://local.aiassistant", request.getHeader("HTTP-Referer"))
        assertEquals("AI Assistant", request.getHeader("X-OpenRouter-Title"))
    }

    @Test
    fun failedChatIncludesHttpStatusAndResponseBody() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"No auth credentials found"}}""")
        )

        val error = assertThrows(IOException::class.java) {
            client.completeChat(
                baseUrl = server.url("/v1").toString(),
                apiKey = "bad-key",
                requestBody = ChatCompletionRequest(
                    model = "m1",
                    messages = listOf(ChatMessageDto("user", "hi")),
                    stream = false
                )
            )
        }

        assertTrue(error.message.orEmpty().contains("HTTP 401"))
        assertTrue(error.message.orEmpty().contains("No auth credentials found"))
    }
}
