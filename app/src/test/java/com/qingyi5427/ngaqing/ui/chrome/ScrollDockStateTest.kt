package com.qingyi5427.ngaqing.ui.chrome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollDockStateTest {
    @Test fun userDragChangesVisibilityOnlyAfterDirectionalThreshold() {
        val dock = ScrollDockState(thresholdPx = 16f)
        dock.recordScroll(Offset(0f, -8f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(dock.visible)
        dock.recordScroll(Offset(0f, -9f), Offset.Zero, NestedScrollSource.UserInput)
        assertFalse(dock.visible)

        dock.recordScroll(Offset(0f, 8f), Offset.Zero, NestedScrollSource.UserInput)
        assertFalse(dock.visible)
        dock.recordScroll(Offset(0f, 9f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(dock.visible)

        dock.recordScroll(Offset(0f, -9f), Offset.Zero, NestedScrollSource.UserInput)
        dock.recordScroll(Offset(0f, 9f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(dock.visible)
    }

    @Test fun edgeAndShortContentDragsStillControlDockButHorizontalDragDoesNot() {
        val dock = ScrollDockState(thresholdPx = 16f)
        dock.recordScroll(Offset.Zero, Offset(0f, -20f), NestedScrollSource.UserInput)
        assertFalse(dock.visible)
        dock.recordScroll(Offset.Zero, Offset(0f, 20f), NestedScrollSource.UserInput)
        assertTrue(dock.visible)
        dock.recordScroll(Offset(-40f, -4f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(dock.visible)
    }

    @Test fun programmaticScrollAndLayoutMovementCannotHideDock() {
        val dock = ScrollDockState(thresholdPx = 16f)
        dock.recordScroll(Offset(0f, -100f), Offset.Zero, NestedScrollSource.SideEffect)
        assertTrue(dock.visible)
        dock.recordScroll(Offset(0f, -8f), Offset(0f, -9f), NestedScrollSource.UserInput)
        assertFalse(dock.visible)
        dock.show()
        assertTrue(dock.visible)
    }
}
