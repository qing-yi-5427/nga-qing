package com.qingyi5427.ngaqing.ui.adaptive

import kotlin.math.min

/** Coordinates are dp relative to the app's current window, never physical screen size. */
data class FoldBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class FoldGeometry(
    val bounds: FoldBounds,
    val vertical: Boolean,
    val separating: Boolean
)

data class PaneLayout(
    val viewportLeft: Float,
    val viewportTop: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val listWidth: Float,
    val gutterWidth: Float,
    val detailWidth: Float,
    val singlePaneLeft: Float,
    val singlePaneWidth: Float
) {
    val isTwoPane: Boolean get() = listWidth > 0f
}

/** Keep one continuous navigation stack while changing only the space allocated to its content. */
fun calculatePaneLayout(width: Float, height: Float, fold: FoldGeometry?): PaneLayout {
    val safeWidth = width.coerceAtLeast(0f)
    val safeHeight = height.coerceAtLeast(0f)
    val horizontalFold = fold?.takeIf {
        it.separating && !it.vertical &&
            it.bounds.top < safeHeight && it.bounds.bottom > 0f &&
            it.bounds.left < safeWidth && it.bounds.right > 0f
    }
    if (horizontalFold != null) {
        val top = horizontalFold.bounds.top.coerceIn(0f, safeHeight)
        val bottom = horizontalFold.bounds.bottom.coerceIn(top, safeHeight)
        val topHeight = top
        val bottomHeight = safeHeight - bottom
        // In tabletop posture all controls and reading text occupy one unobstructed half.
        return if (topHeight >= bottomHeight) {
            PaneLayout(0f, 0f, safeWidth, topHeight, 0f, 0f, safeWidth, 0f, safeWidth)
        } else {
            PaneLayout(0f, bottom, safeWidth, bottomHeight, 0f, 0f, safeWidth, 0f, safeWidth)
        }
    }

    val verticalFold = fold?.takeIf {
        it.separating && it.vertical &&
            it.bounds.left < safeWidth && it.bounds.right > 0f &&
            it.bounds.top < safeHeight && it.bounds.bottom > 0f
    }
    if (verticalFold != null) {
        val left = verticalFold.bounds.left.coerceIn(0f, safeWidth)
        val right = verticalFold.bounds.right.coerceIn(left, safeWidth)
        val leftWidth = left - 8f
        val rightWidth = safeWidth - right - 8f
        val useLeft = left >= safeWidth - right
        val singleLeft = if (useLeft) 0f else right + 8f
        val singleWidth = if (useLeft) leftWidth.coerceAtLeast(0f) else rightWidth.coerceAtLeast(0f)
        if (safeWidth >= 840f && leftWidth >= 280f && rightWidth >= 360f) {
            return PaneLayout(
                0f, 0f, safeWidth, safeHeight,
                leftWidth, safeWidth - leftWidth - rightWidth, rightWidth,
                singleLeft, singleWidth
            )
        }
        // A hinge near an edge or in a narrow window gets one unobstructed pane.
        return PaneLayout(singleLeft, 0f, singleWidth, safeHeight, 0f, 0f, singleWidth, 0f, singleWidth)
    }

    if (safeWidth < 840f) {
        return PaneLayout(0f, 0f, safeWidth, safeHeight, 0f, 0f, safeWidth, 0f, safeWidth)
    }

    val listWidth = min(360f, safeWidth * 0.38f)
    return PaneLayout(0f, 0f, safeWidth, safeHeight, listWidth, 1f, safeWidth - listWidth - 1f, 0f, safeWidth)
}
