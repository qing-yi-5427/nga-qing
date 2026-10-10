package com.qingyi5427.ngaqing.ui.board

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
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

/** Native Compose frames with synthetic public board names; these are not phone or online-content proof. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class BoardEditorialRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w394dp-h854dp")
    fun rootPhoneKeepsSearchAndDirectoryWithFloatingNavigation() {
        render(widthDp = 394f, heightDp = 854f, fontScale = 1f, embedded = false)
        compose.onNodeWithTag("board-directory-root-header").assertIsDisplayed()
        compose.onNodeWithTag("board-directory-search").assertIsDisplayed()
        compose.onNodeWithTag("root-bottom-dock").assertIsDisplayed()
        compose.onAllNodesWithText("演示版块").onFirst().assertIsDisplayed()
        screenshot("v8-board-root-phoneP.png")
    }

    @Test
    @Config(qualifiers = "w986dp-h698dp")
    fun rootWideUsesTopTabsAndSectionJumper() {
        render(widthDp = 986f, heightDp = 698f, fontScale = 1f, embedded = false)
        compose.onNodeWithTag("board-directory-root-header").assertIsDisplayed()
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
        compose.onNodeWithText("目录概览").assertIsDisplayed()
        compose.onNodeWithText("发现").assertIsDisplayed()
        screenshot("v8-board-root-innerL.png")
    }

    @Test
    @Config(qualifiers = "w986dp-h698dp")
    fun embeddedTwoEightyDpAtLargeFontStaysCompactDespiteWideParent() {
        assertEmbeddedFits(280f)
        screenshot("v8-board-embedded-280-font1p5.png")
    }

    @Test
    @Config(qualifiers = "w986dp-h698dp")
    fun embeddedThreeFortyDpAtLargeFontStaysCompactDespiteWideParent() {
        assertEmbeddedFits(340f)
        screenshot("v8-board-embedded-340-font1p5.png")
    }

    private fun assertEmbeddedFits(widthDp: Float) {
        render(widthDp = widthDp, heightDp = 698f, fontScale = 1.5f, embedded = true)
        compose.onNodeWithTag("board-directory-compact-header").assertIsDisplayed()
        compose.onNodeWithTag("board-directory-root-header").assertDoesNotExist()
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
        compose.onNodeWithText("目录概览").assertDoesNotExist()
        compose.onNodeWithContentDescription("全站搜索").assertIsDisplayed()
        val compact = compose.onNodeWithTag("board-directory-compact-header")
            .getUnclippedBoundsInRoot()
        check(compact.right <= widthDp.dp && compact.bottom < 130.dp) {
            "Embedded header must fit the local pane: $compact"
        }
        val favorite = compose.onAllNodesWithText("演示版块").onFirst()
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(favorite.right <= widthDp.dp) {
            "Favorite board text exceeds the local pane: $favorite, width=$widthDp"
        }
    }

    private fun render(widthDp: Float, heightDp: Float, fontScale: Float, embedded: Boolean) {
        val board = Board("10", "演示版块", info = "真实字段可展示的版块介绍")
        val groups = listOf(BoardGroup("社区分类", "社区分类", children = listOf(
            board, Board("11", "另一个讨论版块", info = "讨论与交流"))))
        val vm = mockk<BoardViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow<BoardUiState>(BoardUiState.Success(groups))
            every { favoriteBoards } returns MutableStateFlow(listOf(board))
            every { isRefreshing } returns MutableStateFlow(false)
            every { initialScrollIndex } returns 0
            every { initialScrollOffset } returns 0
        }
        compose.setContent {
            val systemDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides
                Density(systemDensity.density, fontScale)) {
                NgaQingTheme(themeMode = "light") {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val inheritedPolicy = decideV8LayoutPolicy(maxWidth.value,
                            maxHeight.value, fontScale)
                        CompositionLocalProvider(
                            LocalV8LayoutPolicy provides inheritedPolicy,
                            LocalRootNavigationRail provides (!embedded &&
                                inheritedPolicy.availableWidthDp >= 840f)
                        ) {
                            if (embedded) Box(Modifier.width(widthDp.dp).fillMaxHeight()) {
                                BoardScreen(rememberNavController(), vm, showBottomBar = false)
                            } else BoardScreen(rememberNavController(), vm)
                        }
                    }
                }
            }
        }
    }

    private fun screenshot(name: String) {
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, name) }
    }
}
