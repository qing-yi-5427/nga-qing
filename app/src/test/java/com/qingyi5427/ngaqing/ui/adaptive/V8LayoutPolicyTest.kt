package com.qingyi5427.ngaqing.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V8LayoutPolicyTest {
    @Test fun widthThresholdAndMediumTypography() {
        listOf(599f, 600f, 839f).forEach { width ->
            assertFalse(decideV8LayoutPolicy(width, 700f, 1f).readerTwoPane)
        }
        assertFalse(decideV8LayoutPolicy(599f, 700f, 1f).mediumComfortTypography)
        assertTrue(decideV8LayoutPolicy(600f, 700f, 1f).mediumComfortTypography)
        assertTrue(decideV8LayoutPolicy(839f, 700f, 1f).mediumComfortTypography)
        val wide = decideV8LayoutPolicy(840f, 700f, 1f)
        assertTrue(wide.readerTwoPane)
        assertTrue(wide.homeTwoRegions)
        assertEquals(2, wide.favoriteColumns)
    }

    @Test fun shortHeightAndLargeFontsDowngradeReader() {
        assertFalse(decideV8LayoutPolicy(900f, 479f, 1f).readerTwoPane)
        assertTrue(decideV8LayoutPolicy(900f, 479f, 1f).compactHeight)
        assertFalse(decideV8LayoutPolicy(900f, 700f, 1.3f).readerTwoPane)
        assertFalse(decideV8LayoutPolicy(900f, 700f, 1.5f).readerTwoPane)
        assertFalse(decideV8LayoutPolicy(840f, 700f, 1.3f).homeTwoRegions)
        assertFalse(decideV8LayoutPolicy(900f, 700f, 1.5f).homeTwoRegions)
        assertTrue(decideV8LayoutPolicy(950f, 700f, 1.5f).homeTwoRegions)
        assertEquals(2, decideV8LayoutPolicy(900f, 700f, 1.5f).favoriteColumns)
        assertTrue(decideV8LayoutPolicy(1000f, 700f, 1.3f).readerTwoPane)
        // Compact chrome tracks readable rows; reader geometry still uses actual safe dp.
        assertTrue(decideV8LayoutPolicy(320f, 568f, 2f).compactHeight)
        assertFalse(decideV8LayoutPolicy(600f, 700f, 1.3f).compactHeight)
        val largeFontWide = decideV8LayoutPolicy(1200f, 700f, 1.5f)
        assertTrue(largeFontWide.compactHeight)
        assertEquals(calculatePaneLayout(1200f, 700f, null, 1.5f).isTwoPane,
            largeFontWide.readerTwoPane)
    }

    @Test fun safeInsetsAndSeparatingFoldAreUsedBeforePolicy() {
        assertFalse(decideV8LayoutPolicy(860f, 700f, 1f,
            contentWidthDp = 820f).readerTwoPane)
        assertFalse(decideV8LayoutPolicy(900f, 520f, 1f,
            contentHeightDp = 460f).readerTwoPane)
        val fold = FoldGeometry(FoldBounds(425f, 0f, 435f, 608f),
            vertical = true, separating = true)
        val result = decideV8LayoutPolicy(860f, 608f, 1f, fold)
        assertFalse(result.readerTwoPane)
        assertEquals(1, result.favoriteColumns)
        assertTrue(result.availableWidthDp <= 425f)
        val wideFold = FoldGeometry(FoldBounds(640f, 0f, 660f, 700f),
            vertical = true, separating = true)
        val rootSide = decideV8LayoutPolicy(1300f, 700f, 1f, wideFold)
        assertTrue(rootSide.readerTwoPane)
        assertTrue(rootSide.mediumComfortTypography)
        assertFalse(rootSide.homeTwoRegions)
        assertEquals(1, rootSide.favoriteColumns)
        assertEquals(632f, rootSide.availableWidthDp)
    }

    @Test fun preferredListWidthAlwaysProtectsArticle() {
        val narrow = calculatePaneLayout(840f, 700f, null, preferredListWidth = 370f)
        assertTrue(narrow.detailWidth >= 460f)
        val wide = calculatePaneLayout(1200f, 700f, null, preferredListWidth = 370f)
        assertEquals(370f, wide.listWidth)
    }
}
