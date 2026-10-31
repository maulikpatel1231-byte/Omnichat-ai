package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    tertiary = PurpleAccent,
    background = Zinc900,
    onBackground = Zinc100,
    surface = Zinc850,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc300,
    outline = Zinc700,
    outlineVariant = Zinc600
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = ElectricCyan,
    onSecondary = Color.White,
    tertiary = PurpleAccent,
    background = Color(0xFFFFFFFF),
    onBackground = Zinc900,
    surface = Zinc100,
    onSurface = Zinc900,
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Zinc700,
    outline = Zinc300,
    outlineVariant = Zinc400
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
