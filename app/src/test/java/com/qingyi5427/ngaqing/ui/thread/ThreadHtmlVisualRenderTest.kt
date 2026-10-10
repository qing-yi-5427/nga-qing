package com.qingyi5427.ngaqing.ui.thread

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.ThreadRouteContent
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.V8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
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

/** The same honest forum fixture at the HTML phone and landscape inner-window sizes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThreadHtmlVisualRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w394dp-h854dp")
    fun phoneFeedKeepsRealActionsAndEditorialDensity() {
        val (vm, root) = fixture()
        var openedBoards = 0
        var openedTid: String? = null
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                Surface(Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = Routes.THREADS) {
                        composable(Routes.THREADS) {
                            ThreadListScreen(nav, vm, root,
                                onOpenPost = { openedTid = it },
                                onShowBoards = { openedBoards++ })
                        }
                        composable(Routes.BOARDS) { Text("版块根页") }
                    }
                }
            }
        }
        compose.onNodeWithText(FIRST_TITLE).assertIsDisplayed()
        compose.onNodeWithContentDescription("搜索本版").assertIsDisplayed()
        compose.onNodeWithContentDescription("发主题").assertIsDisplayed()
        val dock = compose.onNodeWithTag("root-bottom-dock").getUnclippedBoundsInRoot()
        check(dock.top > 650.dp && dock.bottom <= 854.dp) { "Phone dock must sit at the bottom: $dock" }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-thread-html-phoneP-394x854.png") }
        compose.onNodeWithContentDescription("打开版块目录").performClick()
        check(openedBoards == 1)
        compose.onNodeWithText(FIRST_TITLE).performClick()
        check(openedTid == "101")
        compose.onNodeWithTag("thread-lazy-list").performScrollToIndex(5)
        val lastTitle = compose.onNodeWithText(LAST_TITLE).assertIsDisplayed().getUnclippedBoundsInRoot()
        check(lastTitle.bottom <= dock.top) { "Last thread must remain reachable above dock: $lastTitle / $dock" }
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").performClick()
        compose.onNodeWithText("版块根页").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w986dp-h698dp")
    fun innerLandscapeHasCompactThreadPaneAndEmptyReader() {
        val (vm, root) = fixture()
        val panes = calculatePaneLayout(986f, 698f, null, preferredListWidth = 294f)
        check(panes.isTwoPane)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                CompositionLocalProvider(LocalV8LayoutPolicy provides
                    V8LayoutPolicy(availableWidthDp = 986f, availableHeightDp = 698f,
                        readerTwoPane = true)) {
                    Surface(Modifier.fillMaxSize()) {
                        ThreadRouteContent(panes, rememberNavController(), root, vm,
                            onOpenBoard = {}, onShowBoards = {})
                    }
                }
            }
        }
        compose.onNodeWithText(FIRST_TITLE).assertIsDisplayed()
        compose.onNodeWithText("选择主题开始阅读").assertIsDisplayed()
        val list = compose.onNodeWithTag("thread-top-bar").getUnclippedBoundsInRoot()
        val reader = compose.onNodeWithTag("thread-empty-reader").getUnclippedBoundsInRoot()
        check(list.right <= reader.left && list.right - list.left <= 310.dp)
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-thread-html-innerL-986x698.png") }
    }

    private fun fixture(): Pair<ThreadListViewModel, RootViewModel> {
        val now = System.currentTimeMillis() / 1_000L
        val titles = listOf(
            FIRST_TITLE,
            "求推荐一台安静的小主机，预算三千左右",
            "这几个系统交互细节，真的用过就回不去了",
            "桌面整理半年总结：Less is more",
            "有多少人会长期使用阅读模式和护眼设置？",
            LAST_TITLE
        )
        val authors = listOf("晚风过境", "深蓝信号", "失眠收音机", "南岸的云", "纸上星河", "折线计划")
        val threads = titles.mapIndexed { index, title ->
            ThreadItem((101 + index).toString(), title, authors[index], (41 + index).toString(),
                now - index * 600L, replies = listOf(128, 64, 37, 103, 41, 75)[index],
                recommend = if (index == 2) 1 else 0)
        }
        val vm = mockk<ThreadListViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads, emptyList(), page = 1, totalPages = 1, recommendedOnly = false))
        every { vm.subBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { vm.recommendedOnly } returns MutableStateFlow(false)
        every { vm.sort } returns MutableStateFlow(ThreadSort.LAST_REPLY)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.isFavoriteBoard } returns MutableStateFlow(false)
        every { vm.headerHiddenFraction } returns MutableStateFlow(0f)
        every { vm.visitedTids } returns MutableStateFlow(emptySet())
        every { vm.initialScrollIndex } returns 0
        every { vm.initialScrollOffset } returns 0
        every { vm.fid } returns "10"
        every { vm.stid } returns null
        every { vm.name } returns "数码硬件"
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        return vm to root
    }

    private companion object {
        const val FIRST_TITLE = "折叠屏使用一周后，我终于理解了它真正的意义"
        const val LAST_TITLE = "最近入手一台便携显示器，聊聊体验和踩坑"
    }
}
