package com.qingyi5427.ngaqing.ui.primary

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.RootPane
import com.qingyi5427.ngaqing.ui.ReadingPane
import com.qingyi5427.ngaqing.ui.railAdjustedPaneLayout
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.board.BoardScreen
import com.qingyi5427.ngaqing.ui.board.BoardUiState
import com.qingyi5427.ngaqing.ui.board.BoardViewModel
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.profile.ProfileScreen
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.thread.ThreadListScreen
import com.qingyi5427.ngaqing.ui.thread.ThreadListViewModel
import com.qingyi5427.ngaqing.ui.thread.ThreadSort
import com.qingyi5427.ngaqing.ui.thread.ThreadUiState
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

/** The actual root rail and production profile screen, including their shared inset path. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class RootPaneRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun wideRootUsesRailAndProfileContent() {
        renderRoot(1200f, 800f, 1f)
        compose.onNodeWithText("版块", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("收藏", useUnmergedTree = true).assertIsDisplayed()
        val railProfile = compose.onAllNodesWithText("我的", useUnmergedTree = true)[0]
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(railProfile.right <= 88.dp)
        compose.onNodeWithText("一位阅读论坛的用户").assertIsDisplayed()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "root-profile-1200x800-light.png") }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun wideRootKeepsBoardDirectoryInsideRailShell() {
        val vm = mockk<BoardViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(BoardGroup("社区版块", "社区版块", children = listOf(
                Board("10", "技术讨论", info = "设备、软件与日常使用"),
                Board("11", "阅读交流", info = "值得细读的话题")
            )))
        ))
        every { vm.favoriteBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.initialScrollIndex } returns 0
        every { vm.initialScrollOffset } returns 0
        renderShell(1200f, 800f, 1f, Routes.BOARDS) { nav -> BoardScreen(nav, vm) }
        compose.onNodeWithText("目录概览").assertIsDisplayed()
        compose.onNodeWithText("技术讨论").assertIsDisplayed()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "root-board-1200x800-light.png") }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun wideReadingRouteKeepsRailAndTwoProductionPanes() {
        val boards = mockk<BoardViewModel>(relaxed = true)
        every { boards.uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(BoardGroup("社区版块", "社区版块", children = listOf(
                Board("10", "技术讨论"), Board("11", "阅读交流")
            )))
        ))
        every { boards.favoriteBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { boards.isRefreshing } returns MutableStateFlow(false)
        every { boards.initialScrollIndex } returns 0
        every { boards.initialScrollOffset } returns 0

        val threads = mockk<ThreadListViewModel>(relaxed = true)
        every { threads.uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads = listOf(
                ThreadItem("101", "今天的版本更新体验与问题集中讨论", "北岸行舟", "42", 1_790_540_000L, replies = 36),
                ThreadItem("102", "地图路线和探索心得", "一叶书", "43", 1_790_539_000L, replies = 18),
                ThreadItem("103", "装备搭配与阅读体验", "山间晚灯", "44", 1_790_538_000L, replies = 8)
            ), subBoards = emptyList(), page = 1, totalPages = 1, recommendedOnly = false
        ))
        every { threads.subBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { threads.recommendedOnly } returns MutableStateFlow(false)
        every { threads.sort } returns MutableStateFlow(ThreadSort.LAST_REPLY)
        every { threads.isRefreshing } returns MutableStateFlow(false)
        every { threads.isFavoriteBoard } returns MutableStateFlow(false)
        every { threads.headerHiddenFraction } returns MutableStateFlow(0f)
        every { threads.visitedTids } returns MutableStateFlow(emptySet())
        every { threads.initialScrollIndex } returns 0
        every { threads.initialScrollOffset } returns 0
        every { threads.fid } returns "10"
        every { threads.stid } returns null
        every { threads.name } returns "技术讨论"
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())

        renderContent { nav ->
            ReadingPane(calculatePaneLayout(1200f, 800f, null), nav, Routes.THREADS,
                railInsets = WindowInsets(0.dp, 24.dp, 0.dp, 32.dp)) { panes ->
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(panes.listWidth.dp).fillMaxHeight()) {
                        BoardScreen(nav, boards, showBottomBar = false, selectedBoardKey = "fid:10")
                    }
                    Spacer(Modifier.width(panes.gutterWidth.dp))
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        ThreadListScreen(nav, threads, root)
                    }
                }
            }
        }
        compose.onNodeWithText("今天的版本更新体验与问题集中讨论").assertIsDisplayed()
        val rail = compose.onNodeWithTag("root-navigation-group").getUnclippedBoundsInRoot()
        check(rail.right <= 88.dp)
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "reading-thread-1200x800-light.png") }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h320dp")
    fun shortWideRailKeepsLastTabReachableAtDoubleFont() {
        renderRoot(840f, 320f, 2f, WindowInsets(0.dp, 24.dp, 0.dp, 36.dp))
        val lastTab = compose.onAllNodesWithText("我的", useUnmergedTree = true)[0]
            .performScrollTo().assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        check(lastTab.right <= 88.dp && lastTab.bottom <= 284.dp) { "Last rail tab is clipped: $lastTab" }
        val group = compose.onNodeWithTag("root-navigation-group").getUnclippedBoundsInRoot()
        check(group.left >= 10.dp && group.right <= 78.dp)
        check(group.top >= 32.dp && group.bottom <= 276.dp) {
            "Rail shadow needs room inside the safe viewport: $group"
        }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "root-rail-840x320-2x-light.png") }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun railGroupCentersInsideNonzeroSystemInsets() {
        renderRoot(1200f, 800f, 1f, WindowInsets(28.dp, 36.dp, 0.dp, 52.dp))
        val group = compose.onNodeWithTag("root-navigation-group").getUnclippedBoundsInRoot()
        val safeCenter = (36.dp + (800.dp - 52.dp)) / 2
        val groupCenter = (group.top + group.bottom) / 2
        check(group.top >= 44.dp && group.bottom <= 740.dp)
        check(group.left >= 38.dp && group.right <= 106.dp) {
            "Rail glass shadow must have horizontal space inside its safe allocation: $group"
        }
        check(abs((groupCenter - safeCenter).value) < 2f) {
            "N plus three tabs must center within safe height: $group, safe center $safeCenter"
        }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "root-rail-1200x800-insets-light.png") }
    }

    @Test
    fun railWidthKeepsTwoPanesAt840AndLeavesHingeGeometryAlone() {
        val wide = railAdjustedPaneLayout(calculatePaneLayout(840f, 600f, null), 88f)
        check(wide.isTwoPane && wide.listWidth >= 280f && wide.detailWidth >= 360f)
        check(wide.listWidth + wide.gutterWidth + wide.detailWidth == 752f)
        val tooNarrow = railAdjustedPaneLayout(calculatePaneLayout(840f, 600f, null), 230f)
        check(!tooNarrow.isTwoPane)
    }

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun selectedBoardTabOnReadingRailStillOpensDirectory() {
        lateinit var activeNav: NavHostController
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    activeNav = nav
                    NavHost(navController = nav, startDestination = Routes.BOARDS) {
                        composable(Routes.BOARDS) { Text("版块目录已打开") }
                        composable(Routes.POSTS) {
                            ReadingPane(calculatePaneLayout(1200f, 800f, null), nav, Routes.POSTS) {
                                Text("正在阅读帖子")
                            }
                        }
                    }
                    LaunchedEffect(nav) { nav.navigate(Routes.postRoute("101")) }
                }
            }
        }
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").assertIsSelected().performClick()
        compose.waitUntil(3_000) { activeNav.currentDestination?.route == Routes.BOARDS }
        compose.onNodeWithText("版块目录已打开").assertIsDisplayed()
    }

    private fun renderRoot(
        width: Float,
        height: Float,
        fontScale: Float,
        railInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.userName } returns MutableStateFlow("一位阅读论坛的用户")
        every { root.userId } returns MutableStateFlow("123456")
        renderShell(width, height, fontScale, Routes.PROFILE, railInsets) { nav -> ProfileScreen(nav, root = root) }
    }

    private fun renderShell(
        width: Float,
        height: Float,
        fontScale: Float,
        route: String,
        railInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        content: @Composable (NavHostController) -> Unit
    ) {
        renderContent(fontScale = fontScale) { nav ->
            RootPane(calculatePaneLayout(width, height, null), nav, route, railInsets) {
                content(nav)
            }
        }
    }

    private fun renderContent(
        fontScale: Float = 1f,
        content: @Composable (NavHostController) -> Unit
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NgaQingTheme(themeMode = "light") {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val nav = rememberNavController()
                        content(nav)
                    }
                }
            }
        }
    }
}
