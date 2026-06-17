package com.example.aiassistant.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, AttachmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AiDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
