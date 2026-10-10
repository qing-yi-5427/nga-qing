package com.qingyi5427.ngaqing.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reading-paper surfaces and muted blue controls from the approved native design proposal.
 * Layout and interactions remain native Compose Material 3.
 */
@Composable
fun NgaQingTheme(
    themeMode: String = "system",
    readingTextScale: Float = 1f,
    readingLineSpacing: Float = 1f,
    showSignatures: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val shapes = remember { NgaShapes }
    val typography = remember(readingTextScale, readingLineSpacing) {
        NgaTypography.copy(
            bodyLarge = NgaTypography.bodyLarge.copy(
                fontSize = NgaTypography.bodyLarge.fontSize * readingTextScale,
                lineHeight = NgaTypography.bodyLarge.lineHeight * readingTextScale * readingLineSpacing
            ),
            bodyMedium = NgaTypography.bodyMedium.copy(
                fontSize = NgaTypography.bodyMedium.fontSize * readingTextScale,
                lineHeight = NgaTypography.bodyMedium.lineHeight * readingTextScale * readingLineSpacing
            )
        )
    }
    CompositionLocalProvider(LocalShowSignatures provides showSignatures) {
        MaterialTheme(
            colorScheme = if (darkTheme) NeutralDark else NeutralLight,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

private val NgaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

private val NeutralLight = lightColorScheme(
    primary = Color(0xFF087EFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF3FF), onPrimaryContainer = Color(0xFF0759AF),
    secondary = Color(0xFF48636F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF2F1), onSecondaryContainer = Color(0xFF2A424B),
    tertiary = Color(0xFF526B72), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7F0F4), onTertiaryContainer = Color(0xFF244E65),
    background = Color(0xFFF8F9FB), onBackground = Color(0xFF20232A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF20232A),
    surfaceTint = Color(0xFF087EFF),
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFF1F3F6),
    surfaceVariant = Color(0xFFF1F3F6), onSurfaceVariant = Color(0xFF737B88),
    outline = Color(0xFFA1A7B1), outlineVariant = Color(0xFFE9EBF0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = Color(0xFFF4F5F8),
    surfaceContainerHigh = Color(0xFFEAF3FF),
    surfaceContainerHighest = Color(0xFFE3EAF4),
    inverseSurface = Color(0xFF20232A), inverseOnSurface = Color(0xFFF8F9FB),
    inversePrimary = Color(0xFF64A7FF),
    error = Color(0xFFB91C1C), onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0xFF101A1F)
)

private val NeutralDark = darkColorScheme(
    primary = Color(0xFF64A7FF), onPrimary = Color(0xFF102B4C),
    primaryContainer = Color(0xFF18304C), onPrimaryContainer = Color(0xFFDCEAFF),
    secondary = Color(0xFFB4CBD4), onSecondary = Color(0xFF21343D),
    secondaryContainer = Color(0xFF293A42), onSecondaryContainer = Color(0xFFE0EBED),
    tertiary = Color(0xFFB4CBD4), onTertiary = Color(0xFF21343D),
    tertiaryContainer = Color(0xFF293A42), onTertiaryContainer = Color(0xFFE0EBED),
    background = Color(0xFF0E1014), onBackground = Color(0xFFF2F3F7),
    surface = Color(0xFF191B20), onSurface = Color(0xFFF2F3F7),
    surfaceTint = Color(0xFF64A7FF),
    surfaceBright = Color(0xFF30333B), surfaceDim = Color(0xFF0E1014),
    surfaceVariant = Color(0xFF272A31), onSurfaceVariant = Color(0xFFA6ACB6),
    outline = Color(0xFF7C8390), outlineVariant = Color(0xFF30333B),
    surfaceContainerLowest = Color(0xFF0E1014),
    surfaceContainerLow = Color(0xFF14161B),
    surfaceContainer = Color(0xFF191B20),
    surfaceContainerHigh = Color(0xFF272A31),
    surfaceContainerHighest = Color(0xFF30333B),
    inverseSurface = Color(0xFFF2F3F7), inverseOnSurface = Color(0xFF20232A),
    inversePrimary = Color(0xFF087EFF),
    error = Color(0xFFFCA5A5), onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D), onErrorContainer = Color(0xFFFEE2E2),
    scrim = Color.Black
)

val LocalShowSignatures = staticCompositionLocalOf { true }
