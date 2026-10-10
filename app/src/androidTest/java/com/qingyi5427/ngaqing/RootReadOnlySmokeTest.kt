package com.qingyi5427.ngaqing

import android.graphics.Rect
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
        val launch = instrumentation.targetContext.packageManager
            .getLaunchIntentForPackage(targetPackage)
            ?: throw AssertionError("Target launcher activity is missing")
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        instrumentation.startActivitySync(launch)
        instrumentation.waitForIdleSync()

        assertTrue("Target window did not become active", await { root()?.packageName == targetPackage })
        if (await(2_000) { hasExactText("账号登录") || hasExactText("登录 NGA") }) {
            throw AssertionError("AUTH_REQUIRED: existing session is unavailable; no test credentials were supplied")
        }

        clickBottomTab("版块")
        assertText("按兴趣找到想看的讨论")
        clickExact("全站搜索", contentDescription = true)
        assertText("全站搜索")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        assertText("按兴趣找到想看的讨论")

        clickBottomTab("收藏")
        assertText("篇收藏内容")
        clickBottomTab("我的")
        assertText("账号、阅读记录与社区消息")

        clickExact("打开导航和更多选项", contentDescription = true)
        clickExact("阅读记录")
        assertText("阅读记录")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        assertText("账号、阅读记录与社区消息")

        clickBottomTab("发现")
        assertText("继续阅读")
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
        descendants(root()).any { it.text?.toString() == label }

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
