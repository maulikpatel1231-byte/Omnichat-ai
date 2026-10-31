package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.MessageEntity
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.MarkdownRenderer
import com.example.ui.components.ModelSelectorDropdown
import com.example.ui.components.saveImageToGallery
import com.example.ui.theme.DarkUserBubble
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Zinc300
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc850
import com.example.ui.theme.Zinc900
import com.example.util.AttachedFile
import com.example.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<MessageEntity>,
    selectedTextModel: String,
    selectedImageModel: String,
    isGenerating: Boolean,
    attachedFile: AttachedFile?,
    onSelectTextModel: (String) -> Unit,
    onSendMessage: (text: String, isImageMode: Boolean) -> Unit,
    onAttachFile: (AttachedFile) -> Unit,
    onClearAttachedFile: () -> Unit,
    onStopGenerating: () -> Unit,
    onNewChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenImageStudio: () -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isImageMode by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // Dialog preview for tapped images
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewPrompt by remember { mutableStateOf("") }
    var previewModel by remember { mutableStateOf("") }

    // Activity Result Launchers for Media & Document Picking
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val processed = FileUtils.processUri(context, uri)
                onAttachFile(processed)
                Toast.makeText(context, "Image imported: ${processed.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val processed = FileUtils.processUri(context, uri)
                onAttachFile(processed)
                Toast.makeText(context, "File imported: ${processed.name} (${processed.sizeString})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Text to Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc900)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open sidebar",
                    tint = Color.White
                )
            }

            ModelSelectorDropdown(
                selectedModelId = selectedTextModel,
                onModelSelected = onSelectTextModel
            )

            Row {
                IconButton(
                    onClick = onOpenImageStudio,
                    modifier = Modifier.testTag("image_studio_quick_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Open Image Studio",
                        tint = ElectricCyan
                    )
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("new_chat_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Color.White
                    )
                }
            }
        }

        // Messages Area / Empty Welcome State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                WelcomeEmptyState(
                    onSuggestionClicked = { promptText, forceImage ->
                        inputText = promptText
                        isImageMode = forceImage
                        onSendMessage(promptText, forceImage)
                        inputText = ""
                    },
                    onAttachFileClicked = { showAttachmentSheet = true }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageItemView(
                            message = message,
                            onImageClicked = { url, prompt, model ->
                                previewImageUrl = url
                                previewPrompt = prompt
                                previewModel = model
                            },
                            onSpeak = { text ->
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
                            },
                            onRegenerate = onRegenerate
                        )
                    }

                    if (isGenerating) {
                        item {
                            GeneratingIndicatorView()
                        }
                    }
                }
            }
        }

        // Bottom Chat Input Area
        ChatInputBar(
            inputText = inputText,
            onInputTextChanged = { inputText = it },
            isImageMode = isImageMode,
            onToggleImageMode = { isImageMode = !isImageMode },
            isGenerating = isGenerating,
            attachedFile = attachedFile,
            onOpenAttachmentSheet = { showAttachmentSheet = true },
            onClearAttachedFile = onClearAttachedFile,
            onSendMessage = {
                if (inputText.isNotBlank() || attachedFile != null) {
                    onSendMessage(inputText, isImageMode)
                    inputText = ""
                }
            },
            onStopGenerating = onStopGenerating
        )
    }

    // Attachment Picker BottomSheet
    if (showAttachmentSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = Zinc850
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Import File / Media",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Attach photos for vision analysis, or import code/documents for AI review",
                    style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Photos & Images Option
                Surface(
                    onClick = {
                        showAttachmentSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Zinc800,
                    modifier = Modifier.fillMaxWidth().testTag("pick_photo_option")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = ElectricCyan)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Photos & Images",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Gallery images, screenshots, diagrams",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Documents & Files Option
                Surface(
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch("*/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Zinc800,
                    modifier = Modifier.fillMaxWidth().testTag("pick_document_option")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Documents & Code Files",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "PDF, Python, JSON, CSV, TXT, Kotlin, JS, Markdown",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Fullscreen Image Dialog
    previewImageUrl?.let { url ->
        ImagePreviewDialog(
            imageUrl = url,
            prompt = previewPrompt,
            modelName = previewModel,
            onDismiss = { previewImageUrl = null }
        )
    }
}

@Composable
fun WelcomeEmptyState(
    onSuggestionClicked: (String, Boolean) -> Unit,
    onAttachFileClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OmniChat AI",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Text(
            text = "ChatGPT Intelligence + Free Image Generation & File Import\nPowered by Open-Source models",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Zinc400,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Suggestion Cards Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionCard(
                icon = Icons.Default.AttachFile,
                title = "Import a File or Photo",
                subtitle = "Upload code, document, or image for AI analysis",
                accentColor = EmeraldPrimary,
                onClick = onAttachFileClicked
            )

            SuggestionCard(
                icon = Icons.Default.Palette,
                title = "Generate with Flux AI",
                subtitle = "Cyberpunk neon samurai in rainy Tokyo, 8k",
                accentColor = ElectricCyan,
                onClick = { onSuggestionClicked("A futuristic cyberpunk samurai standing in rainy neon Tokyo with glowing katana, 8k resolution, cinematic lighting", true) }
            )

            SuggestionCard(
                icon = Icons.Default.Code,
                title = "Write Code & Algorithms",
                subtitle = "Python script to scrape and parse weather data",
                accentColor = EmeraldLight,
                onClick = { onSuggestionClicked("Write a clean, commented Python script using requests and BeautifulSoup to scrape and parse weather data with error handling.", false) }
            )
        }
    }
}

@Composable
fun SuggestionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Zinc800),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("suggestion_card_${title.take(6)}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Zinc400,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MessageItemView(
    message: MessageEntity,
    onImageClicked: (url: String, prompt: String, model: String) -> Unit,
    onSpeak: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isUser = message.role == "user"

    if (isUser) {
        // User Message (Right Aligned Bubble with optional attachment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkUserBubble,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // If an attached file was sent with this message
                    if (!message.attachedFileName.isNullOrBlank()) {
                        if (message.attachedFileType == "image" && !message.attachedFilePath.isNullOrBlank()) {
                            // Image attachment thumbnail
                            val imageFile = File(message.attachedFilePath)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Zinc800)
                                    .clickable {
                                        onImageClicked(
                                            message.attachedFilePath,
                                            message.attachedFileName,
                                            "Attached Image"
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = if (imageFile.exists()) imageFile else message.attachedFilePath,
                                    contentDescription = message.attachedFileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            // Document attachment badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Zinc800)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.attachedFileName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!message.attachedFileSize.isNullOrBlank()) {
                                        Text(
                                            text = message.attachedFileSize,
                                            style = MaterialTheme.typography.labelSmall.copy(color = Zinc400)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
            }
        }
    } else {
        // Assistant Message (Left Aligned with AI Avatar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(EmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // If it's an image generation message
                if (message.isImage && !message.imageUrl.isNullOrBlank()) {
                    Text(
                        text = "Generated with Open-Source ${message.modelName}:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Image Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Zinc800)
                            .clickable {
                                onImageClicked(
                                    message.imageUrl,
                                    message.promptUsed ?: "Generated image",
                                    message.modelName
                                )
                            }
                    ) {
                        SubcomposeAsyncImage(
                            model = message.imageUrl,
                            contentDescription = message.promptUsed ?: "Generated image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Rendering image...", style = MaterialTheme.typography.labelSmall.copy(color = Zinc400))
                                    }
                                }
                            }
                        )
                    }

                    // Quick image actions
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val success = saveImageToGallery(context, message.imageUrl, message.promptUsed.orEmpty())
                                    if (success) {
                                        Toast.makeText(context, "Saved image to Gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Zinc400, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Generated with OmniChat AI:\n${message.imageUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Zinc400, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // Regular Markdown Text
                    MarkdownRenderer(text = message.content)

                    // Actions row (Copy, Speak, Share, Regenerate)
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("response", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Zinc400, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { onSpeak(message.content) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = Zinc400, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message.content)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share text"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Zinc400, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Zinc400, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingIndicatorView() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Zinc800)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = EmeraldLight
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thinking & analyzing...",
                style = MaterialTheme.typography.bodySmall.copy(color = Zinc300)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    isImageMode: Boolean,
    onToggleImageMode: () -> Unit,
    isGenerating: Boolean,
    attachedFile: AttachedFile?,
    onOpenAttachmentSheet: () -> Unit,
    onClearAttachedFile: () -> Unit,
    onSendMessage: () -> Unit,
    onStopGenerating: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Zinc900
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Mode toggle tag (Image vs Chat)
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onToggleImageMode,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isImageMode) ElectricCyan else Zinc800,
                    modifier = Modifier.testTag("toggle_image_mode_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (isImageMode) Color.Black else Zinc400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isImageMode) "🎨 Flux Image Mode (Active)" else "💬 Text Chat (Tap for Image)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isImageMode) Color.Black else Zinc400
                            )
                        )
                    }
                }
            }

            // Attached File Preview Chip (if any file is currently selected)
            AnimatedVisibility(visible = attachedFile != null) {
                attachedFile?.let { file ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Zinc800
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (file.isImage && file.localCachedPath != null) {
                                    AsyncImage(
                                        model = File(file.localCachedPath),
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = EmeraldLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (file.isImage) "Image ready for analysis" else "File ready (${file.sizeString})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (file.isImage) ElectricCyan else EmeraldLight,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = onClearAttachedFile,
                                modifier = Modifier.size(28.dp).testTag("clear_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    tint = Zinc400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Input pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Zinc850)
                    .border(
                        1.dp,
                        if (isImageMode) ElectricCyan.copy(alpha = 0.5f) else Zinc700,
                        RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Import '+' Button
                IconButton(
                    onClick = onOpenAttachmentSheet,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Zinc800, CircleShape)
                        .testTag("import_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import file or image",
                        tint = if (attachedFile != null) EmeraldLight else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = {
                        Text(
                            when {
                                attachedFile != null -> "Add a prompt or message about this file..."
                                isImageMode -> "Describe the image you want to generate..."
                                else -> "Message OmniChat or tap '+' to import files..."
                            },
                            color = Zinc400,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_textfield"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                // Action button: Send or Stop
                if (isGenerating) {
                    IconButton(
                        onClick = onStopGenerating,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .testTag("stop_generating_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    val canSend = inputText.isNotBlank() || attachedFile != null
                    IconButton(
                        onClick = onSendMessage,
                        enabled = canSend,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (canSend) {
                                    if (isImageMode) ElectricCyan else EmeraldPrimary
                                } else Zinc700,
                                CircleShape
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Zinc400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
