package com.dastiyar.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF062030),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF042110),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE7ECF3),
    surface = Color(0xFF1A1F29),
    onSurface = Color(0xFFE7ECF3),
    surfaceVariant = Color(0xFF242B38),
    onSurfaceVariant = Color(0xFFB7C2D1),
    outline = Color(0xFF3A4557),
    error = Color(0xFFF87171)
)

@Composable
fun DastiyarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}