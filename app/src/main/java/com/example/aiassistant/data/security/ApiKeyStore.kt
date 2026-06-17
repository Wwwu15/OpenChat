package com.example.aiassistant.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface ApiKeyStorage {
    fun saveApiKey(profileId: String, apiKey: String)
    fun getApiKey(profileId: String): String?
    fun deleteApiKey(profileId: String)
}

class ApiKeyStore(private val context: Context) : ApiKeyStorage {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveApiKey(profileId: String, apiKey: String) {
        prefs.edit().putString(profileId, apiKey).apply()
    }

    override fun getApiKey(profileId: String): String? = prefs.getString(profileId, null)

    override fun deleteApiKey(profileId: String) {
        prefs.edit().remove(profileId).apply()
    }
}
