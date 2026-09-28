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
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val NeutralLight = lightColorScheme(
    primary = Color(0xFF356987), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F0F4), onPrimaryContainer = Color(0xFF244E65),
    secondary = Color(0xFF48636F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF2F1), onSecondaryContainer = Color(0xFF2A424B),
    tertiary = Color(0xFF526B72), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7F0F4), onTertiaryContainer = Color(0xFF244E65),
    background = Color(0xFFFBFBF8), onBackground = Color(0xFF171D21),
    surface = Color(0xFFFBFBF8), onSurface = Color(0xFF171D21),
    surfaceTint = Color(0xFF356987),
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFE9ECEA),
    surfaceVariant = Color(0xFFF4F5F2), onSurfaceVariant = Color(0xFF677078),
    outline = Color(0xFF677078), outlineVariant = Color(0xFFDFE3E1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8F5),
    surfaceContainer = Color(0xFFF4F5F2),
    surfaceContainerHigh = Color(0xFFEBEEEC),
    surfaceContainerHighest = Color(0xFFE3E8E5),
    inverseSurface = Color(0xFF171D21), inverseOnSurface = Color(0xFFFBFBF8),
    inversePrimary = Color(0xFF91C3DD),
    error = Color(0xFFB91C1C), onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0xFF101A1F)
)

private val NeutralDark = darkColorScheme(
    primary = Color(0xFF91C3DD), onPrimary = Color(0xFF123244),
    primaryContainer = Color(0xFF223947), onPrimaryContainer = Color(0xFFC9E7F4),
    secondary = Color(0xFFB4CBD4), onSecondary = Color(0xFF21343D),
    secondaryContainer = Color(0xFF293A42), onSecondaryContainer = Color(0xFFE0EBED),
    tertiary = Color(0xFFB4CBD4), onTertiary = Color(0xFF21343D),
    tertiaryContainer = Color(0xFF293A42), onTertiaryContainer = Color(0xFFE0EBED),
    background = Color(0xFF131B20), onBackground = Color(0xFFEDF2F1),
    surface = Color(0xFF131B20), onSurface = Color(0xFFEDF2F1),
    surfaceTint = Color(0xFF91C3DD),
    surfaceBright = Color(0xFF34434A), surfaceDim = Color(0xFF0D1418),
    surfaceVariant = Color(0xFF1B262C), onSurfaceVariant = Color(0xFFA1AFB5),
    outline = Color(0xFFA1AFB5), outlineVariant = Color(0xFF344148),
    surfaceContainerLowest = Color(0xFF0D1418),
    surfaceContainerLow = Color(0xFF172126),
    surfaceContainer = Color(0xFF1B262C),
    surfaceContainerHigh = Color(0xFF253139),
    surfaceContainerHighest = Color(0xFF2D3B42),
    inverseSurface = Color(0xFFEDF2F1), inverseOnSurface = Color(0xFF171D21),
    inversePrimary = Color(0xFF356987),
    error = Color(0xFFFCA5A5), onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D), onErrorContainer = Color(0xFFFEE2E2),
    scrim = Color.Black
)

val LocalShowSignatures = staticCompositionLocalOf { true }
