package com.qingyi5427.ngaqing.ui.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class ReaderDockStage { HIDDEN, PEEK, EXPANDED }

/** Observes only user scroll consumed by the native reader. It never consumes scroll. */
@Stable
internal class ReaderDockState(
    private val thresholdPx: Float,
    private val jitterPx: Float
) {
    var stage by mutableStateOf(ReaderDockStage.PEEK)
        private set
    var mode by mutableStateOf(ReadingDockMode.AUTO)
        private set
    var timerEpoch by mutableIntStateOf(0)
        private set

    private var direction = 0
    private var distance = 0f

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            recordScroll(consumed, source)
            return Offset.Zero
        }
    }

    fun updateMode(next: ReadingDockMode) {
        if (mode == next) return
        mode = next
        transition(if (next == ReadingDockMode.BOOKS) ReaderDockStage.HIDDEN else ReaderDockStage.PEEK)
    }

    fun onBlankMarginTap() {
        if (mode == ReadingDockMode.BOOKS) transition(ReaderDockStage.PEEK)
    }

    fun expand() = transition(ReaderDockStage.EXPANDED)
    fun peek() = transition(ReaderDockStage.PEEK)
    fun hide() = transition(ReaderDockStage.HIDDEN)
    fun resetIdleTimer() { timerEpoch++ }

    fun onIdleTimeout() {
        when (stage) {
            ReaderDockStage.EXPANDED -> peek()
            ReaderDockStage.PEEK -> hide()
            ReaderDockStage.HIDDEN -> Unit
        }
    }

    /** Called from a keyed LaunchedEffect; cancellation prevents a stale timer from hiding UI. */
    suspend fun idleAfter(milliseconds: Long) {
        delay(milliseconds)
        onIdleTimeout()
    }

    internal fun recordScroll(consumed: Offset, source: NestedScrollSource) {
        if (source != NestedScrollSource.UserInput) return
        if (abs(consumed.y) <= abs(consumed.x)) return
        val delta = consumed.y
        if (abs(delta) < jitterPx) return
        if (mode == ReadingDockMode.BOOKS) {
            if (stage != ReaderDockStage.HIDDEN) hide()
            return
        }
        val newDirection = if (delta > 0f) 1 else -1
        if ((newDirection > 0 && stage != ReaderDockStage.HIDDEN) ||
            (newDirection < 0 && stage == ReaderDockStage.HIDDEN)) {
            direction = 0
            distance = 0f
            return
        }
        if (newDirection != direction) {
            direction = newDirection
            distance = 0f
        }
        distance += abs(delta)
        if (distance >= thresholdPx) {
            if (newDirection > 0) peek() else hide()
        }
    }

    private fun transition(next: ReaderDockStage) {
        stage = next
        timerEpoch++
        direction = 0
        distance = 0f
    }
}

@Composable
internal fun rememberReaderDockState(threadId: String): ReaderDockState {
    val density = LocalDensity.current
    return remember(threadId, density.density) {
        ReaderDockState(
            thresholdPx = with(density) { 26.dp.toPx() },
            jitterPx = with(density) { 2.dp.toPx() }
        )
    }
}
