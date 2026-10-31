package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class AttachedFile(
    val uri: Uri,
    val name: String,
    val sizeString: String,
    val mimeType: String,
    val isImage: Boolean,
    val textContent: String? = null,
    val base64Data: String? = null,
    val localCachedPath: String? = null
)

object FileUtils {
    suspend fun processUri(context: Context, uri: Uri): AttachedFile = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var name = "attachment"
        var sizeBytes = 0L

        // Query metadata
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: "attachment"
                }
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(name)
        val isImage = mimeType.startsWith("image/") || isImageExtension(name)

        val formattedSize = formatFileSize(sizeBytes)

        var textContent: String? = null
        var base64Data: String? = null
        var localCachedPath: String? = null

        try {
            // Save local copy to app internal cache for permanent viewing in chat history
            val cacheDir = File(context.cacheDir, "attachments").apply { mkdirs() }
            val cachedFile = File(cacheDir, "${System.currentTimeMillis()}_$name")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cachedFile).use { output ->
                    input.copyTo(output)
                }
            }
            localCachedPath = cachedFile.absolutePath

            if (isImage) {
                // Read image and compress/convert to base64 for Gemini vision
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        // Resize if too large to save memory & bandwidth
                        val scaled = if (bitmap.width > 1280 || bitmap.height > 1280) {
                            val ratio = minOf(1280f / bitmap.width, 1280f / bitmap.height)
                            Bitmap.createScaledBitmap(
                                bitmap,
                                (bitmap.width * ratio).toInt(),
                                (bitmap.height * ratio).toInt(),
                                true
                            )
                        } else bitmap

                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val bytes = outputStream.toByteArray()
                        base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } else {
                // Read text/code content
                contentResolver.openInputStream(uri)?.use { stream ->
                    // Limit text reading to 100KB to stay fast
                    val reader = stream.bufferedReader()
                    val sb = StringBuilder()
                    var charsRead = 0
                    val buffer = CharArray(1024)
                    while (charsRead < 100_000) {
                        val read = reader.read(buffer, 0, buffer.size)
                        if (read == -1) break
                        sb.append(buffer, 0, read)
                        charsRead += read
                    }
                    if (charsRead >= 100_000) {
                        sb.append("\n...[File truncated due to size limit]")
                    }
                    textContent = sb.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AttachedFile(
            uri = uri,
            name = name,
            sizeString = formattedSize,
            mimeType = mimeType,
            isImage = isImage,
            textContent = textContent,
            base64Data = base64Data,
            localCachedPath = localCachedPath
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> ""
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024f * 1024f))
        }
    }

    private fun isImageExtension(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".gif") || lower.endsWith(".bmp")
    }

    private fun getMimeTypeFromExtension(filename: String): String {
        val lower = filename.lowercase()
        return when {
            isImageExtension(lower) -> "image/jpeg"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".py") -> "text/x-python"
            lower.endsWith(".kt") || lower.endsWith(".java") -> "text/x-java-source"
            lower.endsWith(".js") || lower.endsWith(".ts") -> "application/javascript"
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".md") -> "text/markdown"
            else -> "application/octet-stream"
        }
    }
}
