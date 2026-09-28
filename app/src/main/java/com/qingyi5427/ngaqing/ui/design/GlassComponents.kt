package com.qingyi5427.ngaqing.ui.design

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val LocalNgaBackdrop = staticCompositionLocalOf<HazeState?> { null }

/** Connects scrolling content to floating controls without changing their ownership or state. */
@Composable
fun NgaBackdropScope(content: @Composable () -> Unit) {
    val state = remember { HazeState() }
    CompositionLocalProvider(LocalNgaBackdrop provides state, content = content)
}

/** Mark only the content that may appear behind a glass control, never the control itself. */
@Composable
fun Modifier.ngaBackdropSource(): Modifier {
    val state = LocalNgaBackdrop.current
    return if (state != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
        this.haze(state)
    } else this
}

/**
 * A translucent native control. When an ancestor provides a backdrop and the platform can blur
 * it, Haze draws pixels from that source; older devices use a high-contrast solid fallback.
 */
@Composable
fun NgaGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    shadowElevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val state = LocalNgaBackdrop.current
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val nativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 &&
        LocalView.current.isHardwareAccelerated && state != null
    val tint = if (nativeBlur) surface.copy(alpha = 0.42f) else surface.copy(alpha = 0.97f)
    val dark = surface.luminance() < 0.5f
    val rim = if (dark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
    }
    val base = modifier.shadow(shadowElevation, shape, clip = false).clip(shape)
    val effect = if (nativeBlur) {
        base.hazeChild(
            state = state!!,
            style = HazeDefaults.style(
                backgroundColor = surface,
                tint = HazeTint(surface.copy(alpha = 0.24f)),
                blurRadius = 18.dp,
                noiseFactor = 0.03f
            )
        )
    } else base
    CompositionLocalProvider(LocalContentColor provides onSurface) {
        Box(
            modifier = effect.glassFill(tint, rim, shape, dark),
            content = content
        )
    }
}

private fun Modifier.glassFill(color: Color, rim: Color, shape: Shape, dark: Boolean): Modifier =
    this.then(
        Modifier
            .background(color, shape)
            .border(BorderStroke(1.dp, rim), shape)
            .drawWithContent {
                drawContent()
                val highlight = Color.White.copy(alpha = if (dark) 0.20f else 0.86f)
                drawLine(
                    highlight,
                    start = Offset(18.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
    )

/** A minimum 48 dp touch target with its own glass boundary. */
@Composable
fun NgaGlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    NgaGlassSurface(modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
        Box(
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) { icon() }
    }
}
