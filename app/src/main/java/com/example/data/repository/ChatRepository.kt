package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.MessageEntity
import com.example.data.remote.ImageGenerator
import com.example.data.remote.TextAiService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val imageDao = db.generatedImageDao()

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val allGeneratedImages: Flow<List<GeneratedImageEntity>> = imageDao.getAllImages()
    val favoriteImages: Flow<List<GeneratedImageEntity>> = imageDao.getFavoriteImages()

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun createNewConversation(
        title: String = "New Chat",
        modelName: String = "Llama 3.3"
    ): ConversationEntity {
        val newConv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            modelName = modelName
        )
        conversationDao.insertConversation(newConv)
        return newConv
    }

    suspend fun getConversationById(id: String): ConversationEntity? {
        return conversationDao.getConversationById(id)
    }

    suspend fun updateConversationTitle(id: String, newTitle: String) {
        val conv = conversationDao.getConversationById(id) ?: return
        conversationDao.updateConversation(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun deleteAllConversations() {
        conversationDao.deleteAllConversations()
    }

    suspend fun sendUserMessage(
        conversationId: String,
        userText: String,
        isForceImageMode: Boolean = false,
        textModelId: String = "llama",
        imageModelId: String = "flux",
        imageStyleId: String = "none",
        imageWidth: Int = 1024,
        imageHeight: Int = 1024,
        customApiKey: String? = null,
        attachedFile: com.example.util.AttachedFile? = null,
        onStreamingUpdate: ((String) -> Unit)? = null
    ): MessageEntity {
        val effectiveUserText = if (userText.isBlank()) {
            if (attachedFile?.isImage == true) "Please analyze and describe this image in detail."
            else if (attachedFile != null) "Please review and explain the content of this attached file."
            else userText
        } else userText

        // 1. Insert user message
        val userMsgId = UUID.randomUUID().toString()
        val userMsg = MessageEntity(
            id = userMsgId,
            conversationId = conversationId,
            role = "user",
            content = effectiveUserText,
            attachedFileName = attachedFile?.name,
            attachedFileType = if (attachedFile?.isImage == true) "image" else if (attachedFile != null) "document" else null,
            attachedFileSize = attachedFile?.sizeString,
            attachedFilePath = attachedFile?.localCachedPath ?: attachedFile?.uri?.toString()
        )
        messageDao.insertMessage(userMsg)

        // Update conversation's last message & title if it's the first message
        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            val title = if (conv.title == "New Chat") {
                if (effectiveUserText.length > 28) effectiveUserText.take(25) + "..." else effectiveUserText
            } else conv.title

            conversationDao.updateConversation(
                conv.copy(
                    title = title,
                    lastMessage = effectiveUserText,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 2. Check if this is an image generation request
        val isImage = isForceImageMode || ImageGenerator.isImagePrompt(effectiveUserText)
        if (isImage) {
            val cleanPrompt = ImageGenerator.extractCleanImagePrompt(effectiveUserText)
            val imageUrl = ImageGenerator.buildImageUrl(
                prompt = cleanPrompt,
                model = imageModelId,
                styleId = imageStyleId,
                width = imageWidth,
                height = imageHeight
            )

            // Save to generated images gallery
            val imgEntity = GeneratedImageEntity(
                id = UUID.randomUUID().toString(),
                prompt = cleanPrompt,
                imageUrl = imageUrl,
                model = imageModelId,
                style = imageStyleId,
                width = imageWidth,
                height = imageHeight
            )
            imageDao.insertImage(imgEntity)

            // Create assistant message
            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = "Here is your generated image with open-source **$imageModelId**:\n\n*\"$cleanPrompt\"*",
                imageUrl = imageUrl,
                modelName = imageModelId,
                isImage = true,
                promptUsed = cleanPrompt
            )
            messageDao.insertMessage(assistantMsg)
            return assistantMsg
        }

        // 3. Otherwise, call text AI model
        val historyList = messageDao.getMessagesList(conversationId)
            .filter { !it.isImage }
            .map { Pair(it.role, it.content) }

        val responseText = TextAiService.sendMessage(
            conversationHistory = historyList,
            userMessage = effectiveUserText,
            modelId = textModelId,
            customApiKey = customApiKey,
            attachedFileName = attachedFile?.name,
            attachedFileText = attachedFile?.textContent,
            attachedImageBase64 = attachedFile?.base64Data,
            attachedImageMimeType = attachedFile?.mimeType
        )

        val assistantMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = responseText,
            modelName = textModelId,
            isImage = false
        )
        messageDao.insertMessage(assistantMsg)
        return assistantMsg
    }

    suspend fun saveImageToGallery(
        prompt: String,
        imageUrl: String,
        model: String,
        style: String,
        aspectRatio: String,
        width: Int,
        height: Int
    ): GeneratedImageEntity {
        val entity = GeneratedImageEntity(
            id = UUID.randomUUID().toString(),
            prompt = prompt,
            imageUrl = imageUrl,
            model = model,
            style = style,
            aspectRatio = aspectRatio,
            width = width,
            height = height
        )
        imageDao.insertImage(entity)
        return entity
    }

    suspend fun toggleImageFavorite(image: GeneratedImageEntity) {
        imageDao.updateImage(image.copy(isFavorite = !image.isFavorite))
    }

    suspend fun deleteGeneratedImage(image: GeneratedImageEntity) {
        imageDao.deleteImage(image)
    }

    suspend fun enhancePrompt(rawPrompt: String): String {
        return TextAiService.enhanceImagePrompt(rawPrompt)
    }
}
