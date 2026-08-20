package com.ngaclient.app.ui.gesture

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlin.math.max

/**
 * 子页面统一返回容器。
 *
 * - 系统边缘返回：收集完整手势，仅在用户松手并提交后执行导航；取消时保持当前页。
 * - 内容区右滑：跨过阈值后也只在松手时返回，拖动中仅提供轻量反馈。
 * - 左侧 24dp 留给 Android 系统边缘手势，避免两套识别器争抢。
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentSwipeEnabled: Boolean = enabled,
    customGestureTopInset: Dp = 64.dp,
    content: @Composable () -> Unit
) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            progress.collect { /* 不提前切换页面，等待系统确认提交。 */ }
            onBack()
        } catch (_: CancellationException) {
            // 用户把手势划回去：保留当前页面。
        }
    }

    val density = LocalDensity.current
    val systemEdgeReservePx = with(density) { 24.dp.toPx() }
    val customGestureTopInsetPx = with(density) { customGestureTopInset.toPx() }
    val minimumCommitPx = with(density) { 88.dp.toPx() }
    val maxPreviewPx = with(density) { 56.dp.toPx() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var rawDragPx by remember { mutableFloatStateOf(0f) }
    var previewPx by remember { mutableFloatStateOf(0f) }
    var acceptsGesture by remember { mutableStateOf(false) }

    val progress = (rawDragPx / max(minimumCommitPx, widthPx * 0.18f)).coerceIn(0f, 1f)

    val effectiveContentSwipeEnabled = enabled && contentSwipeEnabled
    val gestureModifier = if (effectiveContentSwipeEnabled) {
        Modifier.pointerInput(effectiveContentSwipeEnabled, widthPx, onBack) {
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        acceptsGesture = effectiveContentSwipeEnabled && start.x >= systemEdgeReservePx &&
                            start.y >= customGestureTopInsetPx
                        rawDragPx = 0f
                        previewPx = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        if (!acceptsGesture) return@detectHorizontalDragGestures
                        val next = (rawDragPx + amount).coerceAtLeast(0f)
                        if (next > 0f || rawDragPx > 0f) {
                            change.consume()
                            rawDragPx = next
                            // 橡皮筋式位移：反馈手势，但不在拖动途中暴露上一层页面。
                            previewPx = (next * 0.24f).coerceAtMost(maxPreviewPx)
                        }
                    },
                    onDragCancel = {
                        acceptsGesture = false
                        rawDragPx = 0f
                        previewPx = 0f
                    },
                    onDragEnd = {
                        val shouldGoBack = acceptsGesture &&
                            rawDragPx >= max(minimumCommitPx, widthPx * 0.18f)
                        acceptsGesture = false
                        rawDragPx = 0f
                        previewPx = 0f
                        if (shouldGoBack) onBack()
                    }
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier.fillMaxSize()
            .onSizeChanged { widthPx = it.width.toFloat() }
            .then(gestureModifier)
    ) {
        val translatedContent = if (effectiveContentSwipeEnabled) {
            Modifier.graphicsLayer { translationX = previewPx }
        } else {
            // AndroidView（尤其 WebView）不应长期包在额外 graphicsLayer 中，
            // 否则滚动时会多一层纹理合成。
            Modifier
        }
        Box(Modifier.fillMaxSize().then(translatedContent)) {
            content()
        }

        if (previewPx > 0f) {
            Box(
                Modifier.align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .graphicsLayer { alpha = 0.35f + progress * 0.65f }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "松手返回",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
