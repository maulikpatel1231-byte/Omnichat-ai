package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.components.ChatDrawerContent
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                OmniChatApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OmniChatApp(viewModel: ChatViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val currentMessages by viewModel.currentMessages.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTextModel by viewModel.selectedTextModel.collectAsState()
    val selectedImageModel by viewModel.selectedImageModel.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val attachedFile by viewModel.attachedFile.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Android Back Handler
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.CHAT) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen != AppScreen.CHAT) {
            viewModel.navigateTo(AppScreen.CHAT)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawerContent(
                conversations = conversations,
                activeConversationId = activeConversationId,
                onSelectConversation = { convId ->
                    viewModel.selectConversation(convId)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteConversation = { convId ->
                    viewModel.deleteConversation(convId)
                },
                onClearAll = {
                    viewModel.clearAllConversations()
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenImageStudio = {
                    viewModel.navigateTo(AppScreen.IMAGE_STUDIO)
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    showSettingsDialog = true
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        when (currentScreen) {
            AppScreen.CHAT -> {
                ChatScreen(
                    messages = currentMessages,
                    selectedTextModel = selectedTextModel,
                    selectedImageModel = selectedImageModel,
                    isGenerating = isGenerating,
                    attachedFile = attachedFile,
                    onSelectTextModel = { viewModel.selectTextModel(it) },
                    onSendMessage = { text, isImage -> viewModel.sendMessage(text, isImage) },
                    onAttachFile = { viewModel.setAttachedFile(it) },
                    onClearAttachedFile = { viewModel.clearAttachedFile() },
                    onStopGenerating = { viewModel.stopGenerating() },
                    onNewChat = { viewModel.startNewChat() },
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onOpenImageStudio = { viewModel.navigateTo(AppScreen.IMAGE_STUDIO) },
                    onRegenerate = { viewModel.regenerateLast() }
                )
            }
            AppScreen.IMAGE_STUDIO -> {
                ImageStudioScreen(
                    currentImageModel = selectedImageModel,
                    galleryImages = galleryImages,
                    onBack = { viewModel.navigateTo(AppScreen.CHAT) },
                    onGenerate = { prompt, styleId, width, height ->
                        viewModel.generateImageInStudio(prompt, styleId, width, height)
                    },
                    onToggleFavorite = { img -> viewModel.toggleImageFavorite(img) },
                    onEnhancePrompt = { raw -> viewModel.enhancePrompt(raw) }
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentTextModel = selectedTextModel,
            currentImageModel = selectedImageModel,
            onSaveSettings = { textModel, imageModel, apiKey ->
                viewModel.updateSettings(textModel, imageModel, apiKey)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
