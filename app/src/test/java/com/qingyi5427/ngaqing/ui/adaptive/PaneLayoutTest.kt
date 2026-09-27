package com.qingyi5427.ngaqing.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneLayoutTest {
    @Test fun compactWindowStaysSinglePane() {
        val pane = calculatePaneLayout(412f, 915f, null)
        assertFalse(pane.isTwoPane)
        assertEquals(412f, pane.singlePaneWidth)
    }

    @Test fun expandedWindowGetsUsableListAndReadingPane() {
        val pane = calculatePaneLayout(860f, 608f, null)
        assertTrue(pane.isTwoPane)
        assertTrue(pane.listWidth >= 280f)
        assertTrue(pane.detailWidth >= 360f)
        assertEquals(860f, pane.listWidth + pane.gutterWidth + pane.detailWidth)
    }

    @Test fun verticalHingeSplitsTwoPanesWithoutCrossingCrease() {
        val fold = FoldGeometry(FoldBounds(425f, 0f, 435f, 608f), vertical = true, separating = true)
        val pane = calculatePaneLayout(860f, 608f, fold)
        assertTrue(pane.isTwoPane)
        assertTrue(pane.listWidth < fold.bounds.left)
        assertTrue(pane.listWidth + pane.gutterWidth > fold.bounds.right)
        assertTrue(
            pane.singlePaneLeft + pane.singlePaneWidth <= fold.bounds.left ||
                pane.singlePaneLeft >= fold.bounds.right
        )
    }

    @Test fun narrowWindowWithHingeUsesOneSafeSide() {
        val fold = FoldGeometry(FoldBounds(350f, 0f, 360f, 700f), vertical = true, separating = true)
        val pane = calculatePaneLayout(700f, 700f, fold)
        assertFalse(pane.isTwoPane)
        assertEquals(0f, pane.viewportLeft)
        assertEquals(342f, pane.viewportWidth)
    }

    @Test fun edgeHingeUsesWiderSideInsteadOfNarrowSecondPane() {
        val fold = FoldGeometry(FoldBounds(80f, 0f, 90f, 608f), vertical = true, separating = true)
        val pane = calculatePaneLayout(860f, 608f, fold)
        assertFalse(pane.isTwoPane)
        assertEquals(98f, pane.viewportLeft)
        assertEquals(762f, pane.viewportWidth)
    }

    @Test fun tabletopPostureKeepsContentBelowHorizontalFold() {
        val fold = FoldGeometry(FoldBounds(0f, 240f, 860f, 260f), vertical = false, separating = true)
        val pane = calculatePaneLayout(860f, 608f, fold)
        assertFalse(pane.isTwoPane)
        assertEquals(260f, pane.viewportTop)
        assertEquals(348f, pane.viewportHeight)
    }

    @Test fun foldOutsideWindowDoesNotChangeLayout() {
        val outside = FoldGeometry(FoldBounds(1200f, 0f, 1210f, 608f), vertical = true, separating = true)
        val above = FoldGeometry(FoldBounds(425f, -30f, 435f, -10f), vertical = true, separating = true)
        val below = FoldGeometry(FoldBounds(0f, 900f, 860f, 910f), vertical = false, separating = true)
        assertTrue(calculatePaneLayout(860f, 608f, outside).isTwoPane)
        assertEquals(860f, calculatePaneLayout(860f, 608f, above).viewportWidth)
        assertEquals(608f, calculatePaneLayout(860f, 608f, below).viewportHeight)
    }
}
