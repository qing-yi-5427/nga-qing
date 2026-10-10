package com.qingyi5427.ngaqing.ui.primary

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.AccountDisplay
import com.qingyi5427.ngaqing.ui.RootPane
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.profile.ProfileScreen
import com.qingyi5427.ngaqing.ui.resolvePostRouteLayout
import com.qingyi5427.ngaqing.ui.root.ProfileLoadState
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

/** V8 root navigation and reader-source behavior at real Compose window sizes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class RootPaneRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w1200dp-h800dp")
    fun wideProfileUsesMastheadWithoutPermanentRail() {
        renderProfile(1200f, 800f, 1f)
        compose.onNodeWithText("NGA Qing", substring = true).assertIsDisplayed()
        compose.onNodeWithText("一位阅读论坛的用户").assertIsDisplayed()
        compose.onNodeWithTag("root-navigation-group").assertDoesNotExist()
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h320dp")
    fun shortWideWindowKeepsBottomTabsReachableAtDoubleFont() {
        renderProfile(840f, 320f, 2f)
        val tab = compose.onNodeWithTag("root-tab-${Routes.PROFILE}")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        check(tab.left >= 0.dp && tab.right <= 840.dp && tab.bottom <= 320.dp)
    }

    @Test
    fun directPostHasNoUnavailableFocusAction() {
        val wide = calculatePaneLayout(986f, 698f, null)
        check(wide.isTwoPane)
        check(!resolvePostRouteLayout(wide, hasThreadSource = false, focusMode = false).canFocus)
        check(!resolvePostRouteLayout(wide, hasThreadSource = false, focusMode = true).showDual)
        check(resolvePostRouteLayout(wide, hasThreadSource = true, focusMode = false).showDual)
        check(resolvePostRouteLayout(wide, hasThreadSource = true, focusMode = true).canFocus)
        check(!resolvePostRouteLayout(wide, hasThreadSource = true, focusMode = true).showDual)
        val narrow = calculatePaneLayout(608f, 860f, null)
        check(!resolvePostRouteLayout(narrow, hasThreadSource = true, focusMode = false).canFocus)
    }

    private fun renderProfile(width: Float, height: Float, fontScale: Float) {
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.userName } returns MutableStateFlow("一位阅读论坛的用户")
        every { root.userId } returns MutableStateFlow("123456")
        every { root.accountDisplay } returns MutableStateFlow(
            AccountDisplay("123456", "一位阅读论坛的用户", "", 1L))
        every { root.profileLoad } returns MutableStateFlow(ProfileLoadState())
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NgaQingTheme(themeMode = "light") {
                    Surface(Modifier.fillMaxSize()) {
                        val nav = rememberNavController()
                        RootPane(calculatePaneLayout(width, height, null), nav, Routes.PROFILE) {
                            ProfileScreen(nav, root = root)
                        }
                    }
                }
            }
        }
    }
}
