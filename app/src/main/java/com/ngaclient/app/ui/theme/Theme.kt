package com.ngaclient.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 「暖纸面」设计系统：降低装饰噪声，让标题、正文和讨论关系成为视觉主体。 */
@Composable
fun NgaQingTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val palette = if (darkTheme) DarkPaper else LightPaper
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) paperDark else paperLight,
            typography = NgaTypography,
            shapes = shapes,
            content = content
        )
    }
}

private val paperLight = lightColorScheme(
    primary = Color(0xFF8A5100), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB5), onPrimaryContainer = Color(0xFF2C1700),
    secondary = Color(0xFF705B41), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBDDBD), onSecondaryContainer = Color(0xFF281805),
    tertiary = Color(0xFF53643F), onTertiary = Color.White,
    background = Color(0xFFFFFBF7), onBackground = Color(0xFF201A16),
    surface = Color(0xFFFFFBF7), onSurface = Color(0xFF201A16),
    surfaceVariant = Color(0xFFF3EDE6), onSurfaceVariant = Color(0xFF51463D),
    outline = Color(0xFF837469), outlineVariant = Color(0xFFD6C5B9),
    error = Color(0xFFBA1A1A), onError = Color.White
)

private val paperDark = darkColorScheme(
    primary = Color(0xFFFFB95F), onPrimary = Color(0xFF492900),
    primaryContainer = Color(0xFF683D00), onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFFDEC2A2), onSecondary = Color(0xFF3E2D17),
    secondaryContainer = Color(0xFF57432B), onSecondaryContainer = Color(0xFFFBDDBD),
    tertiary = Color(0xFFBACDA2), onTertiary = Color(0xFF263514),
    background = Color(0xFF17130F), onBackground = Color(0xFFF2E9E1),
    surface = Color(0xFF17130F), onSurface = Color(0xFFF2E9E1),
    surfaceVariant = Color(0xFF2B2520), onSurfaceVariant = Color(0xFFD6C5B9),
    outline = Color(0xFF9F8D80), outlineVariant = Color(0xFF51463D),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

/** 兼容旧组件名的扩展语义色，七个版块颜色现已收敛为同一品牌色。 */
data class GlassPalette(
    val boardTeal: Color, val boardPink: Color, val boardOrange: Color,
    val boardPurple: Color, val boardBlue: Color, val boardGreen: Color,
    val boardCoral: Color, val unread: Color, val hotBg: Color, val hotFg: Color,
    val rowDivider: Color, val quoteBg: Color, val glassBar: Color,
    val glassBarBorder: Color, val pillNavBg: Color, val pillNavBorder: Color
)

private val LightPaper = GlassPalette(
    boardTeal = Color(0xFF8A5100), boardPink = Color(0xFF8A5100),
    boardOrange = Color(0xFF8A5100), boardPurple = Color(0xFF8A5100),
    boardBlue = Color(0xFF8A5100), boardGreen = Color(0xFF8A5100),
    boardCoral = Color(0xFF8A5100), unread = Color(0xFFBA1A1A),
    hotBg = Color(0xFFFFE4C5), hotFg = Color(0xFF7A4300),
    rowDivider = Color(0xFFE7D8CD), quoteBg = Color(0xFFF3EDE6),
    glassBar = Color(0xFFFFFBF7), glassBarBorder = Color(0xFFE7D8CD),
    pillNavBg = Color(0xFFFFF8F2), pillNavBorder = Color(0xFFE7D8CD)
)

private val DarkPaper = GlassPalette(
    boardTeal = Color(0xFFFFB95F), boardPink = Color(0xFFFFB95F),
    boardOrange = Color(0xFFFFB95F), boardPurple = Color(0xFFFFB95F),
    boardBlue = Color(0xFFFFB95F), boardGreen = Color(0xFFFFB95F),
    boardCoral = Color(0xFFFFB95F), unread = Color(0xFFFFB4AB),
    hotBg = Color(0xFF432C12), hotFg = Color(0xFFFFB95F),
    rowDivider = Color(0xFF3C332C), quoteBg = Color(0xFF2B2520),
    glassBar = Color(0xFF17130F), glassBarBorder = Color(0xFF3C332C),
    pillNavBg = Color(0xFF211C17), pillNavBorder = Color(0xFF3C332C)
)

val LocalGlassPalette = staticCompositionLocalOf { LightPaper }
