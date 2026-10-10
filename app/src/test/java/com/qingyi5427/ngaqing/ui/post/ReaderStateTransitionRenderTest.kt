package com.qingyi5427.ngaqing.ui.post

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.ui.PostRouteContent
import com.qingyi5427.ngaqing.ui.resolvePostRouteLayout
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.V8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises the mounted production editor and reading list while pane width/focus change. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w986dp-h698dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderStateTransitionRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun targetJumpSavesConfirmedFloorInsteadOfPreviousIdleAnchor() {
        val target = MutableStateFlow<Int?>(null)
        val vm = fixture(target)
        @Suppress("UNCHECKED_CAST")
        val state = vm.uiState as MutableStateFlow<PostUiState>
        val success = state.value as PostUiState.Success
        val writes = mutableListOf<Int>()
        every { vm.updateReadFloor(any()) } answers { writes.add(firstArg()); Unit }
        every { vm.consumeTargetFloor() } answers { target.value = null }
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.readingDockMode } returns MutableStateFlow(ReadingDockMode.AUTO)
        every { root.readingTextScale } returns MutableStateFlow(1f)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                PostScreen(rememberNavController(), dark = false, viewModel = vm, root = root)
            }
        }
        compose.onNodeWithTag("post-reading-list").performScrollToNode(hasText("#7"))
        compose.waitForIdle()
        val previous = writes.last()
        assertFalse(previous == 5)
        writes.clear()

        compose.runOnUiThread {
            target.value = 5
            state.value = PostUiState.Loading
        }
        compose.waitForIdle()
        compose.runOnUiThread { state.value = success.copy() }
        compose.onNodeWithText("#5").assertIsDisplayed()
        compose.waitForIdle()
        assertEquals("the actual target floor must be the final saved anchor", 5, writes.last())
        assertFalse("the previous list anchor must not be saved during a pending jump",
            writes.contains(previous))
    }

    @Test fun targetFloorAndReplySelectionSurviveWideNarrowWideAndFocus() {
        val width = mutableStateOf(986.dp)
        val focus = mutableStateOf(false)
        val target = MutableStateFlow<Int?>(5)
        val vm = fixture(target)
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.readingDockMode } returns MutableStateFlow(ReadingDockMode.AUTO)
        every { root.readingTextScale } returns MutableStateFlow(1f)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                val panes = calculatePaneLayout(width.value.value, 698f, null,
                    preferredListWidth = 294f)
                val layout = resolvePostRouteLayout(panes,
                    hasThreadSource = true, focusMode = focus.value)
                CompositionLocalProvider(LocalV8LayoutPolicy provides V8LayoutPolicy(
                    availableWidthDp = width.value.value, availableHeightDp = 698f,
                    readerTwoPane = panes.isTwoPane
                )) {
                    Surface(Modifier.fillMaxHeight()) {
                        Box(Modifier.width(width.value).fillMaxHeight()) {
                            val nav = rememberNavController()
                            PostRouteContent(panes, layout, readerPaneWidth = 294f,
                                onReaderPaneWidthChange = {},
                                threadList = {
                                    Box(Modifier.fillMaxHeight().testTag("thread-list-pane")) {
                                        Text("主题列表")
                                    }
                                },
                                post = {
                                    PostScreen(nav, dark = false, viewModel = vm,
                                        root = root,
                                        isFocusMode = layout.canFocus && focus.value,
                                        onToggleFocus = if (layout.canFocus) ({ focus.value = !focus.value }) else null,
                                        onShowBoards = {}, boardName = "数码硬件")
                                })
                        }
                    }
                }
            }
        }

        compose.onNodeWithText("#5").assertIsDisplayed()
        compose.onNodeWithTag("thread-list-pane").assertIsDisplayed()
        compose.onNodeWithText("专注阅读").performClick()
        compose.onNodeWithText("恢复双栏").assertIsDisplayed()
        compose.onNodeWithTag("thread-list-pane").assertDoesNotExist()
        compose.runOnUiThread { width.value = 394.dp }
        compose.onNodeWithText("#5").assertIsDisplayed()
        compose.onNodeWithTag("thread-list-pane").assertDoesNotExist()
        compose.runOnUiThread { width.value = 986.dp }
        compose.onNodeWithText("#5").assertIsDisplayed()

        compose.onNodeWithContentDescription("更多").performClick()
        compose.onNodeWithText("回复帖子").performClick()
        val editor = compose.onNode(hasSetTextAction())
        editor.performTextInput("abcdefghi")
        editor.performTextInputSelection(TextRange(2, 6))
        check(editor.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(2, 6))

        compose.runOnUiThread {
            width.value = 394.dp
            focus.value = false
        }
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.runOnUiThread {
            width.value = 986.dp
            focus.value = true
        }
        val restored = compose.onNode(hasSetTextAction())
        check(restored.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(2, 6)) {
            "Reply text selection must survive the same pane transitions as its draft"
        }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("#5").assertIsDisplayed()
        compose.onNodeWithText("恢复双栏").performClick()
        compose.onNodeWithTag("thread-list-pane").assertIsDisplayed()
    }

    private fun fixture(target: MutableStateFlow<Int?>): PostViewModel {
        val vm = mockk<PostViewModel>(relaxed = true)
        val posts = (0..8).map { floor ->
            Post("floor-reader", "pid-$floor", "读者$floor", "$floor",
                if (floor == 0) "阅读位置与草稿选择测试" else "",
                1_790_540_000L + floor, floor,
                "这是第${floor}楼的实际正文，包含足够的文字使阅读列表可以滚动。".repeat(4))
        }
        every { vm.tid } returns "floor-reader"
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts, page = 1, totalPages = 1))
        every { vm.subject } returns MutableStateFlow("阅读位置与草稿选择测试")
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns target
        every { vm.totalRows } returns MutableStateFlow(9)
        every { vm.onlyAuthor } returns MutableStateFlow(false)
        every { vm.replyTarget } returns MutableStateFlow(null)
        every { vm.draftContent } returns MutableStateFlow("")
        every { vm.watching } returns MutableStateFlow(false)
        every { vm.ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { vm.webCookiesReady } returns MutableStateFlow(false)
        every { vm.replying } returns MutableStateFlow(false)
        every { vm.replyResult } returns MutableStateFlow<String?>(null)
        every { vm.replySucceeded } returns MutableStateFlow(false)
        every { vm.favoriteError } returns MutableStateFlow<String?>(null)
        return vm
    }
}
