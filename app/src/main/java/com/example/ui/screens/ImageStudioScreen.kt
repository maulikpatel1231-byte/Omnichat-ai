package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.GeneratedImageEntity
import com.example.data.remote.ImageGenerator
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.saveImageToGallery
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc850
import com.example.ui.theme.Zinc900
import kotlinx.coroutines.launch
import kotlin.random.Random

val INSPIRATION_PROMPTS = listOf(
    "Futuristic cyberpunk city at rainy midnight with neon reflections and flying cars",
    "Bioluminescent enchanted forest with glowing mystical creatures and giant glowing mushrooms",
    "High fashion robotic android portrait with golden intricate filigree and crystal eyes",
    "A majestic snow leopard resting on Himalayan mountain peak during dramatic golden hour",
    "Cozy anime coffee shop in Tokyo with rain pouring outside the panoramic window",
    "Mythological warrior wielding a sword of pure electric lightning on an alien planet",
    "Cute miniature astronaut exploring a miniature fantasy bonsai tree planet, 3D render"
)

data class AspectRatioOption(val label: String, val ratio: Float, val width: Int, val height: Int)

val ASPECT_RATIOS = listOf(
    AspectRatioOption("1:1 Square", 1f, 1024, 1024),
    AspectRatioOption("16:9 Landscape", 16f / 9f, 1280, 720),
    AspectRatioOption("9:16 Portrait", 9f / 16f, 720, 1280),
    AspectRatioOption("4:3 Photo", 4f / 3f, 1024, 768)
)

@Composable
fun ImageStudioScreen(
    currentImageModel: String,
    galleryImages: List<GeneratedImageEntity>,
    onBack: () -> Unit,
    onGenerate: (prompt: String, styleId: String, width: Int, height: Int) -> Unit,
    onToggleFavorite: (GeneratedImageEntity) -> Unit,
    onEnhancePrompt: suspend (String) -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    var selectedStyleId by remember { mutableStateOf("none") }
    var selectedRatio by remember { mutableStateOf(ASPECT_RATIOS[0]) }
    var isEnhancing by remember { mutableStateOf(false) }

    // Dialog preview
    var previewImage by remember { mutableStateOf<GeneratedImageEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc900)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_from_studio_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Chat",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Image Studio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Unlimited • Free • Open-Source ($currentImageModel)",
                    style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ElectricCyan.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FLUX AI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Main Studio Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Prompt input card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Zinc850
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Describe your image",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )

                        // Dice button for random prompt
                        IconButton(
                            onClick = {
                                prompt = INSPIRATION_PROMPTS.random()
                            },
                            modifier = Modifier.size(32.dp).testTag("random_prompt_button")
                        ) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = "Random prompt",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = {
                            Text(
                                "e.g. Majestic dragon flying over snowy mountain castle in sunset...",
                                color = Zinc400,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("studio_prompt_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Zinc700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Zinc800,
                            unfocusedContainerColor = Zinc800
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Enhance Prompt Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = {
                                if (prompt.isNotBlank() && !isEnhancing) {
                                    isEnhancing = true
                                    scope.launch {
                                        val enhanced = onEnhancePrompt(prompt)
                                        prompt = enhanced
                                        isEnhancing = false
                                        Toast.makeText(context, "Prompt Enhanced!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Zinc800,
                            modifier = Modifier.testTag("enhance_prompt_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEnhancing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldLight
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnhancing) "Enhancing..." else "✨ Enhance Prompt",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldLight
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Style Presets Carousel
            Text(
                text = "Artistic Style",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageGenerator.STYLE_PRESETS.forEach { style ->
                    val isSelected = style.id == selectedStyleId
                    Surface(
                        onClick = { selectedStyleId = style.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricCyan else Zinc800,
                        modifier = Modifier.testTag("style_chip_${style.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = style.iconEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = style.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio Selector
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ASPECT_RATIOS.forEach { ratio ->
                    val isSelected = ratio.label == selectedRatio.label
                    Surface(
                        onClick = { selectedRatio = ratio },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ratio_${ratio.label.take(4)}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else Zinc800
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio.label.split(" ").first(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Zinc400
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Generate Button
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        onGenerate(prompt, selectedStyleId, selectedRatio.width, selectedRatio.height)
                        Toast.makeText(context, "Generating your image with Flux...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Please enter a prompt first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("studio_generate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Image with Flux",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gallery of Recent Creations
            Text(
                text = "Your Creation Gallery (${galleryImages.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (galleryImages.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Zinc850
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No generated images yet.\nEnter a prompt above and tap Generate!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Zinc400,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    galleryImages.chunked(2).forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowImages.forEach { image ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Zinc800)
                                        .clickable { previewImage = image }
                                ) {
                                    SubcomposeAsyncImage(
                                        model = image.imageUrl,
                                        contentDescription = image.prompt,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = ElectricCyan,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    )

                                    // Floating Favorite Button
                                    IconButton(
                                        onClick = { onToggleFavorite(image) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (image.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (image.isFavorite) Color.Red else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (rowImages.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Fullscreen Preview Dialog
    previewImage?.let { img ->
        ImagePreviewDialog(
            imageUrl = img.imageUrl,
            prompt = img.prompt,
            modelName = img.model,
            onDismiss = { previewImage = null }
        )
    }
}
