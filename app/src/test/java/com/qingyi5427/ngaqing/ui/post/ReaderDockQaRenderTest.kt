package com.qingyi5427.ngaqing.ui.post

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A narrow safe content area with enlarged system text must retain every reader action. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w320dp-h480dp")
class ReaderDockQaRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun expandedDockActionsStayInsideSafeWidthAndRemainClickable() {
        val taps = IntArray(6)
        var interactions = 0
        val pressStates = mutableListOf<Boolean>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                NgaQingTheme {
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        ReaderDock(
                            stage = ReaderDockStage.EXPANDED,
                            replyCount = 128,
                            currentPage = 2,
                            totalPages = 9,
                            isFavorite = false,
                            onlyAuthor = false,
                            onExpand = {},
                            onCollapse = { taps[5]++ },
                            onFavorite = { taps[0]++ },
                            onPage = { taps[1]++ },
                            onJump = { taps[2]++ },
                            onOnlyAuthor = { taps[3]++ },
                            onReply = { taps[4]++ },
                            onFocusChanged = {},
                            onInteraction = { interactions++ },
                            onPressChanged = { pressStates += it },
                            compact = true,
                            availableWidthDp = 288f
                        )
                    }
                }
            }
        }
        listOf(
            "收藏",
            "翻页，当前第 2 页，共 9 页",
            "跳转楼层",
            "只看楼主",
            "回复",
            "收起阅读操作"
        ).forEach { label ->
            val action = compose.onNodeWithContentDescription(label).assertIsDisplayed()
            val bounds = action.getUnclippedBoundsInRoot()
            check(bounds.left >= 16.dp && bounds.right <= 304.dp) {
                "$label falls outside the safe 288dp reader area: $bounds"
            }
            action.performClick()
        }
        assertEquals(listOf(1, 1, 1, 1, 1, 1), taps.toList())
        assertEquals("Every action must reset the idle timer", 6, interactions)
        compose.onNodeWithContentDescription("收藏").performTouchInput { click() }
        assertEquals("A pointer press must still activate the action", 2, taps[0])
        assertEquals("Every click must pause then resume its idle timer", 7, pressStates.size / 2)
        assertEquals("Every click must pair press and release", List(7) { listOf(true, false) }.flatten(), pressStates)
        assertEquals("Pointer click must reset the idle timer once", 7, interactions)
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-reader-dock-320-font1p5-expanded.png")
        }
    }
}
