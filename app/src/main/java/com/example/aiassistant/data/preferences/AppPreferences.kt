package com.example.aiassistant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiassistant.data.openai.ModelDto
import com.example.aiassistant.data.repository.ApiProfile
import com.example.aiassistant.data.repository.ApiProfilePreferences
import com.example.aiassistant.data.repository.ApiProfileStoreState
import com.example.aiassistant.data.repository.PersistedApiProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) : ApiProfilePreferences {
    private val moshi = Moshi.Builder().build()
    private val modelListAdapter = moshi.adapter<List<ModelDto>>(
        Types.newParameterizedType(List::class.java, ModelDto::class.java)
    )
    private val profileListAdapter = moshi.adapter<List<PersistedApiProfile>>(
        Types.newParameterizedType(List::class.java, PersistedApiProfile::class.java)
    )

    private object Keys {
        val CurrentApiProfile = stringPreferencesKey("current_api_profile")
        val BaseUrl = stringPreferencesKey("base_url")
        val CurrentModel = stringPreferencesKey("current_model")
        val ManualContextLimit = intPreferencesKey("manual_context_limit")
        val Models = stringPreferencesKey("models")
        val Profiles = stringPreferencesKey("api_profiles")
        val ActiveProfileId = stringPreferencesKey("active_profile_id")
        val DarkModeEnabled = booleanPreferencesKey("dark_mode_enabled")
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DarkModeEnabled] ?: false }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DarkModeEnabled] = enabled
        }
    }

    suspend fun toggleDarkModeEnabled() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DarkModeEnabled] = !(prefs[Keys.DarkModeEnabled] ?: false)
        }
    }

    override fun loadStore(): ApiProfileStoreState = runBlocking {
        val prefs = context.dataStore.data.first()
        ApiProfileStoreState(
            profiles = prefs[Keys.Profiles]
                ?.let { json -> runCatching { profileListAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList()) }
                .orEmpty(),
            activeProfileId = prefs[Keys.ActiveProfileId]
        )
    }

    override fun saveStore(state: ApiProfileStoreState) {
        runBlocking {
            context.dataStore.edit { prefs ->
                if (state.profiles.isEmpty()) {
                    prefs.remove(Keys.Profiles)
                } else {
                    prefs[Keys.Profiles] = profileListAdapter.toJson(state.profiles)
                }
                state.activeProfileId?.let { prefs[Keys.ActiveProfileId] = it } ?: prefs.remove(Keys.ActiveProfileId)
            }
        }
    }

    override fun loadLegacyProfile(): PersistedApiProfile? = runBlocking {
        val prefs = context.dataStore.data.first()
        val baseUrl = prefs[Keys.BaseUrl]
        val modelId = prefs[Keys.CurrentModel]
        val manualContextLimit = prefs[Keys.ManualContextLimit]
        val models = prefs[Keys.Models]
            ?.let { json -> runCatching { modelListAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList()) }
            .orEmpty()
        if (baseUrl == null && modelId == null && manualContextLimit == null && models.isEmpty()) {
            null
        } else {
            PersistedApiProfile(
                id = prefs[Keys.CurrentApiProfile] ?: ApiProfile.DefaultProfileId,
                baseUrl = baseUrl,
                modelId = modelId,
                manualContextLimit = manualContextLimit,
                models = models
            )
        }
    }

    override fun clearLegacyProfile() {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.remove(Keys.CurrentApiProfile)
                prefs.remove(Keys.BaseUrl)
                prefs.remove(Keys.CurrentModel)
                prefs.remove(Keys.ManualContextLimit)
                prefs.remove(Keys.Models)
            }
        }
    }
}
