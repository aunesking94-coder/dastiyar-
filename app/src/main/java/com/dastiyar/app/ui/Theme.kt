package com.dastiyar.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFFF3B30),
    onPrimary = Color(0xFFFFF1EF),
    primaryContainer = Color(0xFF4A1210),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color(0xFF290806),
    background = Color(0xFF070708),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF101012),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF1C1516),
    onSurfaceVariant = Color(0xFFD0C1C1),
    outline = Color(0xFF3B2B2B),
    outlineVariant = Color(0xFF241A1B),
    error = Color(0xFFFF5252),
    onError = Color(0xFF250505),
    errorContainer = Color(0xFF57120F),
    onErrorContainer = Color(0xFFFFDAD5)
)

@Composable
fun DastiyarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}