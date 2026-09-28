package com.qingyi5427.ngaqing.ui.primary

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.ReadingPane
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.post.PostBlock
import com.qingyi5427.ngaqing.ui.post.PostRenderData
import com.qingyi5427.ngaqing.ui.post.PostScreen
import com.qingyi5427.ngaqing.ui.post.PostUiState
import com.qingyi5427.ngaqing.ui.post.PostViewModel
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
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

/** Exercises the production reading screens with real Compose layout and injected screen state. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h568dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrimaryScreensRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun wideReadingShowsSelectedThreadAndProductionPostSideBySide() {
        val subject = "周末探索路线与地图标记"
        val threads = mockk<ThreadListViewModel>(relaxed = true)
        every { threads.uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads = listOf(
                ThreadItem("101", subject, "北岸行舟", "42", 1_790_540_000L, replies = 36),
                ThreadItem("102", "几处任务线索的整理", "一叶书", "43", 1_790_539_000L, replies = 18),
                ThreadItem("103", "装备搭配与操作手感", "山间晚灯", "44", 1_790_538_000L, replies = 8),
                ThreadItem("104", "剧情末段的小细节", "远处星河", "45", 1_790_537_000L, replies = 12),
                ThreadItem("105", "本周活动组队时间", "夏日海盐", "46", 1_790_536_000L, replies = 24),
                ThreadItem("106", "掌机模式的按键习惯", "纸上山海", "47", 1_790_535_000L, replies = 7)
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
        every { threads.name } returns "探索交流"

        val post = Post("101", "201", "北岸行舟", "42", subject, 1_790_540_000L, 0,
            "周末把走过的路线重新标了一遍。先从营地向北，沿着河岸走到旧桥，再转向西边的山路。地图上的几处岔口容易错过，我把顺序记在这里。")
        val reply = Post("101", "202", "一叶书", "43", "", 1_790_540_600L, 1,
            "旧桥旁边还有一条较短的小路。晴天视野很好，路牌也更容易辨认。")
        val posts = mockk<PostViewModel>(relaxed = true)
        every { posts.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post, reply), page = 1, totalPages = 1,
            renderData = mapOf(
                "201" to PostRenderData(listOf(PostBlock.Text(post.content),
                    PostBlock.Text("补充：回程可以从西侧入口绕回广场，能避开人多的主路。")), emptyList(), emptyList()),
                "202" to PostRenderData(listOf(PostBlock.Text(reply.content)), emptyList(), emptyList())
            )
        ))
        every { posts.subject } returns MutableStateFlow(subject)
        every { posts.isFavorite } returns MutableStateFlow(false)
        every { posts.isLoadingMore } returns MutableStateFlow(false)
        every { posts.loadError } returns MutableStateFlow<String?>(null)
        every { posts.isRefreshing } returns MutableStateFlow(false)
        every { posts.targetFloor } returns MutableStateFlow<Int?>(null)
        every { posts.totalRows } returns MutableStateFlow(2)
        every { posts.onlyAuthor } returns MutableStateFlow(false)
        every { posts.replyTarget } returns MutableStateFlow(null)
        every { posts.draftContent } returns MutableStateFlow("")
        every { posts.watching } returns MutableStateFlow(false)
        every { posts.ngaDomain } returns MutableStateFlow("bbs.nga.cn")
        every { posts.webCookiesReady } returns MutableStateFlow(false)
        every { posts.replying } returns MutableStateFlow(false)
        every { posts.replyResult } returns MutableStateFlow<String?>(null)
        every { posts.replySucceeded } returns MutableStateFlow(false)
        every { posts.favoriteError } returns MutableStateFlow<String?>(null)

        val root = mockk<RootViewModel>(relaxed = true)
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        render { nav ->
            ReadingPane(calculatePaneLayout(1200f, 800f, null), nav, Routes.POSTS,
                railInsets = WindowInsets(0.dp, 24.dp, 0.dp, 32.dp)) { readingPanes ->
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(readingPanes.listWidth.dp).fillMaxHeight()) {
                        ThreadListScreen(nav, threads, root, selectedTid = "101")
                    }
                    Spacer(Modifier.width(readingPanes.gutterWidth.dp))
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        PostScreen(nav, posts, dark = false)
                    }
                }
            }
        }
        compose.onNode(isSelected() and hasText(subject)).assertIsDisplayed()
        val selectedTitle = compose.onNode(isSelected() and hasText(subject)).getUnclippedBoundsInRoot()
        val readingText = compose.onNodeWithText(post.content).assertIsDisplayed().getUnclippedBoundsInRoot()
        val replyAction = compose.onNodeWithText("回复").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(selectedTitle.right < readingText.left)
        check(replyAction.left > selectedTitle.right && replyAction.right <= 1200.dp)
        check(compose.onNodeWithTag("root-navigation-group").getUnclippedBoundsInRoot().right <= 88.dp)
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-reading-1200x800-light.png")
        }
    }

    @Test fun threadListShowsRowsAndErrorAtNarrowWidth() {
        val state = MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads = listOf(
                ThreadItem("101", "这是一篇需要换行的主题标题", "测试作者", "42", 1_790_540_000L, replies = 5),
                ThreadItem("102", "更新后的地图路线和探索心得", "北岸行舟", "43", 1_790_539_000L, replies = 42),
                ThreadItem("103", "装备搭配思路，欢迎补充", "一叶书", "44", 1_790_538_000L, replies = 28),
                ThreadItem("104", "周末组队时间与玩法讨论", "山间晚灯", "45", 1_790_537_000L, replies = 16),
                ThreadItem("105", "关于剧情末段的几个细节", "远处星河", "46", 1_790_536_000L, replies = 9),
                ThreadItem("106", "桌面与掌机的操作体验", "夏日海盐", "47", 1_790_535_000L, replies = 25)
            ),
            subBoards = emptyList(), page = 1, totalPages = 1, recommendedOnly = false
        ))
        val vm = mockk<ThreadListViewModel>(relaxed = true)
        val root = mockk<RootViewModel>(relaxed = true)
        every { vm.uiState } returns state
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
        every { vm.name } returns "测试版块"
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        render { nav -> ThreadListScreen(nav, vm, root, selectedTid = "101") }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-thread-320-light-normal.png")
        }
        compose.onNodeWithText("测试版块").assertIsDisplayed()
        val threadTitle = compose.onNodeWithText("这是一篇需要换行的主题标题")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(threadTitle.right - threadTitle.left > 80.dp) {
            "Native text width was ${threadTitle.right - threadTitle.left}"
        }
        compose.onNodeWithContentDescription("搜索本版").assertIsDisplayed()
        compose.runOnUiThread { state.value = ThreadUiState.Error("", "连接失败") }
        compose.onNodeWithText("连接失败").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun shortThreadWindowKeepsListScrollableAtDoubleFont() {
        val vm = mockk<ThreadListViewModel>(relaxed = true)
        val root = mockk<RootViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<ThreadUiState>(ThreadUiState.Success(
            threads = (1..8).map { ThreadItem("$it", "主题 $it：阅读与讨论", "作者 $it", "42", 1_740_000_000L, replies = it) },
            subBoards = emptyList(), page = 1, totalPages = 1, recommendedOnly = false
        ))
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
        every { vm.name } returns "短窗口版块"
        every { root.blacklistUsers } returns MutableStateFlow(emptySet())
        every { root.blacklistKeywords } returns MutableStateFlow(emptySet())
        render(fontScale = 2f) { nav -> ThreadListScreen(nav, vm, root) }
        compose.onNodeWithText("发主题").assertIsDisplayed()
        compose.onNodeWithText("主题 1：阅读与讨论").performScrollTo().assertIsDisplayed()
    }

    @Test fun postContentAndReplyRemainReachableInDarkLargeText() {
        val post = Post("101", "201", "这是一个非常非常长的作者昵称", "42", "帖子标题", 1L, 0, "正文")
        val laterPost = Post("101", "202", "AnExceptionallyLongAuthorNameWithoutSpaces", "43", "", 2L, 370320, "后续正文")
        val vm = mockk<PostViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post, laterPost), page = 1, totalPages = 123456,
            renderData = mapOf(
                "201" to PostRenderData(listOf(PostBlock.Text("正文内容")), emptyList(), emptyList()),
                "202" to PostRenderData(listOf(
                    PostBlock.Text("后续正文"),
                    PostBlock.Text("这一页继续记录探索路线：沿河向北走到旧桥，再顺着山路回到营地。几个岔口的标记需要逐一核对，避免错过隐藏的入口。"),
                    PostBlock.Text("回程可以经过西侧的小路，那里树荫多一些。路牌在弯道后面，靠近时才看得清，第一次走最好提前留意。"),
                    PostBlock.Text("如果在旧桥附近停留，建议顺便看一眼河对岸的石阶。石阶尽头有一处开阔的平台，能把这一段山路的走向看得更清楚；傍晚的光线也很适合辨认远处的路标。")
                ), emptyList(), emptyList())
            )
        ))
        every { vm.subject } returns MutableStateFlow("帖子标题")
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns MutableStateFlow<Int?>(null)
        every { vm.totalRows } returns MutableStateFlow(1)
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
        render(themeMode = "dark", fontScale = 2f) { nav -> PostScreen(nav, vm, dark = true) }
        compose.onNodeWithContentDescription("翻页，当前第 1 页，共 123456 页").assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320-dark-2x.png")
        }
        compose.onNodeWithText("正文内容").assertIsDisplayed()
        val ownerBadge = compose.onNodeWithText("楼主").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(ownerBadge.right - ownerBadge.left > 24.dp) {
            "Native Chinese badge width was ${ownerBadge.right - ownerBadge.left}"
        }
        val ownerFloor = compose.onNodeWithText("#0").assertIsDisplayed().getUnclippedBoundsInRoot()
        val ownerMenu = compose.onNodeWithContentDescription("0楼操作").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(ownerBadge.right <= ownerFloor.left && ownerFloor.right <= ownerMenu.left)
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        compose.onNodeWithText("#370320").assertIsDisplayed()
        val longPageAction = compose.onNodeWithContentDescription("翻页，当前第 12345 页，共 123456 页")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(longPageAction.right - longPageAction.left in 48.dp..86.dp)
        compose.onNodeWithText("12345", substring = false).assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320-dark-2x-long-page.png")
        }
        val laterFloor = compose.onNodeWithText("#370320").getUnclippedBoundsInRoot()
        val laterMenu = compose.onNodeWithContentDescription("370320楼操作").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(laterFloor.right <= laterMenu.left)
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(0)
        compose.onNodeWithText("楼主").assertIsDisplayed()
        compose.onNodeWithContentDescription("0楼操作").performClick()
        compose.onNodeWithText("回复这层").performClick()
        compose.onNodeWithText("发送").assertIsDisplayed()
    }

    @Test fun postReadingFlowAtNormalTextSize() {
        val post = Post("101", "201", "普通作者", "42", "一篇正常阅读的主题", 1_740_000_000L, 0,
            "周末整理了这份使用体验。连续读了几天之后，最明显的变化是信息更容易扫读，长段落也不会被每层的外框打断。")
        val reply = Post("101", "202", "另一位用户", "43", "", 1_740_000_600L, 1,
            "这个观察很有帮助。我也试了相同的阅读顺序，引用和链接都应保持清楚，但不要抢过正文。")
        val vm = mockk<PostViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post, reply), page = 1, totalPages = 1,
            renderData = mapOf(
                "201" to PostRenderData(listOf(
                    PostBlock.Text(post.content),
                    PostBlock.Text("第二段补充一点：在窄屏上，标题、作者、正文和操作需要各有位置。翻页时仍能看到楼层边界，选中文字也不会被装饰覆盖。"),
                    PostBlock.Link("查看相关讨论", "https://bbs.nga.cn/")
                ), emptyList(), emptyList()),
                "202" to PostRenderData(listOf(
                    PostBlock.Quote(listOf(PostBlock.Text("长段落也不会被每层的外框打断。")), refName = "普通作者", floor = 0),
                    PostBlock.Text(reply.content)
                ), emptyList(), emptyList())
            )
        ))
        every { vm.subject } returns MutableStateFlow("一篇正常阅读的主题")
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns MutableStateFlow<Int?>(null)
        every { vm.totalRows } returns MutableStateFlow(2)
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
        render { nav -> PostScreen(nav, vm, dark = false) }
        compose.onNodeWithText(post.content).assertIsDisplayed()
        val authorAction = compose.onNodeWithContentDescription("只看楼主")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val pageAction = compose.onNodeWithContentDescription("翻页，当前第 1 页，共 1 页")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val replyAction = compose.onNodeWithText("回复").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(authorAction.right - authorAction.left >= 48.dp)
        check(pageAction.right - pageAction.left >= 48.dp)
        check(pageAction.left >= authorAction.right && replyAction.left >= pageAction.right)
        check(replyAction.right - authorAction.left < 240.dp) {
            "Reading actions should float at intrinsic width, not occupy a full footer"
        }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320-light-normal.png")
        }
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        compose.onNodeWithText(reply.content).assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320-light-reply.png")
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w780dp-h390dp")
    fun landscapePostKeepsContentAndCompactActionsTogether() {
        val post = Post("101", "201", "普通作者", "42", "横屏阅读主题", 1_740_000_000L, 0,
            "横屏时正文应继续占据可用高度，操作只停在右下角。")
        val reply = Post("101", "202", "另一位用户", "43", "", 1_740_000_600L, 1,
            "翻页和回复仍然应该随时可达。")
        val vm = mockk<PostViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post, reply), page = 1, totalPages = 2,
            renderData = mapOf(
                "201" to PostRenderData(listOf(PostBlock.Text(post.content)), emptyList(), emptyList()),
                "202" to PostRenderData(listOf(PostBlock.Text(reply.content)), emptyList(), emptyList())
            )
        ))
        every { vm.subject } returns MutableStateFlow(post.subject)
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns MutableStateFlow<Int?>(null)
        every { vm.totalRows } returns MutableStateFlow(2)
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
        render { nav -> PostScreen(nav, vm, dark = false) }
        compose.onNodeWithText(post.content).assertIsDisplayed()
        val authorAction = compose.onNodeWithContentDescription("只看楼主")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val pageAction = compose.onNodeWithContentDescription("翻页，当前第 1 页，共 2 页")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val replyAction = compose.onNodeWithText("回复").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(authorAction.right - authorAction.left >= 48.dp)
        check(pageAction.right - pageAction.left >= 48.dp)
        check(replyAction.right - authorAction.left < 240.dp)
        check(replyAction.right > 700.dp) { "Reading actions should stay at the right edge" }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-780x390-light.png")
        }
        compose.onNodeWithText(reply.content).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("post-reading-list").performTouchInput { swipeUp() }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-780x390-light-end.png")
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun shortDarkPostCanMoveLastLineAboveFloatingActionsAtDoubleFont() {
        val post = Post("101", "201", "普通作者", "42", "短窗口阅读", 1_740_000_000L, 0,
            "主楼有几段讨论内容，短窗口下需要继续滚动。读完第一段以后，还应能继续找到下面的回复。")
        val reply = Post("101", "202", "另一位用户", "43", "", 1_740_000_600L, 1,
            "这是最后一行，应能滚动到浮岛上方。")
        val vm = mockk<PostViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(post, reply), page = 1, totalPages = 1,
            renderData = mapOf(
                "201" to PostRenderData(listOf(PostBlock.Text(post.content)), emptyList(), emptyList()),
                "202" to PostRenderData(listOf(PostBlock.Text(reply.content)), emptyList(), emptyList())
            )
        ))
        every { vm.subject } returns MutableStateFlow(post.subject)
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns MutableStateFlow<Int?>(null)
        every { vm.totalRows } returns MutableStateFlow(2)
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
        render(themeMode = "dark", fontScale = 2f) { nav -> PostScreen(nav, vm, dark = true) }
        compose.onNodeWithContentDescription("只看楼主").assertIsDisplayed()
        compose.onNodeWithContentDescription("翻页，当前第 1 页，共 1 页").assertIsDisplayed()
        compose.onNodeWithText("回复").assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320x320-dark-2x.png")
        }
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        compose.onNodeWithTag("post-reading-list").performTouchInput { swipeUp() }
        val lastLine = compose.onNodeWithText(reply.content).assertIsDisplayed().getUnclippedBoundsInRoot()
        val floatingAction = compose.onNodeWithContentDescription("只看楼主")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(lastLine.bottom <= floatingAction.top) {
            "Last floor must scroll fully above floating actions in a short window"
        }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "primary-post-320x320-dark-2x-end.png")
        }
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
                        Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { screen(rememberNavController()) }
                }
            }
        }
    }
}
