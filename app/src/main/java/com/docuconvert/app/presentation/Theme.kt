package com.docuconvert.app.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF006E54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6AE0C1),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4A635B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE7DD),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF396174),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCE6FF),
    onTertiaryContainer = Color(0xFF001E2E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9FDFB),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF9FDFB),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE3DE),
    onSurfaceVariant = Color(0xFF414845),
    outline = Color(0xFF717976),
    outlineVariant = Color(0xFFC1C9C6),
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFF0F4F2),
    inversePrimary = Color(0xFF4FC6A5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC6A5),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF00533E),
    onPrimaryContainer = Color(0xFF6AE0C1),
    secondary = Color(0xFFB0CABF),
    onSecondary = Color(0xFF1F362F),
    secondaryContainer = Color(0xFF344D46),
    onSecondaryContainer = Color(0xFFCCE7DD),
    tertiary = Color(0xFF9FCDEE),
    onTertiary = Color(0xFF071F2E),
    tertiaryContainer = Color(0xFF20485B),
    onTertiaryContainer = Color(0xFFBCE6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE2E6E3),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE2E6E3),
    surfaceVariant = Color(0xFF414845),
    onSurfaceVariant = Color(0xFFC1C9C6),
    outline = Color(0xFF8B9390),
    outlineVariant = Color(0xFF414845),
    inverseSurface = Color(0xFFE2E6E3),
    inverseOnSurface = Color(0xFF2E3130),
    inversePrimary = Color(0xFF006E54)
)

/** App-wide theme entry point used by MainActivity. */
@Composable
fun DocuConvertTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DocuConvertTypography,
        shapes = DocuConvertShapes,
        content = content
    )
}

private val DocuConvertTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

private val DocuConvertShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)