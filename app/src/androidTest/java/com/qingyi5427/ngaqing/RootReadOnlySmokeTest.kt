package com.qingyi5427.ngaqing

import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Manual, data-preserving on-device smoke test. Run only after separately installing a verified
 * target APK and this test APK; never use connectedAndroidTest because its install lifecycle can
 * replace the target. No account, post, reply, or saved content is created or deleted here.
 */
@RunWith(AndroidJUnit4::class)
class RootReadOnlySmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation = instrumentation.uiAutomation
    private val targetPackage = instrumentation.targetContext.packageName

    @Test
    fun fourTabsAndReadOnlyRoutesOnExistingSession() {
        launchTarget()

        clickBottomTab("版块")
        assertText("按兴趣找到想看的讨论")
        clickExact("搜索帖子、版块与用户")
        assertText("全站搜索")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        assertText("按兴趣找到想看的讨论")

        clickBottomTab("收藏")
        assertText("篇收藏内容")
        clickBottomTab("我的")
        assertText("账号、阅读记录与社区消息")

        clickExact("打开导航和更多选项", contentDescription = true)
        clickExact("阅读记录")
        assertText("浏览历史")
        clickExact("返回", contentDescription = true)
        assertText("账号、阅读记录与社区消息")

        clickBottomTab("发现")
        assertText("继续阅读")
    }

    @Test
    fun replyEditorAndImeOpenWithoutSending() {
        launchTarget()
        assertText("继续阅读  →")
        clickExact("继续阅读  →")
        assertTrue("Reader did not open", await { hasDescription("更多") })
        clickExact("更多", contentDescription = true)
        clickExact("回复帖子")
        val editor = await {
            descendants(root()).any { node ->
                node.isVisibleToUser &&
                    node.className?.toString()?.contains("EditText") == true
            }
        }
        assertTrue("Reply editor did not appear", editor)
        val field = descendants(root()).first {
            it.isVisibleToUser && it.className?.toString()?.contains("EditText") == true
        }
        assertTrue("Reply editor could not receive focus",
            field.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        val imeShown = await(3_000) {
            val state = inputMethodState()
            state.contains("mInputShown=true") && state.contains("mImeWindowVis=3")
        }
        if (!imeShown) {
            captureDeviceScreenshot("nga_v8_reply_ime_diagnostic.png")
            val editorStillVisible = descendants(root()).any { node ->
                node.isVisibleToUser && node.className?.toString()?.contains("EditText") == true
            }
            throw AssertionError("Input method was not visible; " +
                "editorVisible=$editorStillVisible; " +
                "imeState=${inputMethodState()}; " +
                "screenshot=/sdcard/Download/nga_v8_reply_ime_diagnostic.png")
        }
        // Do not read or change an existing draft. Only dismiss the keyboard and editor.
        automation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        clickExact("取消")
        assertTrue("Reader was not restored after closing the editor",
            await { hasDescription("更多") })
    }

    @Test
    fun publicDiscussionScrollAndContinueReading() {
        launchTarget()
        val metrics = instrumentation.targetContext.resources.displayMetrics
        if (await { hasExactText("继续阅读  →") }) {
            clickExact("继续阅读  →")
        } else {
            assertText("正在讨论")
            val heading = descendants(root()).firstOrNull { it.text?.toString() == "正在讨论" }
                ?: throw AssertionError("Discussion heading unavailable")
            val headingBounds = Rect().also(heading::getBoundsInScreen)
            val firstRow = descendants(root()).filter { it.isClickable }.mapNotNull { node ->
                val bounds = Rect().also(node::getBoundsInScreen)
                if (bounds.top > headingBounds.bottom && bounds.bottom < metrics.heightPixels * 0.92 &&
                    bounds.width() > metrics.widthPixels * 0.65) node to bounds else null
            }.minByOrNull { it.second.top }?.first
                ?: throw AssertionError("No visible public discussion row")
            assertTrue("Could not open the public discussion", firstRow.performAction(
                AccessibilityNodeInfo.ACTION_CLICK))
        }
        assertTrue("Reader did not open", await { hasDescription("更多") })

        assertTrue("Reader has no accessible scroll container", await {
            descendants(root()).filter { it.isScrollable }.firstOrNull { node ->
                val bounds = Rect().also(node::getBoundsInScreen)
                bounds.width() > metrics.widthPixels * 0.6 &&
                    bounds.height() > metrics.heightPixels * 0.25
            }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
        })
        clickExact("更多", contentDescription = true)
        clickExact("跳转楼层")
        assertText("楼层")
        val field = descendants(root()).firstOrNull {
            it.className?.toString()?.contains("EditText") == true
        } ?: throw AssertionError("Floor input unavailable")
        val input = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "5")
        }
        assertTrue("Could not enter floor 5", field.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT, input))
        clickExact("跳转")
        val floor5Reached = await {
            !hasExactText("跳转楼层") && visibleFloorLabels().firstOrNull() == "#5"
        }
        if (!floor5Reached) {
            captureDeviceScreenshot("nga_v8_floor5_jump_failure.png")
            throw AssertionError("Floor 5 never became the visible first row; " +
                "active=${root()?.packageName}; dialog=${hasExactText("跳转楼层")}; " +
                "readerMenu=${hasDescription("更多")}; retry=${hasExactText("重试")}; " +
                "floors=${visibleFloorLabels()}; screenshot=/sdcard/Download/nga_v8_floor5_jump_failure.png")
        }
        SystemClock.sleep(150)
        assertTrue("Floor 5 shifted before leaving; floors=${visibleFloorLabels()}",
            visibleFloorLabels().firstOrNull() == "#5")

        repeat(3) {
            if (hasExactText("继续阅读")) return@repeat
            automation.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            await(2_000) { hasExactText("继续阅读") }
        }
        assertText("继续阅读  →")
        val cardFloor5 = hasExactText("第 5 楼")
        captureDeviceScreenshot("nga_v8_home_card5.png")
        assertTrue("Continue card did not persist floor 5", cardFloor5)
        clickExact("继续阅读  →")
        val restoredFloor5 = await { visibleFloorLabels().firstOrNull() == "#5" }
        captureDeviceScreenshot("nga_v8_reader_floor5.png")
        assertTrue("Resume floor #5 not visible; cardFloor5=$cardFloor5; " +
            "readerMenu=${hasDescription("更多")}; visibleFloors=${visibleFloorLabels()}",
            restoredFloor5)
    }

    private fun launchTarget() {
        val launch = instrumentation.targetContext.packageManager
            .getLaunchIntentForPackage(targetPackage)
            ?: throw AssertionError("Target launcher activity is missing")
        val component = launch.component ?: throw AssertionError("Target launcher component missing")
        // The shell activity launcher works on this HyperOS device even when framework
        // startActivitySync waits indefinitely for a Compose idle frame.
        automation.executeShellCommand("am start -W -n ${component.flattenToShortString()}").close()
        assertTrue("Target window did not become active; active=${root()?.packageName}",
            await { root()?.packageName?.toString() == targetPackage })
        if (await(2_000) { hasExactText("账号登录") || hasExactText("登录 NGA") }) {
            throw AssertionError("AUTH_REQUIRED: existing session is unavailable; no test credentials were supplied")
        }
    }

    private fun clickBottomTab(label: String) {
        assertTrue("Bottom tab '$label' unavailable", await {
            val window = root() ?: return@await false
            val height = instrumentation.targetContext.resources.displayMetrics.heightPixels
            val candidate = descendants(window).filter { it.text?.toString() == label }
                .mapNotNull { clickableAncestor(it) }
                .firstOrNull {
                    val bounds = Rect()
                    it.getBoundsInScreen(bounds)
                    bounds.centerY() > height * 0.65
                } ?: return@await false
            candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        })
    }

    private fun clickExact(label: String, contentDescription: Boolean = false) {
        assertTrue("Control '$label' unavailable", await {
            val node = descendants(root()).firstOrNull {
                if (contentDescription) it.contentDescription?.toString() == label
                else it.text?.toString() == label
            } ?: return@await false
            clickableAncestor(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        })
    }

    private fun assertText(label: String) {
        assertTrue("Expected '$label' in target window", await { hasExactText(label) })
    }

    private fun hasExactText(label: String): Boolean =
        descendants(root()).any { it.isVisibleToUser && it.text?.toString() == label }

    private fun hasDescription(label: String): Boolean =
        descendants(root()).any { it.contentDescription?.toString() == label }

    private fun visibleFloorLabels(): List<String> {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        return descendants(root()).filter { it.isVisibleToUser }.mapNotNull { node ->
            val label = node.text?.toString()?.takeIf { it.matches(Regex("#\\d+")) }
                ?: return@mapNotNull null
            val bounds = Rect().also(node::getBoundsInScreen)
            if (bounds.isEmpty || bounds.bottom <= 0 || bounds.top >= metrics.heightPixels ||
                bounds.right <= 0 || bounds.left >= metrics.widthPixels) null
            else label to bounds.top
        }.sortedBy { it.second }.map { it.first }.toList()
    }

    private fun captureDeviceScreenshot(fileName: String) {
        automation.executeShellCommand("screencap -p /sdcard/Download/$fileName")
            .use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
                    val buffer = ByteArray(256)
                    while (stream.read(buffer) != -1) Unit
                }
            }
    }

    private fun inputMethodState(): String =
        automation.executeShellCommand("dumpsys input_method").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader ->
                reader.lineSequence().filter { line ->
                    line.contains("mInputShown=") || line.contains("mImeWindowVis=") ||
                        line.contains("mIsInputViewShown=")
                }.take(5).joinToString(" | ") { it.trim() }
            }
        }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null && !current.isClickable) current = current.parent
        return current
    }

    private fun root(): AccessibilityNodeInfo? = automation.rootInActiveWindow

    private fun descendants(node: AccessibilityNodeInfo?): Sequence<AccessibilityNodeInfo> = sequence {
        if (node != null) {
            yield(node)
            for (index in 0 until node.childCount) yieldAll(descendants(node.getChild(index)))
        }
    }

    private fun await(timeoutMs: Long = 8_000, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(120)
        }
        return condition()
    }
}
