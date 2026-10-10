package com.qingyi5427.ngaqing.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.AccountDisplay
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
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

/** Real Compose root-screen policy with mock public display data; no account credentials are loaded. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class ProfileEditorialRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w608dp-h860dp")
    fun mediumPortraitAtLargeFontKeepsIdentityAboveEntries() {
        render(fontScale = 1.5f)
        val identity = compose.onNodeWithTag("profile-identity").getUnclippedBoundsInRoot()
        val entries = compose.onNodeWithTag("profile-entries").getUnclippedBoundsInRoot()
        check(identity.bottom <= entries.top) { "Large-text profile must be one column: $identity, $entries" }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-profile-608x860-font1p5.png")
        }
    }

    @Test
    @Config(qualifiers = "w986dp-h698dp")
    fun roomyLandscapeGivesIdentityAndEntriesReadableColumns() {
        render(fontScale = 1f)
        val identity = compose.onNodeWithTag("profile-identity").getUnclippedBoundsInRoot()
        val entries = compose.onNodeWithTag("profile-entries").getUnclippedBoundsInRoot()
        check(identity.right <= entries.left) { "Roomy profile columns overlap: $identity, $entries" }
        check(identity.right.value - identity.left.value >= 300f &&
            entries.right.value - entries.left.value >= 400f) {
            "Profile columns are too narrow: $identity, $entries"
        }
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
        compose.onNodeWithText("发现").assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-profile-986x698-light.png")
        }
    }

    @Test
    @Config(qualifiers = "w986dp-h400dp")
    fun shortLandscapeFallsBackToOneColumn() {
        render(fontScale = 1f)
        val identity = compose.onNodeWithTag("profile-identity").getUnclippedBoundsInRoot()
        val entries = compose.onNodeWithTag("profile-entries").getUnclippedBoundsInRoot()
        check(identity.bottom <= entries.top) { "Short landscape must not squeeze profile cards side by side" }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-profile-986x400-short.png")
        }
    }

    private fun render(fontScale: Float) {
        val model = mockk<RootViewModel>(relaxed = true) {
            every { accountDisplay } returns MutableStateFlow(
                AccountDisplay("123456", "示例论坛用户", "", 4L))
            every { profileLoad } returns MutableStateFlow(ProfileLoadState())
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NgaQingTheme(themeMode = "light") {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val policy = decideV8LayoutPolicy(maxWidth.value, maxHeight.value, fontScale)
                        CompositionLocalProvider(
                            LocalV8LayoutPolicy provides policy,
                            LocalRootNavigationRail provides (policy.availableWidthDp >= 840f)
                        ) {
                            ProfileScreen(rememberNavController(), root = model)
                        }
                    }
                }
            }
        }
    }
}
