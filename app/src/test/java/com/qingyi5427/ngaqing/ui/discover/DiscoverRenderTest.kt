package com.qingyi5427.ngaqing.ui.discover

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import androidx.core.view.drawToBitmap
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.RootPane
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.FoldBounds
import com.qingyi5427.ngaqing.ui.adaptive.FoldGeometry
import com.qingyi5427.ngaqing.ui.adaptive.V8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import android.graphics.Color
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures the production Compose screen with repository-shaped data at V8 breakpoints. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w394dp-h854dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiscoverRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val boards = listOf(
        Board("1", "数码硬件", "设备与体验"), Board("2", "游戏综合", "玩家讨论"),
        Board("3", "晴风村", "轻松闲聊"), Board("4", "技术交流", "软件与开发")
    )
    private val titles = listOf(
        "折叠屏使用一周后，我终于理解了它真正的意义",
        "这周最让你惊喜的一款独立游戏是什么？",
        "Compose 的折叠屏适配：不要把方向当成布局条件",
        "桌面整理半年总结：Less is more",
        "有哪些值得一听再听的游戏原声音乐？",
        "这几个系统交互细节，真的用过就回不去了",
        "有多少人会长期使用阅读模式和护眼设置？",
        "把掌机放在床头，意外养成了每天读书的习惯"
    )
    private val authors = listOf("晚风过境", "野原星子", "白纸工程师", "南岸的云",
        "云上漫步", "失眠收音机", "纸上星河", "科技产品")
    private val discussions = (1..8).map { index ->
        ThreadItem(index.toString(), titles[index - 1],
            authors[index - 1], index.toString(), 1_760_000_000L,
            replies = index * 12)
    }

    @Test fun phonePortraitKeepsFourBoardsAndReadableDiscussion() {
        render(394f, 854f, "v8-discover-phoneP.png")
        compose.onAllNodesWithText("发现").assertCountEquals(2)
        compose.onNodeWithText("数码硬件").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w608dp-h860dp")
    fun mediumPortraitUsesComfortableSingleColumn() {
        render(608f, 860f, "v8-discover-innerP.png")
        compose.onNodeWithText("继续阅读").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w698dp-h986dp")
    fun htmlMediumPortraitReferenceCanvas() {
        val applied = render(698f, 986f, "v8-discover-innerP-html-size.png")
        assertFalse(applied.homeTwoRegions)
        compose.onNodeWithText("继续阅读").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w986dp-h698dp")
    fun htmlWideReferenceCanvas() {
        val applied = render(986f, 698f, "v8-discover-innerL-html-size.png")
        assertTrue(applied.homeTwoRegions)
        val discussion = compose.onNodeWithText("正在讨论").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val reading = compose.onNodeWithText("继续阅读").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(discussion.right < reading.left)
    }

    @Test
    @Config(sdk = [33], qualifiers = "w860dp-h608dp")
    fun wideWindowUsesDiscussionAndReadingRegions() {
        val applied = render(860f, 608f, "v8-discover-innerL.png")
        assertTrue(applied.homeTwoRegions)
        val discussion = compose.onNodeWithText("正在讨论").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val reading = compose.onNodeWithText("继续阅读").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue("宽窗发现页两区必须左右排列", discussion.right < reading.left)
        compose.onNodeWithTag("root-navigation-group").assertDoesNotExist()
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w1300dp-h700dp")
    fun separatedFoldAppliesActualSafeSideTypographyAndColumns() {
        val fold = FoldGeometry(FoldBounds(640f, 0f, 660f, 700f),
            vertical = true, separating = true)
        val applied = render(1300f, 700f, "v8-discover-fold-safe-side.png", fold)
        assertEquals(632f, applied.availableWidthDp)
        assertTrue(applied.mediumComfortTypography)
        assertFalse(applied.homeTwoRegions)
        assertEquals(1, applied.favoriteColumns)
        compose.onNodeWithText("数码硬件").assertIsDisplayed()
    }

    /** Reuses one composition across a cover-to-inner-to-cover window resize. */
    @Test
    @Config(sdk = [33], qualifiers = "w860dp-h623dp")
    fun windowResizeKeepsDiscussionDistinctFromBoardsAndHistory() {
        val window = mutableStateOf(425 to 623)
        val nav = mockk<androidx.navigation.NavHostController>(relaxed = true)
        val vm = mockk<DiscoverViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow<DiscoverState>(DiscoverState.Ready(
            boards = boards, discussionBoard = boards.first(), discussions = discussions))
        every { vm.favoriteBoards } returns MutableStateFlow(boards.take(2))
        every { vm.history } returns MutableStateFlow(listOf(
            HistoryEntity("100", titles.first(), authors.first(), "1", lastFloor = 5)))
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                val (width, height) = window.value
                CompositionLocalProvider(LocalV8LayoutPolicy provides
                    decideV8LayoutPolicy(width.toFloat(), height.toFloat(), 1f)) {
                    Box(Modifier.size(width.dp, height.dp).testTag("resizing-home")) {
                        RootPane(calculatePaneLayout(width.toFloat(), height.toFloat(), null),
                            nav, Routes.DISCOVER) { DiscoverScreen(nav, vm) }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("常逛版块").assertIsDisplayed()
        val narrowSearch = compose.onNodeWithContentDescription("搜索帖子、版块与用户")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("narrow search touch target must be 48dp", narrowSearch.right - narrowSearch.left >= 48.dp &&
            narrowSearch.bottom - narrowSearch.top >= 48.dp)
        compose.onNodeWithContentDescription("搜索帖子、版块与用户").performClick()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-resize-cover-before.png") }

        compose.runOnUiThread { window.value = 860 to 608 }
        compose.waitForIdle()
        val discussion = compose.onNodeWithText("正在讨论").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val boardsHeading = compose.onNodeWithText("常逛版块").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val reading = compose.onNodeWithText("继续阅读").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue("wide discussion belongs in the left region", discussion.right < boardsHeading.left)
        assertTrue("wide boards precede reading in the right region", boardsHeading.bottom < reading.top)
        compose.onAllNodesWithText("正在讨论").assertCountEquals(1)
        compose.onAllNodesWithText("常逛版块").assertCountEquals(1)
        compose.onAllNodesWithText("继续阅读").assertCountEquals(1)
        val wideSearch = compose.onNodeWithContentDescription("搜索帖子、版块与用户")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("wide search touch target must be 48dp", wideSearch.right - wideSearch.left >= 48.dp &&
            wideSearch.bottom - wideSearch.top >= 48.dp)
        compose.onNodeWithContentDescription("搜索帖子、版块与用户").performClick()
        verify(exactly = 2) { nav.navigate(Routes.searchRoute(null)) }
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-resize-inner-wide.png") }
        var pixels: android.graphics.Bitmap? = null
        compose.runOnUiThread { pixels = compose.activity.window.decorView.drawToBitmap() }
        val wideBitmap = requireNotNull(pixels)
        val leftBlue = darkBlueFraction(wideBitmap, 0.05f, 0.54f)
        val rightBlue = darkBlueFraction(wideBitmap, 0.61f, 0.96f)
        assertTrue("right reading card should be visibly blue: $rightBlue", rightBlue > 0.04f)
        assertTrue("left discussion must not paint a second reading card: left=$leftBlue right=$rightBlue",
            leftBlue < rightBlue * 0.45f)

        compose.runOnUiThread { window.value = 425 to 623 }
        compose.waitForIdle()
        compose.onNodeWithText("常逛版块").assertIsDisplayed()
        compose.onNodeWithContentDescription("搜索帖子、版块与用户").assertIsDisplayed()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-resize-cover-after.png") }
        compose.runOnUiThread { window.value = 860 to 608 }
        compose.waitForIdle()
        compose.onNodeWithText("正在讨论").assertIsDisplayed()
        compose.onAllNodesWithText("继续阅读").assertCountEquals(1)

        compose.runOnUiThread { window.value = 425 to 623 }
        compose.waitForIdle()
        compose.onNodeWithTag("resizing-home").performTouchInput {
            swipeUp(startY = bottom * 0.82f, endY = bottom * 0.62f)
        }
        compose.waitForIdle()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-resize-cover-scrolled.png") }
        val readingBeforeResize = compose.onNodeWithText("继续阅读").assertIsDisplayed()
            .getUnclippedBoundsInRoot().top
        compose.runOnUiThread { window.value = 860 to 608 }
        compose.waitForIdle()
        compose.runOnUiThread { window.value = 425 to 623 }
        compose.waitForIdle()
        val readingAfterResize = compose.onNodeWithText("继续阅读").assertIsDisplayed()
            .getUnclippedBoundsInRoot().top
        assertTrue("cover scroll position should survive an inner-window round trip",
            kotlin.math.abs((readingAfterResize - readingBeforeResize).value) <= 30f)
    }

    private fun darkBlueFraction(bitmap: android.graphics.Bitmap, fromX: Float, toX: Float): Float {
        var blue = 0
        var total = 0
        for (y in (bitmap.height * 0.30f).toInt() until (bitmap.height * 0.94f).toInt() step 4) {
            for (x in (bitmap.width * fromX).toInt() until (bitmap.width * toX).toInt() step 4) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val valueBlue = Color.blue(pixel)
                if (valueBlue > red + 18 && valueBlue > green + 8 && red < 185) blue++
                total++
            }
        }
        return blue.toFloat() / total
    }

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun largeTextSearchIconFitsCoverMasthead() {
        val vm = mockk<DiscoverViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow<DiscoverState>(DiscoverState.Ready(
            boards = boards, discussionBoard = boards.first(), discussions = discussions))
        every { vm.favoriteBoards } returns MutableStateFlow(boards.take(2))
        every { vm.history } returns MutableStateFlow(listOf(
            HistoryEntity("100", titles.first(), authors.first(), "1", lastFloor = 5)))
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f, 1.5f),
                    LocalV8LayoutPolicy provides decideV8LayoutPolicy(425f, 623f, 1.5f)
                ) {
                    val nav = rememberNavController()
                    Box(Modifier.size(425.dp, 623.dp)) {
                        RootPane(calculatePaneLayout(425f, 623f, null, 1.5f),
                            nav, Routes.DISCOVER) { DiscoverScreen(nav, vm) }
                    }
                }
            }
        }
        compose.waitForIdle()
        val search = compose.onNodeWithContentDescription("搜索帖子、版块与用户")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(search.right - search.left >= 48.dp && search.bottom - search.top >= 48.dp)
        assertTrue("large-text search target must stay inside cover", search.left >= 0.dp &&
            search.right <= 425.dp && search.top >= 0.dp && search.bottom <= 623.dp)
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, "v8-search-cover-font1p5.png") }
    }

    @Test
    fun offlineHomeShowsChineseExplanationAndRetry() {
        val vm = mockk<DiscoverViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow<DiscoverState>(DiscoverState.Error(
            "Unable to resolve host \"nga.178.com\""))
        every { vm.favoriteBoards } returns MutableStateFlow(emptyList())
        every { vm.history } returns MutableStateFlow(emptyList())
        compose.setContent {
            NgaQingTheme {
                val nav = rememberNavController()
                RootPane(calculatePaneLayout(394f, 854f, null), nav, Routes.DISCOVER) {
                    DiscoverScreen(nav, vm)
                }
            }
        }
        compose.onNodeWithText("当前无法连接 NGA，请检查网络后重试").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
    }

    private fun render(width: Float, height: Float, filename: String,
        fold: FoldGeometry? = null): V8LayoutPolicy {
        var applied: V8LayoutPolicy? = null
        val vm = mockk<DiscoverViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow<DiscoverState>(DiscoverState.Ready(
            boards = boards, discussionBoard = boards.first(), discussions = discussions
        ))
        every { vm.favoriteBoards } returns MutableStateFlow(emptyList())
        every { vm.history } returns MutableStateFlow(listOf(
            HistoryEntity("100", titles.first(), authors.first(), "1", lastFloor = 5)
        ))
        compose.setContent {
            NgaQingTheme {
                CompositionLocalProvider(LocalV8LayoutPolicy provides
                    decideV8LayoutPolicy(width, height, 1f, fold)) {
                    val nav = rememberNavController()
                    RootPane(calculatePaneLayout(width, height, fold), nav, Routes.DISCOVER) {
                        applied = LocalV8LayoutPolicy.current
                        DiscoverScreen(nav, vm)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, filename) }
        return requireNotNull(applied)
    }
}
