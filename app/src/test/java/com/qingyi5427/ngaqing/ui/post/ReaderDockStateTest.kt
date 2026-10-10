package com.qingyi5427.ngaqing.ui.post

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderDockStateTest {
    private fun drag(state: ReaderDockState, y: Float, x: Float = 0f,
                     source: NestedScrollSource = NestedScrollSource.UserInput) {
        state.recordScroll(Offset(x, y), source)
    }

    @Test fun directionThresholdUsesConsumedUserScrollAndFiltersJitter() {
        val state = ReaderDockState(thresholdPx = 26f, jitterPx = 2f)
        assertEquals(ReaderDockStage.PEEK, state.stage)
        drag(state, -1f)
        drag(state, -13f)
        drag(state, -12f)
        assertEquals(ReaderDockStage.PEEK, state.stage)
        drag(state, -3f, x = 20f)
        drag(state, -3f, source = NestedScrollSource.SideEffect)
        assertEquals(ReaderDockStage.PEEK, state.stage)
        drag(state, -3f)
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        drag(state, 14f)
        drag(state, -8f) // opposite direction resets accumulated distance
        drag(state, 14f)
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        drag(state, 13f)
        assertEquals(ReaderDockStage.PEEK, state.stage)
    }

    @Test fun booksModeDoesNotRevealOnScrollAndHidesAfterBlankTap() {
        val state = ReaderDockState(26f, 2f)
        state.updateMode(ReadingDockMode.BOOKS)
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        drag(state, 100f)
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        state.onBlankMarginTap()
        assertEquals(ReaderDockStage.PEEK, state.stage)
        drag(state, -3f)
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        state.updateMode(ReadingDockMode.AUTO)
        assertEquals(ReaderDockStage.PEEK, state.stage)
    }

    @Test fun idleTimerTransitionsAndCanceledJobCannotHideExpandedDock() = runTest {
        val state = ReaderDockState(26f, 2f)
        state.expand()
        val staleTimer = launch { state.idleAfter(2800) }
        advanceTimeBy(1000)
        state.resetIdleTimer()
        staleTimer.cancel()
        advanceUntilIdle()
        assertEquals(ReaderDockStage.EXPANDED, state.stage)
        launch { state.idleAfter(2800) }
        advanceTimeBy(2801)
        assertEquals(ReaderDockStage.PEEK, state.stage)
        launch { state.idleAfter(4500) }
        advanceUntilIdle()
        assertEquals(ReaderDockStage.HIDDEN, state.stage)
        assertTrue(state.timerEpoch > 0)
    }
}
