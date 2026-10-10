package com.qingyi5427.ngaqing.ui.discover

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.navigation.compose.rememberNavController
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
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
