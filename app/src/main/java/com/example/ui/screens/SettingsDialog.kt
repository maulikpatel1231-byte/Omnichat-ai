package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.ImageGenerator
import com.example.data.remote.TextAiService
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc850

@Composable
fun SettingsDialog(
    currentTextModel: String,
    currentImageModel: String,
    onSaveSettings: (textModel: String, imageModel: String, customApiKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnichat_prefs", Context.MODE_PRIVATE) }

    var selectedTextModel by remember { mutableStateOf(currentTextModel) }
    var selectedImageModel by remember { mutableStateOf(currentImageModel) }
    var customApiKey by remember { mutableStateOf(prefs.getString("custom_api_key", "").orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Zinc850,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings & AI Models",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Zinc400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Zinc800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% Free & Open-Source",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "OmniChat connects directly to open weights (Flux, Llama 3.3, Mistral, Qwen, DeepSeek). Unlimited image generation with no restrictive filters.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Zinc400,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Text Model Picker
                Text(
                    text = "Default Text Model",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextAiService.AVAILABLE_MODELS.forEach { model ->
                        val isSelected = model.id == selectedTextModel
                        Surface(
                            onClick = { selectedTextModel = model.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else Zinc800,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) EmeraldLight else Color.White
                                        )
                                    )
                                    Text(
                                        text = model.provider,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Zinc400)
                                    )
                                }
                                if (isSelected) {
                                    Text("Active", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldLight, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Image Generator Model
                Text(
                    text = "Image Generator Model",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ImageGenerator.AVAILABLE_MODELS.forEach { model ->
                        val isSelected = model.id == selectedImageModel
                        Surface(
                            onClick = { selectedImageModel = model.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else Zinc800,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) ElectricCyan else Color.White
                                        )
                                    )
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Zinc400)
                                    )
                                }
                                if (isSelected) {
                                    Text("Active", style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional Custom API Key (for Gemini)
                Text(
                    text = "Optional API Key (for Gemini / Private Endpoints)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    placeholder = { Text("Leave blank to use free open-source models", color = Zinc400, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Zinc400) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_api_key_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Zinc700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Zinc800,
                        unfocusedContainerColor = Zinc800
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 100% On-Device Privacy & Security Card
                val vaultStats = remember { com.example.util.FileUtils.getPrivateVaultStats(context) }
                var vaultFileCount by remember { mutableStateOf(vaultStats.first) }
                var vaultSizeBytes by remember { mutableStateOf(vaultStats.second) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Zinc800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                contentDescription = "Privacy Shield",
                                tint = EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% On-Device Private Vault",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aapki uploaded files, photos aur chats sirf aapke is phone ke private internal sandbox me surakshit rehte hain. Koi aur user ya app ise nahi dekh sakta.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Zinc400,
                                lineHeight = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Files in Vault: $vaultFileCount (${vaultSizeBytes / 1024} KB)",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                            )
                            if (vaultFileCount > 0) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        com.example.util.FileUtils.clearPrivateVault(context)
                                        vaultFileCount = 0
                                        vaultSizeBytes = 0L
                                        android.widget.Toast.makeText(context, "Private vault cleared!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Clear Vault", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        prefs.edit().putString("custom_api_key", customApiKey).apply()
                        onSaveSettings(selectedTextModel, selectedImageModel, customApiKey)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
