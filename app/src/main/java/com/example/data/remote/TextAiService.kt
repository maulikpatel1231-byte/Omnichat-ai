package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TextModelOption(
    val id: String,
    val displayName: String,
    val provider: String,
    val description: String,
    val isDefault: Boolean = false
)

object TextAiService {
    private const val TAG = "TextAiService"

    val AVAILABLE_MODELS = listOf(
        TextModelOption(
            id = "llama",
            displayName = "Llama 3.3 70B",
            provider = "Meta Open-Source",
            description = "Meta's flagship open-source LLM, fast & comprehensive",
            isDefault = true
        ),
        TextModelOption(
            id = "mistral",
            displayName = "Mistral Nemo",
            provider = "Mistral AI",
            description = "Efficient, precise open reasoning model"
        ),
        TextModelOption(
            id = "deepseek",
            displayName = "DeepSeek R1 / V3",
            provider = "DeepSeek Open-Source",
            description = "Advanced reasoning, math, and code capabilities"
        ),
        TextModelOption(
            id = "qwen",
            displayName = "Qwen 2.5 72B",
            provider = "Alibaba Open-Source",
            description = "Multilingual powerhouse with great Hindi & English skills"
        ),
        TextModelOption(
            id = "gemini",
            displayName = "Gemini 1.5/2.0 Flash",
            provider = "Google AI",
            description = "Google's fast multimodal model with huge context window"
        )
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT =
        "You are OmniChat AI, an exceptionally smart, friendly, and helpful assistant designed like ChatGPT. " +
                "You are powered by open-source AI models and can generate unlimited images without restrictions. " +
                "Provide detailed, structured responses formatted with clear Markdown headers, bold highlights, bullet points, and code blocks with language tags when relevant. " +
                "You understand and can converse fluently in English, Hindi, Hinglish, and other languages. " +
                "If the user asks to generate, draw, or create an image, explain what image prompt you recommend and remind them that they can also tap the 'Image Mode' button or type '/image <prompt>' to generate it immediately."

    suspend fun sendMessage(
        conversationHistory: List<Pair<String, String>>, // list of Pair(role, content)
        userMessage: String,
        modelId: String,
        customApiKey: String? = null,
        attachedFileName: String? = null,
        attachedFileText: String? = null,
        attachedImageBase64: String? = null,
        attachedImageMimeType: String? = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        val effectiveMessage = buildString {
            if (!attachedFileName.isNullOrBlank()) {
                if (!attachedFileText.isNullOrBlank()) {
                    append("Attached File: ")
                    append(attachedFileName)
                    append("\n```\n")
                    append(attachedFileText)
                    append("\n```\n\n")
                } else if (!attachedImageBase64.isNullOrBlank()) {
                    append("[User attached image: ")
                    append(attachedFileName)
                    append("]\n\n")
                }
            }
            append(userMessage)
        }

        if (modelId == "gemini" || !attachedImageBase64.isNullOrBlank()) {
            try {
                return@withContext callGemini(
                    conversationHistory = conversationHistory,
                    userMessage = effectiveMessage,
                    customApiKey = customApiKey,
                    attachedImageBase64 = attachedImageBase64,
                    attachedImageMimeType = attachedImageMimeType
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gemini failed, falling back to open-source model: ${e.message}")
            }
        }

        // Call Pollinations open-source endpoint
        try {
            return@withContext callPollinations(conversationHistory, effectiveMessage, modelId)
        } catch (e: Exception) {
            Log.e(TAG, "Pollinations API error: ${e.message}")
            // If pollinations failed and we have gemini key, try Gemini fallback
            val geminiKey = customApiKey?.takeIf { it.isNotBlank() }
                ?: (try { BuildConfig.GEMINI_API_KEY.takeIf { it != "MY_GEMINI_API_KEY" && it.isNotBlank() } ?: "" } catch (_: Exception) { "" })
            if (geminiKey.isNotBlank()) {
                try {
                    return@withContext callGemini(
                        conversationHistory = conversationHistory,
                        userMessage = effectiveMessage,
                        customApiKey = geminiKey,
                        attachedImageBase64 = attachedImageBase64,
                        attachedImageMimeType = attachedImageMimeType
                    )
                } catch (ge: Exception) {
                    Log.e(TAG, "Gemini fallback also failed: ${ge.message}")
                }
            }

            // Return a helpful fallback response
            return@withContext "I received your request with the attached file. However, there was a connection timeout with the open-source inference servers. Please check your network and try again."
        }
    }

    private fun callPollinations(
        conversationHistory: List<Pair<String, String>>,
        userMessage: String,
        modelId: String
    ): String {
        val targetModel = when (modelId) {
            "llama" -> "openai" // Pollinations maps openai route or standard fast route
            "mistral" -> "mistral"
            "deepseek" -> "deepseek"
            "qwen" -> "qwen"
            else -> "openai"
        }

        val messagesArray = JSONArray()
        // System message
        val sysObj = JSONObject()
        sysObj.put("role", "system")
        sysObj.put("content", SYSTEM_PROMPT)
        messagesArray.put(sysObj)

        // Previous context (last 6 messages to stay lightweight)
        val recentHistory = conversationHistory.takeLast(6)
        for ((role, content) in recentHistory) {
            val msgObj = JSONObject()
            msgObj.put("role", if (role == "user") "user" else "assistant")
            msgObj.put("content", content)
            messagesArray.put(msgObj)
        }

        // Current message
        val currentObj = JSONObject()
        currentObj.put("role", "user")
        currentObj.put("content", userMessage)
        messagesArray.put(currentObj)

        val jsonBody = JSONObject()
        jsonBody.put("messages", messagesArray)
        jsonBody.put("model", targetModel)
        jsonBody.put("jsonMode", false)

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://text.pollinations.ai/")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        // If response is the 45-second rate limit message or error
        if (responseBody.contains("Free limit reached") || responseBody.contains("pollen key")) {
            // Attempt Gemini or retry with alternative model parameter
            throw Exception("Pollinations rate limit reached")
        }

        return responseBody.trim()
    }

    private fun callGemini(
        conversationHistory: List<Pair<String, String>>,
        userMessage: String,
        customApiKey: String?,
        attachedImageBase64: String? = null,
        attachedImageMimeType: String? = "image/jpeg"
    ): String {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig.GEMINI_API_KEY.takeIf { it != "MY_GEMINI_API_KEY" && it.isNotBlank() } ?: "" } catch (_: Exception) { "" })

        if (apiKey.isBlank()) {
            throw Exception("Gemini API key is not configured. Please use open-source models (Llama, Mistral, Qwen).")
        }

        val contentsArray = JSONArray()
        val recentHistory = conversationHistory.takeLast(6)
        for ((role, content) in recentHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", if (role == "user") "user" else "model")
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", content)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        // Add current user message
        val currentObj = JSONObject()
        currentObj.put("role", "user")
        val partsArray = JSONArray()

        if (!attachedImageBase64.isNullOrBlank()) {
            val imgPart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mime_type", attachedImageMimeType ?: "image/jpeg")
            inlineData.put("data", attachedImageBase64)
            imgPart.put("inline_data", inlineData)
            partsArray.put(imgPart)
        }

        val partObj = JSONObject()
        partObj.put("text", "$SYSTEM_PROMPT\n\nUser Question: $userMessage")
        partsArray.put(partObj)
        currentObj.put("parts", partsArray)
        contentsArray.put(currentObj)

        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("Gemini API error ${response.code}: $responseString")
        }

        val json = JSONObject(responseString)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                textBuilder.append(parts.getJSONObject(i).optString("text"))
            }
            return textBuilder.toString().trim()
        }

        throw Exception("No response candidate returned by Gemini.")
    }

    suspend fun enhanceImagePrompt(rawPrompt: String): String = withContext(Dispatchers.IO) {
        val enhanceRequest = "You are an expert AI image prompt engineer. " +
                "Convert this short idea into a rich, detailed, vivid image generation prompt (max 40 words). " +
                "Output ONLY the prompt text without quotes or explanations.\nIdea: $rawPrompt"
        try {
            val response = sendMessage(emptyList(), enhanceRequest, "llama")
            if (response.isNotBlank() && !response.contains("network timeout")) {
                return@withContext response.trim().removeSurrounding("\"")
            }
        } catch (_: Exception) {}

        // Fallback enhancement if offline
        return@withContext "$rawPrompt, ultra high definition, intricate details, masterwork aesthetic, cinematic lighting, 8k resolution"
    }
}
