package com.qingyi5427.ngaqing.ui.design

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h568dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NeutralRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shortWindowStillShowsErrorAndRetryInBothThemes() {
        var mode by mutableStateOf("light")
        compose.setContent {
            NgaQingTheme(themeMode = mode) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    NgaStatePanel("加载失败", "可以检查连接后重试。", "重试", onAction = {})
                }
            }
        }
        compose.onNodeWithText("加载失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "native-state-panel-320x568-light.png")
        }
        compose.runOnUiThread { mode = "dark" }
        compose.onNodeWithText("加载失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "native-state-panel-320x568-dark.png")
        }
    }
}
