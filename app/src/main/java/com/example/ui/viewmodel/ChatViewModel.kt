package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    CHAT,
    IMAGE_STUDIO
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)
    private val prefs = application.getSharedPreferences("omnichat_prefs", android.content.Context.MODE_PRIVATE)

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val galleryImages: StateFlow<List<GeneratedImageEntity>> = repository.allGeneratedImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val currentMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) {
                repository.getMessagesForConversation(convId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTextModel = MutableStateFlow(prefs.getString("selected_text_model", "llama") ?: "llama")
    val selectedTextModel: StateFlow<String> = _selectedTextModel.asStateFlow()

    private val _selectedImageModel = MutableStateFlow(prefs.getString("selected_image_model", "flux") ?: "flux")
    val selectedImageModel: StateFlow<String> = _selectedImageModel.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var currentJob: Job? = null
    private var lastUserPrompt: String? = null

    init {
        // Initialize or pick latest conversation
        viewModelScope.launch {
            conversations.collect { list ->
                if (_activeConversationId.value == null && list.isNotEmpty()) {
                    _activeConversationId.value = list.first().id
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        _currentScreen.value = AppScreen.CHAT
    }

    fun startNewChat() {
        viewModelScope.launch {
            val conv = repository.createNewConversation(modelName = _selectedTextModel.value)
            _activeConversationId.value = conv.id
            _currentScreen.value = AppScreen.CHAT
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = null
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.deleteAllConversations()
            _activeConversationId.value = null
        }
    }

    fun selectTextModel(modelId: String) {
        _selectedTextModel.value = modelId
        prefs.edit().putString("selected_text_model", modelId).apply()
    }

    fun selectImageModel(modelId: String) {
        _selectedImageModel.value = modelId
        prefs.edit().putString("selected_image_model", modelId).apply()
    }

    private val _attachedFile = MutableStateFlow<com.example.util.AttachedFile?>(null)
    val attachedFile: StateFlow<com.example.util.AttachedFile?> = _attachedFile.asStateFlow()

    fun setAttachedFile(file: com.example.util.AttachedFile?) {
        _attachedFile.value = file
    }

    fun clearAttachedFile() {
        _attachedFile.value = null
    }

    fun sendMessage(text: String, isImageMode: Boolean = false) {
        val currentAttached = _attachedFile.value
        if ((text.isBlank() && currentAttached == null) || _isGenerating.value) return

        lastUserPrompt = text
        _isGenerating.value = true
        _attachedFile.value = null // clear attached file after dispatch

        currentJob = viewModelScope.launch {
            try {
                // Ensure conversation exists
                var convId = _activeConversationId.value
                if (convId == null) {
                    val newConv = repository.createNewConversation(modelName = _selectedTextModel.value)
                    convId = newConv.id
                    _activeConversationId.value = convId
                }

                val customApiKey = prefs.getString("custom_api_key", null)

                repository.sendUserMessage(
                    conversationId = convId,
                    userText = text,
                    isForceImageMode = isImageMode,
                    textModelId = _selectedTextModel.value,
                    imageModelId = _selectedImageModel.value,
                    customApiKey = customApiKey,
                    attachedFile = currentAttached
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGenerating() {
        currentJob?.cancel()
        _isGenerating.value = false
    }

    fun regenerateLast() {
        val prompt = lastUserPrompt ?: return
        sendMessage(prompt, isImageMode = false)
    }

    fun generateImageInStudio(prompt: String, styleId: String, width: Int, height: Int) {
        viewModelScope.launch {
            val imageUrl = com.example.data.remote.ImageGenerator.buildImageUrl(
                prompt = prompt,
                model = _selectedImageModel.value,
                styleId = styleId,
                width = width,
                height = height
            )

            repository.saveImageToGallery(
                prompt = prompt,
                imageUrl = imageUrl,
                model = _selectedImageModel.value,
                style = styleId,
                aspectRatio = "${width}x${height}",
                width = width,
                height = height
            )
        }
    }

    fun toggleImageFavorite(image: GeneratedImageEntity) {
        viewModelScope.launch {
            repository.toggleImageFavorite(image)
        }
    }

    suspend fun enhancePrompt(rawPrompt: String): String {
        return repository.enhancePrompt(rawPrompt)
    }

    fun updateSettings(textModel: String, imageModel: String, customApiKey: String) {
        _selectedTextModel.value = textModel
        _selectedImageModel.value = imageModel
        prefs.edit()
            .putString("selected_text_model", textModel)
            .putString("selected_image_model", imageModel)
            .putString("custom_api_key", customApiKey)
            .apply()
    }
}
