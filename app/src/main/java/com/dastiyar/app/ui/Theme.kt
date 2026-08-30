package com.dastiyar.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dastiyar.app.R

val DastiyarSuccess = Color(0xFF57CB62)
val DastiyarWarning = Color(0xFFFFC857)
val DastiyarDanger = Color(0xFFE15454)

private val Dark = darkColorScheme(
    primary = Color(0xFF66C0F4),
    onPrimary = Color(0xFF082030),
    primaryContainer = Color(0xFF123047),
    onPrimaryContainer = Color(0xFFB8E2FF),
    secondary = Color(0xFF8ECBF8),
    onSecondary = Color(0xFF0B2233),
    background = Color(0xFF0F1216),
    onBackground = Color(0xFFE8ECF0),
    surface = Color(0xFF161C22),
    onSurface = Color(0xFFE8ECF0),
    surfaceVariant = Color(0xFF1E2730),
    onSurfaceVariant = Color(0xFF9AA5B0),
    outline = Color(0xFF2A3542),
    outlineVariant = Color(0xFF232E39),
    tertiary = Color(0xFF94A3B8),
    onTertiary = Color(0xFF0B1219),
    error = Color(0xFFE15454),
    onError = Color(0xFF230B0B),
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFFC9C9)
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val Typography = Typography(
    displaySmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp),
    titleLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 16.sp)
)

@Composable
fun DastiyarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = Dark,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}