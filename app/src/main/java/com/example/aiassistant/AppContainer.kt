package com.example.aiassistant

import android.content.Context
import androidx.room.Room
import com.example.aiassistant.data.db.AiDatabase
import com.example.aiassistant.data.openai.OpenAiClient
import com.example.aiassistant.data.preferences.AppPreferences
import com.example.aiassistant.data.repository.ApiProfileRepository
import com.example.aiassistant.data.repository.ChatRepository
import com.example.aiassistant.data.repository.ConversationRepository
import com.example.aiassistant.data.security.ApiKeyStore
import com.example.aiassistant.domain.context.ModelContextResolver

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AiDatabase::class.java,
        "ai_assistant.db"
    ).build()

    private val openAiClient = OpenAiClient()
    private val contextResolver = ModelContextResolver()
    val appPreferences = AppPreferences(context.applicationContext)

    val apiProfiles = ApiProfileRepository(ApiKeyStore(context.applicationContext), openAiClient, appPreferences)
    val conversations = ConversationRepository(database.conversationDao())
    val chat = ChatRepository(openAiClient)
}
