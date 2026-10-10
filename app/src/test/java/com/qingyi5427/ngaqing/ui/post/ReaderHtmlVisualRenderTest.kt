package com.qingyi5427.ngaqing.ui.post

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.view.drawToBitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.data.local.ReadingDockMode
import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.V8LayoutPolicy
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
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
import java.io.File
import kotlin.math.abs

/** Same title and story as the HTML prototype, rendered by production PostScreen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderHtmlVisualRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [33], qualifiers = "w394dp-h854dp")
    fun phonePortraitMatchesPrototypeContentAndKeepsRichBlocksReachable() {
        renderReader(394f, 854f, medium = false)
        compose.onNodeWithText(STORY_TITLE).assertIsDisplayed()
        compose.onNodeWithText(FIRST_PARAGRAPH, substring = true).assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("post-image-loaded").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-reader-html-phoneP-394x854.png")
        }
        compose.onNodeWithText("一个关于阅读的小结").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-reader-html-phoneP-rich-394x854.png")
        }
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        compose.onNodeWithText("引用仍可阅读").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w698dp-h986dp")
    fun innerPortraitUsesComfortTypographyWithSameStory() {
        renderReader(698f, 986f, medium = true)
        compose.onNodeWithText(STORY_TITLE).assertIsDisplayed()
        compose.onNodeWithText(FIRST_PARAGRAPH, substring = true).assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("post-image-loaded").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-reader-html-innerP-698x986.png")
        }
        compose.onNodeWithText("一个关于阅读的小结").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-reader-html-innerP-rich-698x986.png")
        }
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        compose.onNodeWithText("引用仍可阅读").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun replyBodyQuoteAndActionsShareMainBodyLeftEdgeOnCover() {
        assertReplyLeftEdge(425f, 623f, medium = false, "cover")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w698dp-h986dp")
    fun replyBodyQuoteAndActionsShareMainBodyLeftEdgeOnInnerPortrait() {
        assertReplyLeftEdge(698f, 986f, medium = true, "innerP")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w986dp-h698dp")
    fun replyBodyQuoteAndActionsShareMainBodyLeftEdgeOnWideWindow() {
        assertReplyLeftEdge(986f, 698f, medium = true, "wide")
    }

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun publicPhotoFixtureIsDecodedAndVisibleInsteadOfInitial() {
        val resource = requireNotNull(javaClass.classLoader?.getResource("avatar/github-u1.png"))
        renderReader(425f, 623f, medium = false, avatar = resource.toURI().toString())
        compose.onAllNodesWithTag("post-author-avatar").onFirst().assertIsDisplayed()
        compose.waitUntil(5_000) { avatarCenterPixel()?.let(::isPortraitSkinPixel) == true }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-122-avatar-loaded-public-fixture.png")
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun blankAvatarKeepsInitialFallback() {
        renderReader(425f, 623f, medium = false, avatar = "")
        compose.onAllNodesWithTag("post-author-avatar").onFirst().assertIsDisplayed()
        check(avatarCenterPixel()?.let(::isPortraitSkinPixel) == false)
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-122-avatar-blank-fallback.png")
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w425dp-h623dp")
    fun failedAvatarKeepsInitialFallback() {
        renderReader(425f, 623f, medium = false, avatar = "file:///missing-v8-avatar.jpg")
        compose.onAllNodesWithTag("post-author-avatar").onFirst().assertIsDisplayed()
        compose.waitForIdle()
        check(avatarCenterPixel()?.let(::isPortraitSkinPixel) == false)
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-122-avatar-error-fallback.png")
        }
    }

    private fun avatarCenterPixel(): Int? {
        val bounds = compose.onAllNodesWithTag("post-author-avatar").onFirst()
            .getUnclippedBoundsInRoot()
        var pixel: Int? = null
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            val x = ((bounds.left + bounds.right) / 2f).value.toInt()
            val y = ((bounds.top + bounds.bottom) / 2f).value.toInt()
            if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                pixel = bitmap.getPixel(x, y)
            }
            bitmap.recycle()
        }
        return pixel
    }

    private fun isPortraitSkinPixel(pixel: Int): Boolean =
        Color.red(pixel) > Color.green(pixel) + 15 &&
            Color.green(pixel) > Color.blue(pixel) + 5

    private fun assertReplyLeftEdge(width: Float, height: Float, medium: Boolean, suffix: String) {
        renderReader(width, height, medium)
        val mainLeft = compose.onNodeWithText(FIRST_PARAGRAPH, substring = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot().left
        compose.onNodeWithTag("post-reading-list").performScrollToIndex(1)
        val replyLeft = compose.onNodeWithText("同感！能够一键收起列表很重要", substring = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot().left
        val quoteLeft = compose.onAllNodesWithTag("post-quote").onLast()
            .assertIsDisplayed().getUnclippedBoundsInRoot().left
        val actionsLeft = compose.onAllNodesWithTag("post-reply-actions").onFirst()
            .assertIsDisplayed().getUnclippedBoundsInRoot().left
        listOf("reply" to replyLeft, "quote" to quoteLeft, "actions" to actionsLeft)
            .forEach { (name, left) ->
                check(abs((left - mainLeft).value) <= 4f) {
                    "$name starts after a redundant avatar column: main=$mainLeft actual=$left"
                }
            }
        compose.runOnUiThread {
            saveNativeScreenshot(compose.activity, "v8-122-reader-reply-$suffix.png")
        }
    }

    private fun renderReader(width: Float, height: Float, medium: Boolean, avatar: String = "") {
        val imageFile = makeIllustration()
        val first = Post("html-reader", "html-first", "晚风过境", "42", STORY_TITLE,
            1_790_540_000L, 0, "$FIRST_PARAGRAPH\n\n$SECOND_PARAGRAPH", avatar = avatar)
        val reply = Post("html-reader", "html-reply", "山间回声", "43", "",
            1_790_540_600L, 1, "同感！能够一键收起列表很重要，安静看完一篇文章再切回来，整个过程就很顺。")
        val later = Post("html-reader", "html-later", "云间来信", "44", "",
            1_790_541_200L, 2, "正文有最大行宽限制，大屏阅读时眼睛就不必来回追很长的句子。")
        val vm = mockk<PostViewModel>(relaxed = true)
        every { vm.tid } returns "html-reader"
        every { vm.uiState } returns MutableStateFlow<PostUiState>(PostUiState.Success(
            posts = listOf(first, reply, later), page = 1, totalPages = 2,
            renderData = mapOf(
                "html-first" to PostRenderData(listOf(
                    PostBlock.Text("$FIRST_PARAGRAPH\n\n$SECOND_PARAGRAPH"),
                    PostBlock.Img(imageFile.toURI().toString()),
                    PostBlock.Text("一个关于阅读的小结", bold = true),
                    PostBlock.Quote(listOf(PostBlock.Text("让工具在需要的时候出现，让内容始终占据主角的位置。")),
                        refName = "阅读小结", floor = 0),
                    PostBlock.Text("$THIRD_PARAGRAPH\n\n$FOURTH_PARAGRAPH")
                ), emptyList(), emptyList()),
                "html-reply" to PostRenderData(listOf(
                    PostBlock.Quote(listOf(PostBlock.Text("引用仍可阅读")), refName = "晚风过境", floor = 0),
                    PostBlock.Text(reply.content)
                ), emptyList(), emptyList()),
                "html-later" to PostRenderData(listOf(PostBlock.Text(later.content)), emptyList(), emptyList())
            )
        ))
        every { vm.subject } returns MutableStateFlow(STORY_TITLE)
        every { vm.isFavorite } returns MutableStateFlow(false)
        every { vm.isLoadingMore } returns MutableStateFlow(false)
        every { vm.loadError } returns MutableStateFlow<String?>(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        every { vm.targetFloor } returns MutableStateFlow<Int?>(null)
        every { vm.totalRows } returns MutableStateFlow(3)
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
        val root = mockk<RootViewModel>(relaxed = true)
        every { root.readingDockMode } returns MutableStateFlow(ReadingDockMode.AUTO)
        every { root.readingTextScale } returns MutableStateFlow(1f)
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                CompositionLocalProvider(LocalV8LayoutPolicy provides V8LayoutPolicy(
                    availableWidthDp = width, availableHeightDp = height,
                    mediumComfortTypography = medium
                )) {
                    Surface(Modifier.fillMaxSize()) {
                        PostScreen(rememberNavController(), dark = false, viewModel = vm,
                            root = root, boardName = "数码硬件")
                    }
                }
            }
        }
    }

    private fun makeIllustration(): File {
        val file = File(compose.activity.cacheDir, "reader-html-illustration.png")
        val bitmap = Bitmap.createBitmap(640, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(246, 248, 254))
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(91, 108, 130)
            style = Paint.Style.STROKE
            strokeWidth = 7f
        }
        canvas.drawRoundRect(60f, 28f, 166f, 263f, 20f, 20f, border)
        canvas.drawRoundRect(272f, 45f, 585f, 250f, 22f, 22f, border)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(168, 198, 233)
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
        }
        for (y in 65..225 step 27) {
            canvas.drawLine(77f, y.toFloat(), 145f, y.toFloat(), line)
            canvas.drawLine(292f, y.toFloat(), 392f, y.toFloat(), line)
            canvas.drawLine(418f, y.toFloat(), 552f, y.toFloat(), line)
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private companion object {
        const val STORY_TITLE = "折叠屏使用一周后，我终于理解了它真正的意义"
        const val FIRST_PARAGRAPH = "折叠屏用了整整一周。最开始，我以为它的价值只是屏幕面积翻倍；后来发现，真正改变的是自己使用手机的方式。"
        const val SECOND_PARAGRAPH = "外屏适合快速浏览信息：通勤时扫一眼主题列表，看到感兴趣的内容先存起来。展开内屏后，我希望能够安心阅读，而不是看到占据一半屏幕的按钮和导航。"
        const val THIRD_PARAGRAPH = "尤其在横屏时，主题列表和正文并排确实方便。但列表不需要永久占据三分之一以外的空间，更没有必要把版块导航也固定在旁边。"
        const val FOURTH_PARAGRAPH = "比较舒服的比例是：让主题列表保持可以识别标题的宽度，把剩下的空间留给正文。需要换版块时，导航从边缘轻轻滑出来，用完就收起。"
    }
}
