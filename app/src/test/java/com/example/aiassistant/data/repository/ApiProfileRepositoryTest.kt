package com.example.aiassistant.data.repository

import com.example.aiassistant.data.openai.ChatCompletionRequest
import com.example.aiassistant.data.openai.ModelDto
import com.example.aiassistant.data.openai.OpenAiApi
import com.example.aiassistant.data.openai.OpenAiClient
import com.example.aiassistant.data.openai.OpenAiFileAttachment
import com.example.aiassistant.data.openai.OpenAiImageAttachment
import com.example.aiassistant.data.security.ApiKeyStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiProfileRepositoryTest {
    @Test
    fun newRepositoryStartsWithNoProfilesAndNoActiveProfile() {
        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences()
        )

        assertEquals(emptyList<ApiProfile>(), repository.profiles())
        assertNull(repository.activeProfile())
        assertEquals(ApiProfile(), repository.currentProfile())
        assertEquals("", repository.apiKey())
        assertEquals(emptyList<ModelDto>(), repository.currentModels())
    }

    @Test
    fun creatingMultipleProfilesKeepsThemPersistedAndSelectable() {
        val preferences = InMemoryApiProfilePreferences()
        val keyStore = InMemoryApiKeyStore()
        val first = ApiProfileRepository(keyStore, OpenAiClient(), preferences)

        val alphaId = first.createProfile(
            baseUrl = "https://alpha.example.com/v1",
            apiKey = "alpha-key",
            modelId = "alpha-model",
            manualContextLimit = 12345
        )
        val betaId = first.createProfile(
            baseUrl = "https://beta.example.com/v1",
            apiKey = "beta-key",
            modelId = "beta-model",
            manualContextLimit = 54321
        )

        val second = ApiProfileRepository(keyStore, OpenAiClient(), preferences)

        assertEquals(listOf(alphaId, betaId), second.profiles().map { it.id })
        assertEquals("https://alpha.example.com/v1", second.profile(alphaId)?.baseUrl)
        assertEquals("https://beta.example.com/v1", second.profile(betaId)?.baseUrl)
        assertEquals("beta-key", second.apiKey(betaId))
        assertNull(second.activeProfile())
    }

    @Test
    fun enablingProfileChangesActiveProfileOnly() {
        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences()
        )

        val alphaId = repository.createProfile(
            baseUrl = "https://alpha.example.com/v1",
            apiKey = "alpha-key",
            modelId = "alpha-model"
        )
        val betaId = repository.createProfile(
            baseUrl = "https://beta.example.com/v1",
            apiKey = "beta-key",
            modelId = "beta-model"
        )

        repository.enableProfile(betaId)

        assertEquals(betaId, repository.activeProfile()?.id)
        assertEquals("https://beta.example.com/v1", repository.currentProfile().baseUrl)
        assertEquals("beta-key", repository.apiKey())
        assertEquals("alpha-model", repository.profile(alphaId)?.modelId)
    }

    @Test
    fun updatingExistingProfileReplacesFieldsWithoutCreatingDuplicate() {
        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences()
        )

        val profileId = repository.createProfile(
            baseUrl = "https://old.example.com/v1",
            apiKey = "old-key",
            modelId = "old-model",
            manualContextLimit = 111
        )

        repository.updateProfile(
            profileId = profileId,
            baseUrl = "https://new.example.com/v1",
            apiKey = "new-key",
            modelId = "new-model",
            manualContextLimit = 222
        )

        assertEquals(1, repository.profiles().size)
        assertEquals("https://new.example.com/v1", repository.profile(profileId)?.baseUrl)
        assertEquals("new-model", repository.profile(profileId)?.modelId)
        assertEquals(222, repository.profile(profileId)?.manualContextLimit)
        assertEquals("new-key", repository.apiKey(profileId))
    }

    @Test
    fun deletingNonActiveProfileKeepsActiveProfileUntouched() {
        val keyStore = InMemoryApiKeyStore()
        val repository = ApiProfileRepository(
            apiKeyStore = keyStore,
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences()
        )

        val alphaId = repository.createProfile(
            baseUrl = "https://alpha.example.com/v1",
            apiKey = "alpha-key",
            modelId = "alpha-model"
        )
        val betaId = repository.createProfile(
            baseUrl = "https://beta.example.com/v1",
            apiKey = "beta-key",
            modelId = "beta-model"
        )
        repository.enableProfile(betaId)

        repository.deleteProfile(alphaId)

        assertEquals(listOf(betaId), repository.profiles().map { it.id })
        assertEquals(betaId, repository.activeProfile()?.id)
        assertNull(repository.profile(alphaId))
        assertNull(keyStore.getApiKey(alphaId))
    }

    @Test
    fun deletingActiveProfileClearsActiveSelectionAndDeletesKey() {
        val keyStore = InMemoryApiKeyStore()
        val repository = ApiProfileRepository(
            apiKeyStore = keyStore,
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences()
        )

        val profileId = repository.createProfile(
            baseUrl = "https://active.example.com/v1",
            apiKey = "active-key",
            modelId = "active-model"
        )

        repository.deleteProfile(profileId)

        assertEquals(emptyList<ApiProfile>(), repository.profiles())
        assertNull(repository.activeProfile())
        assertEquals(ApiProfile(), repository.currentProfile())
        assertEquals("", repository.apiKey())
        assertNull(keyStore.getApiKey(profileId))
    }

    @Test
    fun persistedModelListIsLoadedForActiveProfile() {
        val preferences = InMemoryApiProfilePreferences(
            ApiProfileStoreState(
                profiles = listOf(
                    PersistedApiProfile(
                        id = "profile-a",
                        baseUrl = "https://example.com/v1",
                        modelId = "model-a",
                        models = listOf(
                            ModelDto(id = "model-a", contextLength = 32000),
                            ModelDto(id = "model-b", contextLength = 64000)
                        )
                    )
                ),
                activeProfileId = "profile-a"
            )
        )

        val repository = ApiProfileRepository(InMemoryApiKeyStore(), OpenAiClient(), preferences)

        assertEquals("https://example.com/v1", repository.currentProfile().baseUrl)
        assertEquals("model-a", repository.currentProfile().modelId)
        assertEquals(32000, repository.currentProfile().modelContextById["model-a"])
        assertEquals(64000, repository.currentProfile().modelContextById["model-b"])
        assertEquals(2, repository.currentModels().size)
    }

    @Test
    fun profileSpecificModelListsCanBeReadBack() {
        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = OpenAiClient(),
            preferences = InMemoryApiProfilePreferences(
                ApiProfileStoreState(
                    profiles = listOf(
                        PersistedApiProfile(
                            id = "alpha",
                            baseUrl = "https://alpha.example.com/v1",
                            modelId = "alpha-model",
                            models = listOf(ModelDto(id = "alpha-model", contextLength = 1))
                        ),
                        PersistedApiProfile(
                            id = "beta",
                            baseUrl = "https://beta.example.com/v1",
                            modelId = "beta-model",
                            models = listOf(ModelDto(id = "beta-model", contextLength = 2))
                        )
                    ),
                    activeProfileId = "alpha"
                )
            )
        )

        assertEquals(1, repository.models("alpha").size)
        assertEquals(1, repository.models("beta").size)
    }

    @Test
    fun testConnectionKeepsSelectedModelWhenItStillExists() = runTest {
        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = ListingOpenAiClient(
                listOf(
                    ModelDto(id = "openrouter/fusion", contextLength = 200000),
                    ModelDto(id = "google/gemma-4-31b-it:free", contextLength = 131072)
                )
            ),
            preferences = InMemoryApiProfilePreferences()
        )

        val profileId = repository.createProfile(
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "key",
            modelId = "google/gemma-4-31b-it:free"
        )
        repository.enableProfile(profileId)
        repository.testConnection("https://openrouter.ai/api/v1", "key")

        assertEquals("google/gemma-4-31b-it:free", repository.currentProfile().modelId)
    }

    @Test
    fun legacySingleProfileStateMigratesIntoProfileList() {
        val preferences = InMemoryApiProfilePreferences(
            legacyProfile = PersistedApiProfile(
                id = "legacy-profile",
                baseUrl = "https://legacy.example.com/v1",
                modelId = "legacy-model",
                manualContextLimit = 4096
            )
        )

        val repository = ApiProfileRepository(
            apiKeyStore = InMemoryApiKeyStore(),
            client = OpenAiClient(),
            preferences = preferences
        )

        assertEquals(1, repository.profiles().size)
        assertEquals("legacy-profile", repository.activeProfile()?.id)
        assertTrue(preferences.savedStates.isNotEmpty())
    }

    @Test
    fun persistedApiProfileCanBeSerializedByMoshiCodegen() {
        val adapter = Moshi.Builder().build().adapter<List<PersistedApiProfile>>(
            Types.newParameterizedType(List::class.java, PersistedApiProfile::class.java)
        )

        val json = adapter.toJson(
            listOf(
                PersistedApiProfile(
                    id = "profile-1",
                    baseUrl = "https://example.com/v1",
                    modelId = "model-1",
                    manualContextLimit = 1234,
                    models = listOf(ModelDto(id = "model-1", contextLength = 32000))
                )
            )
        )

        assertTrue(json.contains("profile-1"))
        assertTrue(json.contains("https://example.com/v1"))
    }
}

private class InMemoryApiProfilePreferences(
    initialState: ApiProfileStoreState = ApiProfileStoreState(),
    private var legacyProfile: PersistedApiProfile? = null
) : ApiProfilePreferences {
    var snapshot = initialState
    val savedStates = mutableListOf<ApiProfileStoreState>()

    override fun loadStore(): ApiProfileStoreState = snapshot

    override fun saveStore(state: ApiProfileStoreState) {
        snapshot = state
        savedStates += state
    }

    override fun loadLegacyProfile(): PersistedApiProfile? = legacyProfile

    override fun clearLegacyProfile() {
        legacyProfile = null
    }
}

private class InMemoryApiKeyStore : ApiKeyStorage {
    private val keys = mutableMapOf<String, String>()

    override fun saveApiKey(profileId: String, apiKey: String) {
        keys[profileId] = apiKey
    }

    override fun getApiKey(profileId: String): String? = keys[profileId]

    override fun deleteApiKey(profileId: String) {
        keys.remove(profileId)
    }
}

private class ListingOpenAiClient(
    private val models: List<ModelDto>
) : OpenAiApi {
    override fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = models

    override fun streamChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): Flow<String> = emptyFlow()

    override fun completeChat(
        baseUrl: String,
        apiKey: String,
        requestBody: ChatCompletionRequest,
        images: List<OpenAiImageAttachment>
    ): String = ""

    override fun completeResponseWithFiles(
        baseUrl: String,
        apiKey: String,
        model: String,
        inputText: String,
        files: List<OpenAiFileAttachment>
    ): String = ""
}
