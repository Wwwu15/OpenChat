package com.example.aiassistant.data.repository

import com.example.aiassistant.data.openai.ModelDto
import com.example.aiassistant.data.openai.OpenAiApi
import com.example.aiassistant.data.security.ApiKeyStorage
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class ApiProfile(
    val id: String = DefaultProfileId,
    val baseUrl: String = "",
    val modelId: String = "",
    val manualContextLimit: Int? = null,
    val modelContextById: Map<String, Int?> = emptyMap()
) {
    companion object {
        const val DefaultProfileId = "default"
    }
}

@JsonClass(generateAdapter = true)
data class PersistedApiProfile(
    val id: String = ApiProfile.DefaultProfileId,
    val baseUrl: String? = null,
    val modelId: String? = null,
    val manualContextLimit: Int? = null,
    val models: List<ModelDto> = emptyList()
)

data class ApiProfileStoreState(
    val profiles: List<PersistedApiProfile> = emptyList(),
    val activeProfileId: String? = null
)

interface ApiProfilePreferences {
    fun loadStore(): ApiProfileStoreState
    fun saveStore(state: ApiProfileStoreState)
    fun loadLegacyProfile(): PersistedApiProfile? = null
    fun clearLegacyProfile() = Unit
}

class NoOpApiProfilePreferences : ApiProfilePreferences {
    override fun loadStore(): ApiProfileStoreState = ApiProfileStoreState()
    override fun saveStore(state: ApiProfileStoreState) = Unit
}

class ApiProfileRepository(
    private val apiKeyStore: ApiKeyStorage,
    private val client: OpenAiApi,
    private val preferences: ApiProfilePreferences = NoOpApiProfilePreferences()
) {
    private var state = migrateIfNeeded(preferences.loadStore(), preferences.loadLegacyProfile())

    init {
        persist()
        preferences.clearLegacyProfile()
    }

    fun profiles(): List<ApiProfile> = state.profiles.map { it.toApiProfile() }

    fun profile(profileId: String): ApiProfile? = state.profiles.firstOrNull { it.id == profileId }?.toApiProfile()

    fun activeProfile(): ApiProfile? = profile(state.activeProfileId.orEmpty())

    fun currentProfile(): ApiProfile = activeProfile() ?: ApiProfile()

    fun models(profileId: String): List<ModelDto> = state.profiles.firstOrNull { it.id == profileId }?.models.orEmpty()

    fun currentModels(): List<ModelDto> = activeProfile()?.let { profile ->
        state.profiles.firstOrNull { it.id == profile.id }?.models.orEmpty()
    }.orEmpty()

    fun apiKey(profileId: String = currentProfile().id): String = apiKeyStore.getApiKey(profileId).orEmpty()

    fun createProfile(
        baseUrl: String,
        apiKey: String,
        modelId: String = "",
        manualContextLimit: Int? = null,
        models: List<ModelDto> = emptyList()
    ): String {
        val id = UUID.randomUUID().toString()
        val profile = PersistedApiProfile(
            id = id,
            baseUrl = baseUrl.trim(),
            modelId = modelId,
            manualContextLimit = manualContextLimit,
            models = models
        )
        state = state.copy(
            profiles = state.profiles + profile,
            activeProfileId = state.activeProfileId
        )
        persist()
        saveKeyIfPresent(id, apiKey)
        return id
    }

    fun updateProfile(
        profileId: String,
        baseUrl: String,
        apiKey: String,
        modelId: String,
        manualContextLimit: Int?,
        models: List<ModelDto> = profile(profileId)?.let { current -> state.profileState(profileId).models }.orEmpty()
    ) {
        state = state.copy(
            profiles = state.profiles.map { existing ->
                if (existing.id != profileId) existing else existing.copy(
                    baseUrl = baseUrl.trim(),
                    modelId = modelId,
                    manualContextLimit = manualContextLimit,
                    models = models
                )
            }
        )
        persist()
        saveKeyIfPresent(profileId, apiKey)
    }

    fun enableProfile(profileId: String) {
        if (state.profiles.none { it.id == profileId }) return
        state = state.copy(activeProfileId = profileId)
        persist()
    }

    fun deleteProfile(profileId: String) {
        state = state.copy(
            profiles = state.profiles.filterNot { it.id == profileId },
            activeProfileId = state.activeProfileId.takeUnless { it == profileId }
        )
        persist()
        apiKeyStore.deleteApiKey(profileId)
    }

    fun save(
        baseUrl: String,
        apiKey: String,
        modelId: String = currentProfile().modelId,
        manualContextLimit: Int? = currentProfile().manualContextLimit
    ) {
        val current = activeProfile()
        if (current == null) {
            createProfile(baseUrl, apiKey, modelId, manualContextLimit)
        } else {
            updateProfile(current.id, baseUrl, apiKey, modelId, manualContextLimit, models(current.id))
        }
    }

    fun selectModel(modelId: String) {
        val current = activeProfile() ?: return
        updateProfile(
            profileId = current.id,
            baseUrl = current.baseUrl,
            apiKey = apiKey(current.id),
            modelId = modelId,
            manualContextLimit = current.manualContextLimit,
            models = currentModels()
        )
    }

    fun setManualContextLimit(limit: Int?) {
        val current = activeProfile() ?: return
        updateProfile(
            profileId = current.id,
            baseUrl = current.baseUrl,
            apiKey = apiKey(current.id),
            modelId = current.modelId,
            manualContextLimit = limit,
            models = currentModels()
        )
    }

    suspend fun testConnection(baseUrl: String, apiKey: String): List<ModelDto> = withContext(Dispatchers.IO) {
        val fetched = fetchModels(baseUrl, apiKey)
        val current = activeProfile()
        val selected = current?.modelId?.takeIf { modelId -> fetched.any { it.id == modelId } }
            ?: fetched.firstOrNull()?.id.orEmpty()
        if (current == null) {
            createProfile(baseUrl, apiKey, selected, null, fetched)
        } else {
            updateProfile(current.id, baseUrl, apiKey, selected, current.manualContextLimit, fetched)
        }
        fetched
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<ModelDto> = withContext(Dispatchers.IO) {
        client.fetchModels(baseUrl, apiKey)
    }

    private fun saveKeyIfPresent(profileId: String, apiKey: String) {
        if (apiKey.isNotBlank()) {
            apiKeyStore.saveApiKey(profileId, apiKey)
        }
    }

    private fun persist() {
        preferences.saveStore(state)
    }

    private fun migrateIfNeeded(
        store: ApiProfileStoreState,
        legacyProfile: PersistedApiProfile?
    ): ApiProfileStoreState {
        if (store.profiles.isNotEmpty() || legacyProfile == null) return store
        return ApiProfileStoreState(
            profiles = listOf(legacyProfile),
            activeProfileId = legacyProfile.id
        )
    }

    private fun ApiProfileStoreState.profileState(profileId: String): PersistedApiProfile {
        return profiles.first { it.id == profileId }
    }

    private fun PersistedApiProfile.toApiProfile(): ApiProfile {
        return ApiProfile(
            id = id,
            baseUrl = baseUrl.orEmpty(),
            modelId = modelId.orEmpty(),
            manualContextLimit = manualContextLimit,
            modelContextById = models.associate { it.id to it.contextLength }
        )
    }
}
