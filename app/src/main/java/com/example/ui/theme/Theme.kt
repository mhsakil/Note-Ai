package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RealBlue500,
    secondary = RealBlue600,
    tertiary = RealEmerald,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextLightCrisp,
    onSurface = TextLightCrisp,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedLight
)

private val LightColorScheme = lightColorScheme(
    primary = RealBlue600,
    secondary = RealBlue500,
    tertiary = RealEmerald,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = TextDarkSlate,
    onTertiary = Color.White,
    onBackground = TextDarkSlate,
    onSurface = TextDarkSlate,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We default to the gorgeous dark slate theme to deliver the high-impact Notion/Obsidian look
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
