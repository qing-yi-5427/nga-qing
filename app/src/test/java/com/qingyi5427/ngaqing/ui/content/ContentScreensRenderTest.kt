package com.qingyi5427.ngaqing.ui.content

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.DraftEntity
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.board.BoardScreen
import com.qingyi5427.ngaqing.ui.board.BoardUiState
import com.qingyi5427.ngaqing.ui.board.BoardViewModel
import com.qingyi5427.ngaqing.ui.compose.NewTopicScreen
import com.qingyi5427.ngaqing.ui.compose.NewTopicViewModel
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.favorites.FavoritesScreen
import com.qingyi5427.ngaqing.ui.favorites.FavoritesViewModel
import com.qingyi5427.ngaqing.ui.history.HistoryScreen
import com.qingyi5427.ngaqing.ui.history.HistoryViewModel
import com.qingyi5427.ngaqing.ui.search.SearchScreen
import com.qingyi5427.ngaqing.ui.search.SearchUiState
import com.qingyi5427.ngaqing.ui.search.SearchViewModel
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertTrue

/** Production screens, rendered at narrow phone width with real Compose semantics. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w320dp-h568dp")
class ContentScreensRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun boardShowsSelectedContentLoadingAndRetry() {
        val board = Board(fid = "10", name = "版块 A", info = "版块说明")
        val state = MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(BoardGroup("分类", "组", children = listOf(board, Board("11", "版块 B"))))
        ))
        val vm = mockk<BoardViewModel>(relaxed = true)
        every { vm.uiState } returns state
        every { vm.favoriteBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.initialScrollIndex } returns 0
        every { vm.initialScrollOffset } returns 0
        render { nav -> BoardScreen(nav, vm, showBottomBar = false, selectedBoardKey = "fid:10") }
        compose.onNodeWithText("版块 A").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("版块 B").assertIsDisplayed().assertIsNotSelected()
        compose.onNodeWithText("版块说明").assertIsDisplayed()
        screenshot("board-phone-light.png")
        compose.runOnUiThread { state.value = BoardUiState.Success(emptyList()) }
        compose.onNodeWithText("暂无版块").assertIsDisplayed()
        compose.runOnUiThread { state.value = BoardUiState.Loading }
        compose.onNodeWithText("版块 A").assertDoesNotExist()
        compose.runOnUiThread { state.value = BoardUiState.Error(null, "无法连接") }
        compose.onNodeWithText("版块加载失败").assertIsDisplayed()
        compose.onNodeWithText("无法连接").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        verify { vm.load() }
    }

    @Test fun favoritesKeepsRowsFiltersAndFailureActions() {
        val rows = MutableStateFlow(listOf(FavoriteEntity("1", "收藏主题", "10", "作者")))
        val folders = MutableStateFlow(listOf("默认"))
        val syncError = MutableStateFlow<String?>(null)
        val vm = mockk<FavoritesViewModel>(relaxed = true)
        every { vm.favorites } returns rows
        every { vm.folders } returns folders
        every { vm.syncing } returns MutableStateFlow(false)
        every { vm.syncMessage } returns MutableStateFlow<String?>(null)
        every { vm.syncError } returns syncError
        every { vm.actionError } returns MutableStateFlow<String?>(null)
        render(themeMode = "dark") { nav -> FavoritesScreen(nav, vm) }
        compose.onNodeWithText("收藏主题").assertIsDisplayed()
        compose.onNodeWithText("全部").assertIsDisplayed()
        compose.onNodeWithContentDescription("取消收藏 收藏主题").assertIsDisplayed()
        screenshot("favorites-phone-dark.png")
        compose.runOnUiThread { rows.value = emptyList() }
        compose.onNodeWithText("还没有收藏").assertIsDisplayed()
        compose.runOnUiThread { syncError.value = "第 2 页失败" }
        compose.onNodeWithText("收藏同步失败").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        verify { vm.syncFromServer() }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h568dp")
    fun favoritesLargeTextKeepsFolderAndDeleteActionsReachable() {
        val vm = mockk<FavoritesViewModel>(relaxed = true)
        every { vm.favorites } returns MutableStateFlow(listOf(
            FavoriteEntity("1", "收藏主题", "10", "作者", folder = "比较长的收藏分组名称")
        ))
        every { vm.folders } returns MutableStateFlow(listOf("其他"))
        every { vm.syncing } returns MutableStateFlow(false)
        every { vm.syncMessage } returns MutableStateFlow<String?>(null)
        every { vm.syncError } returns MutableStateFlow<String?>(null)
        every { vm.actionError } returns MutableStateFlow<String?>(null)
        render(themeMode = "dark", fontScale = 2f) { nav -> FavoritesScreen(nav, vm) }
        // Bring the full row above the floating dock; the list remains scrollable beneath it.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        screenshot("favorites-320-dark-2x-row.png")
        val folder = compose.onNodeWithText("比较长的收藏分组名称")
        folder.assertIsDisplayed().performClick()
        compose.onNodeWithText("移动收藏").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        val delete = compose.onNodeWithContentDescription("取消收藏 收藏主题").performScrollTo()
        delete.assertIsDisplayed().performClick()
        compose.onNodeWithText("取消收藏？").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun favoritesSearchRetainsFocusAcrossEmptyResultsAndBottomNavWorks() {
        val saved = FavoriteEntity("1", "收藏主题", "10", "作者")
        val rows = MutableStateFlow(listOf(saved))
        val vm = mockk<FavoritesViewModel>(relaxed = true)
        every { vm.favorites } returns rows
        every { vm.folders } returns MutableStateFlow<List<String>>(emptyList())
        every { vm.syncing } returns MutableStateFlow(false)
        every { vm.syncMessage } returns MutableStateFlow<String?>(null)
        every { vm.syncError } returns MutableStateFlow<String?>(null)
        every { vm.actionError } returns MutableStateFlow<String?>(null)
        render(fontScale = 2f) { nav ->
            NavHost(navController = nav, startDestination = Routes.FAVORITES) {
                composable(Routes.FAVORITES) { FavoritesScreen(nav, vm) }
                composable(Routes.PROFILE) { Text("个人页目标") }
                composable(Routes.BOARDS) { Text("版块页目标") }
            }
        }
        val search = compose.onAllNodes(hasSetTextAction()).onFirst()
        search.performScrollTo().performTextInput("甲")
        search.assertIsFocused()
        compose.runOnUiThread { rows.value = emptyList() }
        search.assertIsFocused()
        search.performTextInput("乙")
        compose.runOnUiThread { rows.value = listOf(saved) }
        search.assertIsFocused()
        search.performTextInput("丙")
        verify { vm.setQuery(match { it.contains("甲") && it.contains("乙") && it.contains("丙") }) }
        compose.onNodeWithText("我的").assertIsDisplayed().performClick()
        compose.onNodeWithText("个人页目标").assertIsDisplayed()
    }

    @Test fun historyShowsContentEmptyAndConfirmation() {
        val rows = MutableStateFlow(listOf(HistoryEntity("1", "历史主题", "作者", "10", lastFloor = 3)))
        val vm = mockk<HistoryViewModel>(relaxed = true)
        every { vm.history } returns rows
        render { nav -> HistoryScreen(nav, vm) }
        compose.onNodeWithText("历史主题").assertIsDisplayed()
        compose.onNodeWithContentDescription("删除浏览记录 历史主题").assertIsDisplayed()
        screenshot("history-phone-light.png")
        compose.onNodeWithContentDescription("清空历史").performClick()
        compose.onNodeWithText("清空浏览历史？").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.runOnUiThread { rows.value = emptyList() }
        compose.onNodeWithText("还没有浏览记录").assertIsDisplayed()
    }

    @Test fun searchShowsOfflineContentEmptyAndErrorRetry() {
        val state = MutableStateFlow<SearchUiState>(SearchUiState.Success(
            listOf(ThreadItem("1", "搜索结果", "作者", "42", System.currentTimeMillis(), replies = 2)), fromCache = true
        ))
        val vm = mockk<SearchViewModel>(relaxed = true)
        every { vm.uiState } returns state
        render(themeMode = "dark") { nav -> SearchScreen(nav, fid = null, stid = null, viewModel = vm) }
        compose.onNodeWithText("搜索结果").assertIsDisplayed()
        compose.onNodeWithText("当前显示离线搜索结果").assertIsDisplayed()
        screenshot("search-phone-dark.png")
        compose.runOnUiThread { state.value = SearchUiState.Success(emptyList()) }
        compose.onNodeWithText("没有找到相关主题").assertIsDisplayed()
        compose.runOnUiThread { state.value = SearchUiState.Loading }
        compose.onNodeWithText("没有找到相关主题").assertDoesNotExist()
        compose.runOnUiThread { state.value = SearchUiState.Error("", "搜索网络错误") }
        compose.onNodeWithText("搜索失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test fun newTopicRetainsInputThroughBusyStateInDarkLargeText() {
        val publishing = MutableStateFlow(false)
        val result = MutableStateFlow<String?>(null)
        val vm = mockk<NewTopicViewModel>(relaxed = true)
        every { vm.draft } returns MutableStateFlow<DraftEntity?>(DraftEntity("new:10:", "new", subject = "草稿标题"))
        every { vm.draftLoaded } returns MutableStateFlow(true)
        every { vm.publishing } returns publishing
        every { vm.result } returns result
        every { vm.publishSucceeded } returns MutableStateFlow(false)
        every { vm.ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { vm.boardName } returns "测试版块"
        every { vm.fid } returns "10"
        every { vm.stid } returns null
        render(themeMode = "dark", fontScale = 1.3f) { nav -> NewTopicScreen(nav, vm) }
        compose.onNodeWithText("发布到 测试版块").assertIsDisplayed()
        compose.onNodeWithText("草稿标题").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("补充")
        compose.runOnUiThread { publishing.value = true }
        compose.onNodeWithText("正在提交，请稍候").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread { publishing.value = false; result.value = "服务器没有返回明确的成功结果" }
        compose.onNodeWithText("发布未完成").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("服务器没有返回明确的成功结果").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("草稿标题", substring = true).performScrollTo().assertIsDisplayed()
        verify { vm.saveDraft(match { it.contains("补充") }, any(), any()) }
    }

    @Test fun newTopicBlankEditorShowsDraftLoadingThenFields() {
        val loaded = MutableStateFlow(false)
        val vm = mockk<NewTopicViewModel>(relaxed = true)
        every { vm.draft } returns MutableStateFlow<DraftEntity?>(null)
        every { vm.draftLoaded } returns loaded
        every { vm.publishing } returns MutableStateFlow(false)
        every { vm.result } returns MutableStateFlow<String?>(null)
        every { vm.publishSucceeded } returns MutableStateFlow(false)
        every { vm.ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { vm.boardName } returns "测试版块"
        every { vm.fid } returns "10"
        every { vm.stid } returns null
        render { nav -> NewTopicScreen(nav, vm) }
        compose.onNodeWithText("正在载入草稿…").assertIsDisplayed()
        compose.runOnUiThread { loaded.value = true }
        compose.onNodeWithText("标题").assertIsDisplayed()
        compose.onNodeWithText("正文").assertIsDisplayed()
        compose.onNodeWithText("正在载入草稿…").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun newTopicShortWindowAtDoubleFontKeepsActionsAndEditorReachable() {
        val vm = mockk<NewTopicViewModel>(relaxed = true)
        every { vm.draft } returns MutableStateFlow<DraftEntity?>(
            DraftEntity("new:10:", "new", subject = "测试标题", content = "测试正文")
        )
        every { vm.draftLoaded } returns MutableStateFlow(true)
        every { vm.publishing } returns MutableStateFlow(false)
        every { vm.result } returns MutableStateFlow<String?>(null)
        every { vm.publishSucceeded } returns MutableStateFlow(false)
        every { vm.ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { vm.boardName } returns "测试版块"
        every { vm.fid } returns "10"
        every { vm.stid } returns null
        render(fontScale = 2f) { nav ->
            NavHost(navController = nav, startDestination = "compose-test") {
                composable("compose-test") { NewTopicScreen(nav, vm) }
                composable(Routes.WEB_EDITOR) { Text("高级编辑目标") }
            }
        }
        val rootBounds = compose.onRoot().getUnclippedBoundsInRoot()
        val back = compose.onNodeWithContentDescription("返回").assertIsDisplayed()
        val advanced = compose.onNodeWithText("图片/高级").assertIsDisplayed().assertIsEnabled()
        val publish = compose.onNodeWithText("发布").assertIsDisplayed().assertIsEnabled()
        val backBounds = back.getUnclippedBoundsInRoot()
        val advancedBounds = advanced.getUnclippedBoundsInRoot()
        val publishBounds = publish.getUnclippedBoundsInRoot()
        for (bounds in listOf(backBounds, advancedBounds, publishBounds)) {
            assertTrue("完整顶栏触点须位于短窗视口内: $bounds", bounds.left >= rootBounds.left &&
                bounds.top >= rootBounds.top && bounds.right <= rootBounds.right &&
                bounds.bottom <= rootBounds.bottom)
        }
        assertTrue("返回与高级入口触点不可重叠",
            backBounds.right <= advancedBounds.left || advancedBounds.right <= backBounds.left ||
                backBounds.bottom <= advancedBounds.top || advancedBounds.bottom <= backBounds.top)
        assertTrue("高级入口与发布触点不可重叠",
            advancedBounds.right <= publishBounds.left || publishBounds.right <= advancedBounds.left ||
                advancedBounds.bottom <= publishBounds.top || publishBounds.bottom <= advancedBounds.top)
        screenshot("new-topic-short-2x-light.png")
        val editor = compose.onAllNodes(hasSetTextAction()).onLast()
        editor.performScrollTo()
        editor.assertIsDisplayed()
        editor.performTextInput("短窗内容")
        editor.assertTextContains("短窗内容", substring = true)
        verify { vm.saveDraft(any(), match { it.contains("短窗内容") }, any()) }
        publish.performClick()
        verify { vm.publish("测试标题", match { it.contains("短窗内容") }) }
        advanced.performClick()
        compose.onNodeWithText("高级编辑目标").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h600dp")
    fun boardWideDirectoryShowsTwoColumnsAndFavorites() {
        val favorite = Board(fid = "9", name = "常看版块")
        val vm = mockk<BoardViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(BoardGroup("社区", "社区", children = listOf(
                Board("10", "生活交流"), Board("11", "数码讨论"), Board("12", "阅读分享")
            )))
        ))
        every { vm.favoriteBoards } returns MutableStateFlow(listOf(favorite))
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.initialScrollIndex } returns 0
        every { vm.initialScrollOffset } returns 0
        render { nav -> BoardScreen(nav, vm, showBottomBar = false, selectedBoardKey = "fid:11") }
        compose.onNodeWithText("目录概览").assertIsDisplayed()
        compose.onNodeWithText("常看版块").assertIsDisplayed()
        compose.onNodeWithText("生活交流").assertIsDisplayed()
        compose.onNodeWithText("数码讨论").assertIsDisplayed().assertIsSelected()
        screenshot("board-wide-light.png")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h600dp")
    fun boardKeepsSameVisibleBoardAcrossWideNarrowWide() {
        val boards = (0..20).map { Board(fid = "${10 + it}", name = "版块 $it") }
        val vm = mockk<BoardViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(BoardGroup("分类", "分类", children = boards))
        ))
        every { vm.favoriteBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.initialScrollIndex } returns 8
        every { vm.initialScrollOffset } returns 0
        val wide = mutableStateOf(true)
        render { nav ->
            Box(Modifier.requiredWidth(if (wide.value) 840.dp else 320.dp)) {
                BoardScreen(nav, vm, showBottomBar = false, selectedBoardKey = "fid:15")
            }
        }
        compose.onNodeWithText("版块 5").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("目录概览").assertIsDisplayed()
        compose.runOnUiThread { wide.value = false }
        compose.onNodeWithText("版块 5").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("目录概览").assertDoesNotExist()
        compose.runOnUiThread { wide.value = true }
        compose.onNodeWithText("版块 5").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("目录概览").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h600dp")
    fun boardWideDirectoryJumpsToAndReopensCategory() {
        val vm = mockk<BoardViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(
            listOf(
                BoardGroup("第一分类", "第一分类", children = (0..14).map {
                    Board(fid = "${100 + it}", name = "第一版块 $it")
                }),
                BoardGroup("第二分类", "第二分类", children = listOf(Board("200", "目标版块")))
            )
        ))
        every { vm.favoriteBoards } returns MutableStateFlow<List<Board>>(emptyList())
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.initialScrollIndex } returns 0
        every { vm.initialScrollOffset } returns 0
        render { nav -> BoardScreen(nav, vm, showBottomBar = false) }
        compose.onNodeWithText("目标版块").assertDoesNotExist()
        compose.onNodeWithContentDescription("跳转到第二分类").performClick()
        compose.onNodeWithContentDescription("跳转到第二分类").assertIsSelected()
        compose.onNodeWithText("目标版块").assertIsDisplayed()
        compose.onNodeWithContentDescription("收起第二分类").performClick()
        compose.onNodeWithText("目标版块").assertDoesNotExist()
        compose.onNodeWithContentDescription("跳转到第二分类").performClick()
        compose.onNodeWithText("目标版块").assertIsDisplayed()
    }

    private fun screenshot(filename: String) {
        compose.runOnIdle { saveNativeScreenshot(compose.activity, filename) }
    }

    private fun render(
        themeMode: String = "light",
        fontScale: Float = 1f,
        screen: @Composable (NavHostController) -> Unit
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NgaQingTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        screen(rememberNavController())
                    }
                }
            }
        }
    }
}
