package com.qingyi5427.ngaqing.ui.adaptive

import androidx.compose.runtime.staticCompositionLocalOf

/** Decisions use the current window's usable dp, never a device name or physical pixels. */
data class V8LayoutPolicy(
    val availableWidthDp: Float = 0f,
    val availableHeightDp: Float = 0f,
    val homeTwoRegions: Boolean = false,
    val readerTwoPane: Boolean = false,
    val favoriteColumns: Int = 1,
    val compactHeight: Boolean = false,
    val mediumComfortTypography: Boolean = false
)

val LocalV8LayoutPolicy = staticCompositionLocalOf { V8LayoutPolicy() }

/** The fold-safe pane geometry is calculated first and supplied to every screen. */
fun decideV8LayoutPolicy(
    widthDp: Float,
    heightDp: Float,
    fontScale: Float,
    fold: FoldGeometry? = null,
    contentHeightDp: Float = heightDp,
    contentWidthDp: Float = widthDp
): V8LayoutPolicy {
    val panes = calculatePaneLayout(widthDp, heightDp, fold, fontScale,
        contentHeight = contentHeightDp, contentWidth = contentWidthDp)
    // Root destinations render on one safe side of a separating vertical hinge,
    // even when the reader can use both sides as its list and article panes.
    val usableWidth = minOf(if (fold?.vertical == true && fold.separating)
        panes.singlePaneWidth else if (panes.isTwoPane) panes.viewportWidth else panes.singlePaneWidth,
        contentWidthDp)
    val usableHeight = minOf(panes.viewportHeight, contentHeightDp)
    // Large accessibility fonts reduce the effective number of readable rows.
    val compact = usableHeight / fontScale.coerceAtLeast(1f) < 480f
    val largeText = fontScale >= 1.3f
    val homeMin = if (largeText) 360f else 300f
    // Discover uses 34dp outer gutters, a 24dp gap and 58/42 columns.
    val homeCanvas = (usableWidth - 68f - 24f).coerceAtLeast(0f)
    val homeWide = usableWidth >= 840f && homeCanvas * 0.42f >= homeMin
    val favoriteMin = if (largeText) 360f else 290f
    // Favorites uses 20dp list gutters and a 12dp card gap.
    val favoriteCard = ((usableWidth - 40f - 12f) / 2f).coerceAtLeast(0f)
    val twoFavorites = usableWidth >= 840f && favoriteCard >= favoriteMin
    return V8LayoutPolicy(
        availableWidthDp = usableWidth,
        availableHeightDp = usableHeight,
        homeTwoRegions = homeWide && (!compact || usableWidth >= 900f),
        readerTwoPane = panes.isTwoPane,
        favoriteColumns = if (twoFavorites) 2 else 1,
        compactHeight = compact,
        mediumComfortTypography = usableWidth in 600f..<840f && !compact
    )
}
