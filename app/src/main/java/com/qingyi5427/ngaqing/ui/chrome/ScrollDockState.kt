package com.qingyi5427.ngaqing.ui.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Visibility shared by a floating dock and the scrolling content behind it. */
@Stable
class ScrollDockState internal constructor(private val thresholdPx: Float) {
    var visible by mutableStateOf(true)
        private set

    private var direction = 0
    private var travel = 0f

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            recordScroll(consumed, available, source)
            return Offset.Zero
        }
    }

    /** Also allows callers to reveal the dock when an explicit page action needs it. */
    fun show() {
        visible = true
        direction = 0
        travel = 0f
    }

    internal fun recordScroll(consumed: Offset, available: Offset, source: NestedScrollSource) {
        if (source != NestedScrollSource.UserInput) return

        // Use the whole user drag, including unconsumed distance at either edge or on short
        // content. Sideways drags do not control the dock.
        val drag = consumed + available
        if (abs(drag.y) <= abs(drag.x)) return
        val delta = drag.y
        if (delta == 0f) return
        val newDirection = if (delta > 0f) 1 else -1
        if ((newDirection > 0) == visible) {
            direction = 0
            travel = 0f
            return
        }
        if (newDirection != direction) {
            direction = newDirection
            travel = 0f
        }
        travel += abs(delta)
        if (travel >= thresholdPx) {
            visible = newDirection > 0
            direction = 0
            travel = 0f
        }
    }
}

@Composable
fun rememberScrollDockState(): ScrollDockState {
    val density = LocalDensity.current
    return remember(density.density) {
        ScrollDockState(with(density) { 16.dp.toPx() })
    }
}
