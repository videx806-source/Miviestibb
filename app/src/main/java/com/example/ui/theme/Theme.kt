package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C3AED), // Purple style accent (#7c3aed in old theme)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = Color(0xFFEF4444), // Crimson/Red for highlights
    onSecondary = Color.White,
    background = Color(0xFF0C0C12), // Deeper gorgeous modern black
    surface = Color(0xFF161622), // Dark card surface
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF222233),
    onSurfaceVariant = Color(0xFF9CA3AF)
)

@Composable
fun VidexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
