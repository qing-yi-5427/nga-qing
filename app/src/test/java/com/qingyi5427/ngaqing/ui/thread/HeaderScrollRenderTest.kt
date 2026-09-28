package com.qingyi5427.ngaqing.ui.thread

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** Measures intermediate frames from the actual screen, not a detached animation mock. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w390dp-h700dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeaderScrollRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun headerAndFirstRowTravelTogetherAndPartialProgressSurvivesRecreation() {
        val progress = MutableStateFlow(0f)
        val vm = threadViewModel(progress)
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        val mounted = mutableStateOf(true)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                Surface(Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    if (mounted.value) ThreadListScreen(nav, vm, root)
                }
            }
        }

        val firstTitle = "主题 1：地图路线"
        val initialHeader = compose.onNodeWithTag("thread-header").getUnclippedBoundsInRoot()
        val initialRow = compose.onNodeWithText(firstTitle).assertIsDisplayed().getUnclippedBoundsInRoot()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "thread-header-start-390.png") }

        compose.onNodeWithTag("thread-list-viewport").performTouchInput {
            down(center)
            moveBy(Offset(0f, -64f), delayMillis = 500)
            up()
        }
        val middleHeader = compose.onNodeWithTag("thread-header").getUnclippedBoundsInRoot()
        val middleRow = compose.onNodeWithText(firstTitle).assertIsDisplayed().getUnclippedBoundsInRoot()
        val headerTravel = (initialHeader.top - middleHeader.top).value
        val rowTravel = (initialRow.top - middleRow.top).value
        check(progress.value in 0.05f..0.9f) { "The slow drag should leave a partially hidden header" }
        check(headerTravel in 20f..80f) { "Expected a continuous intermediate frame, moved $headerTravel dp" }
        check(abs(headerTravel - rowTravel) <= 3f) {
            "Header moved $headerTravel dp while the first row moved $rowTravel dp"
        }
        check(abs((initialHeader.bottom - initialHeader.top).value -
            (middleHeader.bottom - middleHeader.top).value) <= 1f) {
            "The header's measured height must stay intact while translating"
        }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "thread-header-middle-390.png") }

        compose.runOnUiThread { mounted.value = false }
        compose.runOnUiThread { mounted.value = true }
        val restoredHeader = compose.onNodeWithTag("thread-header").getUnclippedBoundsInRoot()
        val restoredRow = compose.onNodeWithText(firstTitle).assertIsDisplayed().getUnclippedBoundsInRoot()
        check(abs((restoredHeader.top - middleHeader.top).value) <= 1f)
        check(abs((restoredRow.top - middleRow.top).value) <= 1f)
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "thread-header-restored-390.png") }

        compose.onNodeWithTag("thread-list-viewport").performTouchInput { swipeUp() }
        check(progress.value > 0.9f) { "A long upward gesture should move the header fully out" }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "thread-header-hidden-390.png") }
        compose.onNodeWithTag("thread-list-viewport").performTouchInput { swipeDown() }
        check(progress.value < 0.9f) { "A downward gesture should reveal the header" }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun shortWindowCanDragHeaderToReachListAtDoubleFont() {
        val progress = MutableStateFlow(0f)
        val vm = threadViewModel(progress)
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)
            ) {
                NgaQingTheme(themeMode = "light") {
                    Surface(Modifier.fillMaxSize()) { ThreadListScreen(rememberNavController(), vm, root) }
                }
            }
        }
        compose.onNodeWithTag("thread-header").performTouchInput { swipeUp() }
        check(progress.value > 0f)
        compose.onNodeWithText("主题 1：地图路线").assertIsDisplayed()
    }

    private fun threadViewModel(progress: MutableStateFlow<Float>): ThreadListViewModel =
        mockk<ThreadListViewModel>(relaxed = true).also { vm ->
            every { vm.uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
                threads = (1..12).map { n ->
                    ThreadItem("$n", "主题 $n：地图路线", "北岸行舟", "42", 1_790_540_000L, replies = n * 2)
                }, subBoards = emptyList(), page = 1, totalPages = 1, recommendedOnly = false
            ))
            every { vm.subBoards } returns MutableStateFlow<List<Board>>(emptyList())
            every { vm.recommendedOnly } returns MutableStateFlow(false)
            every { vm.sort } returns MutableStateFlow(ThreadSort.LAST_REPLY)
            every { vm.isRefreshing } returns MutableStateFlow(false)
            every { vm.isFavoriteBoard } returns MutableStateFlow(false)
            every { vm.headerHiddenFraction } returns progress
            every { vm.setHeaderHiddenFraction(any()) } answers { progress.value = firstArg() }
            every { vm.visitedTids } returns MutableStateFlow(emptySet())
            every { vm.initialScrollIndex } returns 0
            every { vm.initialScrollOffset } returns 0
            every { vm.fid } returns "10"
            every { vm.stid } returns null
            every { vm.name } returns "探索交流"
        }
}
