package com.example.data.remote

import java.net.URLEncoder
import kotlin.random.Random

data class ImageModelInfo(
    val id: String,
    val displayName: String,
    val description: String
)

data class StylePreset(
    val id: String,
    val name: String,
    val promptSuffix: String,
    val iconEmoji: String
)

object ImageGenerator {
    val AVAILABLE_MODELS = listOf(
        ImageModelInfo("flux", "Flux Schnell (Open-Source)", "State-of-the-art fast open-source image generation"),
        ImageModelInfo("flux-realism", "Flux Realism", "Hyper-detailed lifelike realism and textures"),
        ImageModelInfo("flux-anime", "Flux Anime", "Vibrant modern Japanese anime & manga artwork"),
        ImageModelInfo("flux-3d", "Flux 3D", "Stylized 3D character & scene octane renders"),
        ImageModelInfo("turbo", "SDXL Turbo", "Ultra-fast Stable Diffusion XL generation")
    )

    val STYLE_PRESETS = listOf(
        StylePreset("none", "None", "", "✨"),
        StylePreset("photoreal", "Photoreal", ", 8k UHD, hyperrealistic, high resolution DSLR photography, studio lighting, sharp focus", "📸"),
        StylePreset("anime", "Anime", ", high quality anime style, makoto shinkai aesthetic, vibrant colors, detailed illustration", "🎨"),
        StylePreset("cyberpunk", "Cyberpunk", ", cyberpunk neon lighting, futuristic sci-fi city, rainy reflections, moody atmospheric bloom", "🌆"),
        StylePreset("cinematic", "Cinematic", ", cinematic movie still, 35mm lens, dramatic color grading, masterwork cinematography", "🎬"),
        StylePreset("3d", "3D Pixar", ", cute 3D render, Pixar style, subsurface scattering, Octane render, smooth textures", "🧸"),
        StylePreset("fantasy", "Fantasy", ", epic fantasy concept art, mystical glowing lighting, intricate details, artstation trending", "🐉"),
        StylePreset("oil", "Oil Painting", ", master oil painting, textured brush strokes, classical fine art composition", "🖌️")
    )

    fun buildImageUrl(
        prompt: String,
        model: String = "flux",
        styleId: String = "none",
        width: Int = 1024,
        height: Int = 1024,
        seed: Long? = null,
        nologo: Boolean = true
    ): String {
        val selectedStyle = STYLE_PRESETS.firstOrNull { it.id == styleId }
        val fullPrompt = if (selectedStyle != null && selectedStyle.promptSuffix.isNotEmpty()) {
            prompt.trim() + selectedStyle.promptSuffix
        } else {
            prompt.trim()
        }

        val effectiveSeed = seed ?: Random.nextLong(1, 999999999)
        val encodedPrompt = URLEncoder.encode(fullPrompt, "UTF-8")
        return "https://image.pollinations.ai/prompt/$encodedPrompt?model=$model&width=$width&height=$height&seed=$effectiveSeed&nologo=$nologo"
    }

    fun isImagePrompt(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower.startsWith("/image") ||
                lower.startsWith("/imagine") ||
                lower.startsWith("generate an image") ||
                lower.startsWith("generate image") ||
                lower.startsWith("create an image") ||
                lower.startsWith("draw ") ||
                lower.startsWith("paint ")
    }

    fun extractCleanImagePrompt(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("/image ") -> trimmed.substring(7).trim()
            lower.startsWith("/imagine ") -> trimmed.substring(9).trim()
            lower.startsWith("generate an image of ") -> trimmed.substring(21).trim()
            lower.startsWith("generate image of ") -> trimmed.substring(18).trim()
            lower.startsWith("generate an image: ") -> trimmed.substring(19).trim()
            lower.startsWith("generate image: ") -> trimmed.substring(16).trim()
            lower.startsWith("create an image of ") -> trimmed.substring(19).trim()
            lower.startsWith("draw ") -> trimmed.substring(5).trim()
            lower.startsWith("paint ") -> trimmed.substring(6).trim()
            else -> trimmed
        }
    }
}
