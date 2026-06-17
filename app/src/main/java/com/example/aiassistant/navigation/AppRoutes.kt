package com.example.aiassistant.navigation

object AppRoutes {
    const val Chat = "chat"
    const val ChatWithConversation = "chat/{conversationId}"
    const val History = "history"
    const val ApiManagement = "api-management"
    const val Settings = "settings"
    const val SettingsWithProfile = "settings?profileId={profileId}"
    const val Models = "models"
    const val States = "states"

    fun chat(conversationId: String): String = "chat/$conversationId"
    fun settings(profileId: String? = null): String = if (profileId.isNullOrBlank()) Settings else "settings?profileId=$profileId"
}
