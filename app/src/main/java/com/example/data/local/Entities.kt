package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val modelName: String = "Llama 3.3 (Open-Source)"
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val imageUrl: String? = null,
    val modelName: String = "Llama 3.3",
    val timestamp: Long = System.currentTimeMillis(),
    val isImage: Boolean = false,
    val promptUsed: String? = null,
    val attachedFileName: String? = null,
    val attachedFileType: String? = null,
    val attachedFileSize: String? = null,
    val attachedFilePath: String? = null
)

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey
    val id: String,
    val prompt: String,
    val imageUrl: String,
    val model: String,
    val style: String = "General",
    val aspectRatio: String = "1:1",
    val width: Int = 1024,
    val height: Int = 1024,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
