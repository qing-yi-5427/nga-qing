package com.qingyi5427.ngaqing.ui.login

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks the fixed official login entry and native shell; it does not sign in to NGA. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h480dp")
class LoginEntryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun allSelectedForumDomainsUseOnlyTheFixedOfficialLoginAction() {
        NgaDomains.options.forEach { option ->
            assertEquals(
                "https://${option.host}/nuke.php?__lib=login&__act=login_ui",
                trustedLoginPageUrl(option.host)
            )
        }
        assertEquals(
            trustedLoginPageUrl(NgaDomains.DEFAULT_HOST),
            trustedLoginPageUrl("https://bbs.nga.cn.evil.example/nuke.php?__act=other")
        )
        assertEquals(
            trustedLoginPageUrl(NgaDomains.DEFAULT_HOST),
            trustedLoginPageUrl("http://not-nga.example/")
        )
    }

    @Test fun preparationHelpRefreshErrorAndCaptureRemainReachable() {
        val loginState = MutableStateFlow<LoginState>(LoginState.Idle)
        val webReadyState = MutableStateFlow(false)
        val domain = MutableStateFlow("ngabbs.com")
        val model = mockk<LoginViewModel>(relaxed = true) {
            every { state } returns loginState
            every { webReady } returns webReadyState
            every { ngaDomain } returns domain
        }
        compose.setContent {
            NgaQingTheme { LoginScreen(rememberNavController(), viewModel = model) }
        }
        compose.onNodeWithText("正在准备登录环境").assertIsDisplayed()
        compose.onNodeWithText("完成登录").assertIsNotEnabled()
        compose.onNodeWithContentDescription("登录帮助").performClick()
        compose.onNodeWithText("如何登录").assertIsDisplayed()
        compose.onNodeWithText("请在 NGA 网页使用账号登录，或让另一台设备扫描本页显示的二维码，以登录本应用。扫码后若未自动完成，请点右上角刷新，再点“完成登录”检测状态。网页内容可以双指缩放。")
            .assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()

        compose.runOnIdle {
            webReadyState.value = true
            loginState.value = LoginState.Error("未检测到登录凭证。请完成网页的账号或验证流程；也可让另一设备扫描本页二维码。扫码后若未生效，请刷新页面再检测。")
        }
        compose.onNodeWithText("正在准备登录环境").assertDoesNotExist()
        compose.onNodeWithText("未检测到登录凭证。请完成网页的账号或验证流程；也可让另一设备扫描本页二维码。扫码后若未生效，请刷新页面再检测。")
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("刷新页面").assertIsDisplayed().performClick()
        compose.onNodeWithText("完成登录").assertIsEnabled().performClick()
        verify(exactly = 1) { model.tryCapture() }
        compose.runOnUiThread {
            val web = findWebView(compose.activity.window.decorView)
                ?: error("Prepared login page has no WebView")
            assertEquals(trustedLoginPageUrl("ngabbs.com"), web.url)
        }
    }

    @Test fun leavingAddAccountLoginRestoresSavedWebCookies() {
        var visible by mutableStateOf(true)
        val model = mockk<LoginViewModel>(relaxed = true) {
            every { state } returns MutableStateFlow<LoginState>(LoginState.Idle)
            every { webReady } returns MutableStateFlow(false)
            every { ngaDomain } returns MutableStateFlow(NgaDomains.DEFAULT_HOST)
        }
        compose.setContent {
            NgaQingTheme {
                if (visible) LoginScreen(rememberNavController(), addingAccount = true, viewModel = model)
            }
        }
        compose.waitForIdle()
        verify(exactly = 1) { model.prepareLogin(true) }
        compose.runOnIdle { visible = false }
        compose.waitForIdle()
        verify(exactly = 1) { model.restoreSavedCookies() }
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }
}
