package com.omegaapi.huesort.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF1B102F),
    secondary = Color(0xFFFFB86C),
    onSecondary = Color(0xFF2A1600),
    surface = Color(0xFF14101F),
    onSurface = Color(0xFFF7F2FF),
    surfaceVariant = Color(0xFF272038),
    onSurfaceVariant = Color(0xFFCFC4E5),
    outline = Color(0xFF5C5275)
)

@Composable
fun HueSortTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
