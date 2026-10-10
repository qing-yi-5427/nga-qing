package com.qingyi5427.ngaqing.ui.secondary

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.graphics.Color
import androidx.core.view.drawToBitmap
import java.util.concurrent.atomic.AtomicReference
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.AccountDisplay
import com.qingyi5427.ngaqing.data.local.SavedAccount
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import com.qingyi5427.ngaqing.data.local.WatchedThreadEntity
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.UserProfile
import com.qingyi5427.ngaqing.ui.community.CommunityScreen
import com.qingyi5427.ngaqing.ui.community.CommunityState
import com.qingyi5427.ngaqing.ui.community.CommunityTab
import com.qingyi5427.ngaqing.ui.community.CommunityViewModel
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.login.LoginScreen
import com.qingyi5427.ngaqing.ui.login.LoginState
import com.qingyi5427.ngaqing.ui.login.LoginViewModel
import com.qingyi5427.ngaqing.ui.profile.ProfileScreen
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.root.ProfileLoadState
import com.qingyi5427.ngaqing.ui.settings.SettingsScreen
import com.qingyi5427.ngaqing.ui.settings.SettingsViewModel
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import com.qingyi5427.ngaqing.ui.user.UserScreen
import com.qingyi5427.ngaqing.ui.user.UserUiState
import com.qingyi5427.ngaqing.ui.user.UserViewModel
import com.qingyi5427.ngaqing.ui.web.WebEditorScreen
import com.qingyi5427.ngaqing.ui.web.WebEditorErrorPanel
import com.qingyi5427.ngaqing.ui.web.WebEditorViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the production screens in a narrow, short window. ShadowWebView checks the native
 * login/editor shell only; these tests cannot prove what NGA's remote HTML renders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w320dp-h420dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SecondaryScreensRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun screenshot(filename: String) {
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, filename) }
    }

    private fun render(
        mode: String = "light",
        fontScale: Float = 1f,
        screen: @Composable (NavHostController) -> Unit
    ) {
        compose.setContent {
            NgaQingTheme(themeMode = mode) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale)
                ) {
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

    @Test
    fun loginLoadingAndErrorRemainReachableInShortWindow() {
        val loginState = MutableStateFlow<LoginState>(LoginState.Idle)
        val ready = MutableStateFlow(false)
        val model = mockk<LoginViewModel>(relaxed = true) {
            every { state } returns loginState
            every { webReady } returns ready
            every { ngaDomain } returns MutableStateFlow(NgaDomains.DEFAULT_HOST)
        }
        render(fontScale = 1.4f) { LoginScreen(it, viewModel = model) }
        screenshot("secondary-login-320-light-header.png")
        val loadingBounds = compose.onNodeWithText("正在准备登录环境").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText("完成登录").assertIsNotEnabled().assertIsDisplayed()
        val buttonBounds = button.getUnclippedBoundsInRoot()
        check(loadingBounds.bottom <= buttonBounds.top) { "Web content overlaps login action" }
        screenshot("secondary-login-320-light-large-font.png")

        compose.runOnIdle {
            loginState.value = LoginState.Error("登录凭证未找到")
        }
        compose.onNodeWithText("登录凭证未找到").assertIsDisplayed()
        compose.onNodeWithText("完成登录").assertIsDisplayed()
        compose.runOnIdle {
            loginState.value = LoginState.Idle
            ready.value = true
        }
        compose.onNodeWithText("完成登录").assertIsEnabled()
        screenshot("secondary-login-ready-320x420.png")
        val viewportBounds = compose.onNodeWithContentDescription("NGA 登录网页区域")
            .getUnclippedBoundsInRoot()
        val actionBounds = compose.onNodeWithText("完成登录").getUnclippedBoundsInRoot()
        check(viewportBounds.bottom - viewportBounds.top > 180.dp && viewportBounds.bottom <= actionBounds.top) {
            "Login viewport must occupy the short window without covering its action: $viewportBounds, $actionBounds"
        }
        compose.runOnUiThread {
            val web = findWebView(compose.activity.window.decorView)
                ?: error("Login WebView was not created")
            check(web.settings.useWideViewPort && web.settings.loadWithOverviewMode)
            check(web.settings.builtInZoomControls && !web.settings.displayZoomControls)
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w320dp-h320dp")
    fun loginAtDoubleFontKeepsUsableWebViewportAndFixedAction() {
        val model = mockk<LoginViewModel>(relaxed = true) {
            every { state } returns MutableStateFlow<LoginState>(LoginState.Idle)
            every { webReady } returns MutableStateFlow(true)
            every { ngaDomain } returns MutableStateFlow(NgaDomains.DEFAULT_HOST)
        }
        render(fontScale = 2f) { LoginScreen(it, viewModel = model) }
        screenshot("secondary-login-ready-320x320-2x.png")
        compose.onNodeWithText("完成登录").assertIsDisplayed()
        compose.onNodeWithContentDescription("登录帮助").assertIsDisplayed().performClick()
        compose.onNodeWithText("如何登录").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        val viewportBounds = compose.onNodeWithContentDescription("NGA 登录网页区域")
            .getUnclippedBoundsInRoot()
        val actionBounds = compose.onNodeWithText("完成登录").getUnclippedBoundsInRoot()
        check(viewportBounds.bottom - viewportBounds.top > 110.dp && viewportBounds.bottom <= actionBounds.top) {
            "Double-font login viewport collapsed or overlaps its action: $viewportBounds, $actionBounds"
        }
        compose.runOnUiThread {
            val web = findWebView(compose.activity.window.decorView)
                ?: error("Login WebView was not created")
            check(web.settings.useWideViewPort && web.settings.loadWithOverviewMode)
        }
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }

    @Test
    fun profileEntriesRemainVisibleWithLongNameInDarkTheme() {
        val model = mockk<RootViewModel>(relaxed = true) {
            every { userName } returns MutableStateFlow("一位名字很长的论坛用户")
            every { userId } returns MutableStateFlow("123456")
            every { accountDisplay } returns MutableStateFlow(
                AccountDisplay("123456", "一位名字很长的论坛用户", "", 1L))
            every { profileLoad } returns MutableStateFlow(ProfileLoadState())
        }
        render(mode = "dark", fontScale = 1.3f) { ProfileScreen(it, root = model) }
        compose.onNodeWithText("UID 123456").assertIsDisplayed()
        screenshot("secondary-profile-320-dark-identity.png")
        compose.onNodeWithText("消息与关注").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("设置与阅读偏好").performScrollTo().assertIsDisplayed()
        screenshot("secondary-profile-320-dark-large-font.png")
    }

    @Test
    fun profileReplacesLegacyPlaceholderAndOffersRefreshRetry() {
        val display = MutableStateFlow(AccountDisplay("123457", "", "", 7L))
        val load = MutableStateFlow(ProfileLoadState())
        val model = mockk<RootViewModel>(relaxed = true) {
            every { accountDisplay } returns display
            every { profileLoad } returns load
        }
        render { ProfileScreen(it, root = model) }
        compose.onNodeWithText("NGA 用户").assertIsDisplayed()
        compose.onNodeWithText("UID 123457").assertIsDisplayed()

        compose.runOnUiThread {
            display.value = display.value.copy(username = "恢复的用户")
            load.value = ProfileLoadState("123457", 7L, error = "暂时无法更新用户资料")
        }
        compose.onNodeWithText("恢复的用户").assertIsDisplayed()
        compose.onNodeWithText("资料更新失败，重试").performClick()
        verify(atLeast = 1) { model.refreshOwnProfile() }
    }

    @Test
    fun profileLoadsLocalAvatarAndRendersItOverPlaceholder() {
        // Public GitHub photo fixture; verifies the local Coil path, not NGA network delivery.
        val photo = requireNotNull(javaClass.classLoader?.getResource("avatar/github-u1.png"))
        val avatar = photo.toURI().toString()
        val imageFailure = AtomicReference<Throwable?>()
        val model = mockk<RootViewModel>(relaxed = true) {
            every { accountDisplay } returns MutableStateFlow(
                AccountDisplay("123458", "头像测试用户", avatar, 8L))
            every { profileLoad } returns MutableStateFlow(ProfileLoadState())
        }
        render { ProfileScreen(it, root = model, onAvatarLoadError = imageFailure::set) }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("profile-avatar-loaded").assertExists(); true }
                .getOrDefault(false) ||
                runCatching { compose.onNodeWithTag("profile-avatar-failed").assertExists(); true }
                    .getOrDefault(false)
        }
        screenshot("v8-122-profile-local-avatar-loaded.png")
        assertTrue("Coil failed for bundled avatar fixture: " +
            "${imageFailure.get()?.javaClass?.simpleName}: ${imageFailure.get()?.message}",
            runCatching { compose.onNodeWithTag("profile-avatar-loaded").assertExists(); true }
                .getOrDefault(false))
        val avatarBounds = compose.onNodeWithTag("profile-avatar-loaded")
            .fetchSemanticsNode().boundsInRoot
        val image = compose.runOnUiThread {
            compose.activity.findViewById<View>(android.R.id.content).drawToBitmap()
        }
        val left = avatarBounds.left.toInt().coerceIn(0, image.width)
        val top = avatarBounds.top.toInt().coerceIn(0, image.height)
        val right = kotlin.math.ceil(avatarBounds.right).toInt().coerceIn(left, image.width)
        val bottom = kotlin.math.ceil(avatarBounds.bottom).toInt().coerceIn(top, image.height)
        val visiblePortraitPixel = (top until bottom).any { y ->
            (left until right).any { x ->
                val pixel = image.getPixel(x, y)
                Color.red(pixel) > Color.green(pixel) + 15 &&
                    Color.green(pixel) > Color.blue(pixel) + 5
            }
        }
        assertTrue("Loaded avatar must be visible in its native frame bounds: $avatarBounds",
            visiblePortraitPixel)
    }

    @Test
    fun profileDockHidesOnUserScrollAndReturnsOnReverseScroll() {
        val model = mockk<RootViewModel>(relaxed = true) {
            every { accountDisplay } returns MutableStateFlow(
                AccountDisplay("123459", "滑动测试用户", "", 9L))
            every { profileLoad } returns MutableStateFlow(ProfileLoadState())
        }
        render { ProfileScreen(it, root = model) }
        compose.onNodeWithTag("root-bottom-dock").assertIsDisplayed()
        compose.onNodeWithTag("profile-scroll").performTouchInput { swipeUp() }
        compose.waitUntil(3_000) {
            runCatching {
                compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
        compose.onNodeWithTag("root-bottom-dock").assertDoesNotExist()
        compose.onNodeWithTag("profile-scroll").performTouchInput { swipeDown() }
        compose.onNodeWithTag("root-bottom-dock").assertIsDisplayed()
    }

    @Test
    fun settingsAccountEntryAndLogoutDialogAreReachableByScrolling() {
        val model = mockk<SettingsViewModel>(relaxed = true) {
            every { themeMode } returns MutableStateFlow("light")
            every { ngaDomain } returns MutableStateFlow(NgaDomains.DEFAULT_HOST)
            every { blacklistUsers } returns MutableStateFlow<Set<String>>(emptySet())
            every { blacklistKeywords } returns MutableStateFlow<Set<String>>(emptySet())
            every { readingTextScale } returns MutableStateFlow(1f)
            every { readingLineSpacing } returns MutableStateFlow(1f)
            every { readingDockMode } returns MutableStateFlow(ReadingDockMode.AUTO)
            every { showSignatures } returns MutableStateFlow(true)
            every { accounts } returns MutableStateFlow(
                listOf(SavedAccount("123456", "cid", "测试用户"))
            )
            every { activeUid } returns MutableStateFlow("123456")
        }
        render(fontScale = 1.3f) { SettingsScreen(it, viewModel = model) }
        compose.onNodeWithText("外观").assertIsDisplayed()
        screenshot("secondary-settings-320-light-large-font.png")
        compose.onNodeWithText("深色").performClick()
        verify { model.setTheme("dark") }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithText("测试用户").assertIsDisplayed()
        compose.onNodeWithText("添加账号").performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
        compose.onNodeWithText("退出登录").assertIsDisplayed().performClick()
        compose.onNodeWithText("退出登录？").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun userScreenRendersLoadingErrorAndTopicContent() {
        val userState = MutableStateFlow(UserUiState())
        val model = mockk<UserViewModel>(relaxed = true) {
            every { uid } returns "123456"
            every { fallbackName } returns "测试用户"
            every { state } returns userState
        }
        render { UserScreen(it, viewModel = model) }
        compose.onNodeWithText("正在加载用户资料").assertIsDisplayed()

        compose.runOnIdle { userState.value = UserUiState(loading = false, error = "网络不可用") }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        compose.onNodeWithText("网络不可用").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()

        compose.runOnIdle {
            userState.value = UserUiState(
                loading = false,
                profile = UserProfile(uid = "123456", username = "测试用户",
                    group = "普通用户", posts = 128, followedBy = 34),
                topics = listOf(ThreadItem(
                    tid = "100", subject = "一篇公开主题", author = "测试用户",
                    authorId = "123456", postDate = System.currentTimeMillis() / 1000 - 3600,
                    replies = 2
                ))
            )
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        compose.onNodeWithText("一篇公开主题").assertIsDisplayed()
        screenshot("secondary-user-320-light-content.png")
    }

    @Test
    fun communityScreenRendersLoadingEmptyErrorAndContent() {
        val tabState = MutableStateFlow(CommunityTab.NOTIFICATIONS)
        val contentState = MutableStateFlow<CommunityState>(CommunityState.Loading)
        val watchedState = MutableStateFlow<List<WatchedThreadEntity>>(emptyList())
        val model = mockk<CommunityViewModel>(relaxed = true) {
            every { tab } returns tabState
            every { state } returns contentState
            every { watched } returns watchedState
        }
        render(mode = "dark", fontScale = 1.2f) { CommunityScreen(it, viewModel = model) }
        compose.onNodeWithText("正在加载消息").assertIsDisplayed()

        compose.runOnIdle { contentState.value = CommunityState.Content(emptyList()) }
        compose.onNodeWithText("暂无提醒").assertIsDisplayed()
        compose.runOnIdle { contentState.value = CommunityState.Error("消息加载失败") }
        compose.onNodeWithText("消息加载失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()

        compose.runOnIdle {
            contentState.value = CommunityState.Content(
                listOf(CommunityItem(id = "1", title = "收到一条新提醒",
                    summary = "主题有新回复", actor = "论坛用户",
                    createdAt = System.currentTimeMillis() / 1000 - 3600))
            )
        }
        compose.onNodeWithText("收到一条新提醒").assertIsDisplayed()
        screenshot("secondary-community-320-dark-content.png")
        compose.onNodeWithText("关注").performClick()
        verify { model.select(CommunityTab.WATCHING) }
        compose.runOnIdle {
            tabState.value = CommunityTab.WATCHING
            watchedState.value = listOf(
                WatchedThreadEntity(tid = "100", title = "关注的主题", fid = "1")
            )
        }
        compose.onNodeWithText("关注的主题").assertIsDisplayed()
    }

    @Test
    fun webEditorShowsCookieSyncAndRealWebViewShell() {
        val ready = MutableStateFlow(false)
        val model = mockk<WebEditorViewModel>(relaxed = true) {
            every { cookiesReady } returns ready
            every { title } returns "高级编辑"
            every { url } returns "https://bbs.nga.cn/post.php"
        }
        render { WebEditorScreen(it, viewModel = model) }
        compose.onNodeWithText("正在同步登录状态").assertIsDisplayed()
        screenshot("secondary-editor-320-light-sync.png")
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithText("高级编辑").assertIsDisplayed()
        compose.onNodeWithText("正在同步登录状态").assertDoesNotExist()
    }

    @Test
    fun webEditorMainFrameErrorUsesProductionRetryPanel() {
        render(mode = "dark", fontScale = 1.4f) { WebEditorErrorPanel(onRetry = {}) }
        compose.onNodeWithText("网页编辑器加载失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
        screenshot("secondary-editor-320-dark-error.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h600dp")
    fun wideProfileRetainsIdentityAndEntries() {
        val model = mockk<RootViewModel>(relaxed = true) {
            every { userName } returns MutableStateFlow("一位名字很长的论坛用户")
            every { userId } returns MutableStateFlow("123456")
            every { accountDisplay } returns MutableStateFlow(
                AccountDisplay("123456", "一位名字很长的论坛用户", "", 1L))
            every { profileLoad } returns MutableStateFlow(ProfileLoadState())
        }
        render { ProfileScreen(it, root = model) }
        compose.onNodeWithText("一位名字很长的论坛用户").assertIsDisplayed()
        compose.onNodeWithText("消息与关注").assertIsDisplayed()
        compose.onNodeWithText("设置与阅读偏好").assertIsDisplayed()
        screenshot("secondary-profile-840-light-content.png")
    }

    @Test
    fun editorStatusFitsAtDoubleFontScale() {
        val model = mockk<WebEditorViewModel>(relaxed = true) {
            every { cookiesReady } returns MutableStateFlow(false)
            every { title } returns "高级编辑"
            every { url } returns "https://bbs.nga.cn/post.php"
        }
        render(mode = "dark", fontScale = 2f) { WebEditorScreen(it, viewModel = model) }
        compose.onNodeWithText("NGA 网页编辑器").assertIsDisplayed()
        compose.onNodeWithText("同步账号中").assertIsDisplayed()
        screenshot("secondary-editor-320-dark-2x-font.png")
    }
}
