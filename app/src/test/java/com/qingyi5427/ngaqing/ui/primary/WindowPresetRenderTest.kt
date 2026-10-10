package com.qingyi5427.ngaqing.ui.primary

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.dp
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.PostRouteContent
import com.qingyi5427.ngaqing.ui.ReadingPane
import com.qingyi5427.ngaqing.ui.RootPane
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.ThreadRouteContent
import com.qingyi5427.ngaqing.ui.resolvePostRouteLayout
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.discover.DiscoverScreen
import com.qingyi5427.ngaqing.ui.discover.DiscoverState
import com.qingyi5427.ngaqing.ui.discover.DiscoverViewModel
import com.qingyi5427.ngaqing.ui.favorites.FavoritesScreen
import com.qingyi5427.ngaqing.ui.favorites.FavoritesViewModel
import com.qingyi5427.ngaqing.ui.post.PostBlock
import com.qingyi5427.ngaqing.ui.post.PostRenderData
import com.qingyi5427.ngaqing.ui.post.PostScreen
import com.qingyi5427.ngaqing.ui.post.PostUiState
import com.qingyi5427.ngaqing.ui.post.PostViewModel
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import com.qingyi5427.ngaqing.ui.thread.ThreadListScreen
import com.qingyi5427.ngaqing.ui.thread.ThreadListViewModel
import com.qingyi5427.ngaqing.ui.thread.ThreadSort
import com.qingyi5427.ngaqing.ui.thread.ThreadUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native Compose simulation; Fold dp values assume 440 dpi and are not physical Fold evidence. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WindowPresetRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test @Config(sdk = [33], qualifiers = "w403dp-h875dp")
    fun phoneP() = renderPreset("phoneP-480dpi", 403f, 875f, readerWide = false)

    @Test @Config(sdk = [33], qualifiers = "w875dp-h403dp")
    fun phoneL() = renderPreset("phoneL-480dpi", 875f, 403f, readerWide = false)

    @Test @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun coverP() = renderPreset("coverP-440dpi-assumed", 425f, 623f, readerWide = false)

    @Test @Config(sdk = [33], qualifiers = "w623dp-h425dp")
    fun coverL() = renderPreset("coverL-440dpi-assumed", 623f, 425f, readerWide = false)

    @Test @Config(sdk = [33], qualifiers = "w608dp-h860dp")
    fun innerP() = renderPreset("innerP-440dpi-assumed", 608f, 860f, readerWide = false)

    @Test @Config(sdk = [33], qualifiers = "w860dp-h608dp")
    fun innerL() = renderPreset("innerL-440dpi-assumed", 860f, 608f, readerWide = true)

    private fun renderPreset(name: String, width: Float, height: Float, readerWide: Boolean) {
        val panes = calculatePaneLayout(width, height, null)
        val policy = decideV8LayoutPolicy(width, height, 1f)
        check(panes.isTwoPane == readerWide)
        check(policy.readerTwoPane == readerWide)
        check(policy.homeTwoRegions == readerWide)
        val root = rootFixture()
        val home = homeFixture()
        val favorites = favoritesFixture()
        val threads = threadFixture()
        val posts = postFixture()
        val page = mutableIntStateOf(0)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                Surface(Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    CompositionLocalProvider(LocalV8LayoutPolicy provides policy) {
                        when (page.intValue) {
                            0 -> RootPane(panes, nav, Routes.DISCOVER) {
                                DiscoverScreen(nav, home)
                            }
                            1 -> RootPane(panes, nav, Routes.FAVORITES) {
                                FavoritesScreen(nav, favorites)
                            }
                            2 -> ReadingPane(panes, nav, Routes.THREADS) { current ->
                                ThreadRouteContent(current, nav, root, threads,
                                    onOpenBoard = {}, onShowBoards = {})
                            }
                            else -> ReadingPane(panes, nav, Routes.POSTS) { current ->
                                PostRouteContent(current,
                                    resolvePostRouteLayout(current, hasThreadSource = true,
                                        focusMode = false),
                                    readerPaneWidth = current.listWidth,
                                    onReaderPaneWidthChange = {},
                                    threadList = {
                                        ThreadListScreen(nav, threads, root, selectedTid = "101")
                                    }, post = {
                                        PostScreen(nav, dark = false, viewModel = posts, root = root,
                                            onToggleFocus = {}, onShowBoards = {}, boardName = "数码硬件")
                                    })
                            }
                        }
                    }
                }
            }
        }
        val pages = listOf("discover", "favorites", "threads", "reader")
        pages.forEachIndexed { index, label ->
            compose.runOnUiThread { page.intValue = index }
            compose.waitForIdle()
            when (index) {
                0, 1 -> compose.onNodeWithContentDescription("打开导航和更多选项").assertIsDisplayed()
                2 -> compose.onNodeWithContentDescription("打开版块目录").assertIsDisplayed()
                3 -> compose.onNodeWithContentDescription("收藏").assertIsDisplayed()
            }
            if (index == 2 && readerWide) {
                compose.onNodeWithTag("thread-empty-reader").assertIsDisplayed()
            } else {
                compose.onNodeWithTag("thread-empty-reader").assertDoesNotExist()
            }
            compose.onNodeWithTag("root-navigation-group").assertDoesNotExist()
            if (!readerWide && index != 3) {
                val dock = compose.onNodeWithTag("root-bottom-dock")
                    .assertIsDisplayed().getUnclippedBoundsInRoot()
                check(dock.left >= 0.dp && dock.right <= width.dp && dock.bottom <= height.dp)
            } else {
                compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
            }
            compose.runOnUiThread {
                saveNativeScreenshot(compose.activity, "v8-sim-$name-$label.png")
            }
        }
    }

    private fun rootFixture(): RootViewModel = mockk(relaxed = true) {
        every { blacklistUsers } returns MutableStateFlow(emptySet())
        every { blacklistKeywords } returns MutableStateFlow(emptySet())
        every { readingDockMode } returns MutableStateFlow(ReadingDockMode.AUTO)
        every { readingTextScale } returns MutableStateFlow(1f)
    }

    private fun homeFixture(): DiscoverViewModel = mockk(relaxed = true) {
        val board = Board("10", "数码硬件", "设备与体验")
        every { state } returns MutableStateFlow<DiscoverState>(DiscoverState.Ready(
            boards = listOf(board), discussionBoard = board,
            discussions = listOf(threadItem())))
        every { favoriteBoards } returns MutableStateFlow(emptyList())
        every { history } returns MutableStateFlow(listOf(
            HistoryEntity("101", "折叠屏使用一周后的体验", "晚风过境", "10", lastFloor = 5)))
    }

    private fun favoritesFixture(): FavoritesViewModel = mockk(relaxed = true) {
        every { favorites } returns MutableStateFlow(listOf(
            FavoriteEntity("101", "折叠屏使用一周后的体验", "10", "晚风过境", folder = "数码硬件"),
            FavoriteEntity("102", "阅读模式和护眼设置", "10", "纸上星河", folder = "数码硬件")))
        every { visitedTids } returns MutableStateFlow(setOf("101"))
        every { folders } returns MutableStateFlow(listOf("数码硬件"))
        every { syncing } returns MutableStateFlow(false)
        every { syncMessage } returns MutableStateFlow<String?>(null)
        every { syncError } returns MutableStateFlow<String?>(null)
        every { actionError } returns MutableStateFlow<String?>(null)
    }

    private fun threadFixture(): ThreadListViewModel = mockk(relaxed = true) {
        every { uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads = listOf(threadItem()), subBoards = emptyList(), page = 1,
            totalPages = 1, recommendedOnly = false))
        every { subBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { recommendedOnly } returns MutableStateFlow(false)
        every { sort } returns MutableStateFlow(ThreadSort.LAST_REPLY)
        every { isRefreshing } returns MutableStateFlow(false)
        every { isFavoriteBoard } returns MutableStateFlow(false)
        every { headerHiddenFraction } returns MutableStateFlow(0f)
        every { visitedTids } returns MutableStateFlow(emptySet())
        every { initialScrollIndex } returns 0
        every { initialScrollOffset } returns 0
        every { fid } returns "10"
        every { stid } returns null
        every { name } returns "数码硬件"
    }

    private fun postFixture(): PostViewModel = mockk(relaxed = true) {
        val post = Post("101", "201", "晚风过境", "42", "折叠屏使用一周后的体验",
            1_790_540_000L, 0, "这周把阅读和讨论都搬到内屏，正文仍然清楚可读。")
        every { uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post), page = 1, totalPages = 1,
            renderData = mapOf("201" to PostRenderData(
                listOf(PostBlock.Text(post.content)), emptyList(), emptyList()))))
        every { subject } returns MutableStateFlow(post.subject)
        every { isFavorite } returns MutableStateFlow(false)
        every { isLoadingMore } returns MutableStateFlow(false)
        every { loadError } returns MutableStateFlow<String?>(null)
        every { isRefreshing } returns MutableStateFlow(false)
        every { targetFloor } returns MutableStateFlow<Int?>(null)
        every { totalRows } returns MutableStateFlow(1)
        every { onlyAuthor } returns MutableStateFlow(false)
        every { replyTarget } returns MutableStateFlow(null)
        every { draftContent } returns MutableStateFlow("")
        every { watching } returns MutableStateFlow(false)
        every { ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { webCookiesReady } returns MutableStateFlow(false)
        every { replying } returns MutableStateFlow(false)
        every { replyResult } returns MutableStateFlow<String?>(null)
        every { replySucceeded } returns MutableStateFlow(false)
        every { favoriteError } returns MutableStateFlow<String?>(null)
    }

    private fun threadItem() = ThreadItem("101", "折叠屏使用一周后的体验", "晚风过境",
        "42", 1_790_540_000L, replies = 36)
}
