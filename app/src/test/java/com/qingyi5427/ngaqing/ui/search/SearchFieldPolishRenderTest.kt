package com.qingyi5427.ngaqing.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
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

/** Software-rendered Compose states; screenshots are not proof of device GPU output. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchFieldPolishRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun coverFieldFitsWhenEmptyFocusedAndSubmittedByIme() {
        exercise(widthDp = 425, fontScale = 1f, suffix = "cover")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h480dp")
    fun narrowLargeTextFieldFitsAndSubmits() {
        exercise(widthDp = 320, fontScale = 1.5f, suffix = "narrow-font1p5")
    }

    private fun exercise(widthDp: Int, fontScale: Float, suffix: String) {
        val vm = mockk<SearchViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow<SearchUiState>(SearchUiState.Empty)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NgaQingTheme(themeMode = "light") {
                    Surface(Modifier.fillMaxSize()) {
                        SearchScreen(rememberNavController(), fid = null, stid = null, viewModel = vm)
                    }
                }
            }
        }

        val shell = compose.onNodeWithTag("search-field-shell").assertIsDisplayed()
        val field = compose.onNodeWithTag("search-field").assertIsDisplayed()
        val searchButton = compose.onNodeWithContentDescription("搜索")
        searchButton.assertIsNotEnabled()
        assertFieldGeometry(widthDp)
        screenshot("v8-122-search-$suffix-empty.png")

        field.performClick().assertIsFocused()
        assertFieldGeometry(widthDp)
        screenshot("v8-122-search-$suffix-focused-empty.png")

        field.performTextInput("折叠屏")
        searchButton.assertIsEnabled()
        assertFieldGeometry(widthDp)
        screenshot("v8-122-search-$suffix-focused-text.png")
        field.performImeAction()
        verify(exactly = 1) { vm.search("折叠屏", null, null) }

        // Both states use one border and the same safe width. Screenshot review checks pixels.
        val shellBounds = shell.getUnclippedBoundsInRoot()
        check(shellBounds.right - shellBounds.left > 200.dp)
    }

    private fun assertFieldGeometry(widthDp: Int) {
        val shell = compose.onNodeWithTag("search-field-shell").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("search-field").getUnclippedBoundsInRoot()
        check(shell.left >= 12.dp && shell.right <= (widthDp - 12).dp) {
            "Search shell escapes horizontal safe area: $shell"
        }
        check(field.left >= shell.left && field.right <= shell.right &&
            field.top >= shell.top && field.bottom <= shell.bottom) {
            "Search field must fit its single visual shell: field=$field shell=$shell"
        }
        check(field.bottom - field.top >= 48.dp) { "Search target is too short: $field" }
    }

    private fun screenshot(name: String) {
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, name) }
    }
}
