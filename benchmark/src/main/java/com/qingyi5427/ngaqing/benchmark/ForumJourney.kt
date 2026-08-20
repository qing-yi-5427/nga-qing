package com.qingyi5427.ngaqing.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice

internal const val TARGET_PACKAGE = "com.qingyi5427.ngaqing"

/**
 * 覆盖论坛最常见的热路径：启动、等待首屏、连续上下滚动。
 * 未登录时没有列表，流程会只采集启动路径而不会让测试失败。
 */
internal fun UiDevice.runForumJourney(startActivity: () -> Unit) {
    wakeUp()
    startActivity()
    waitForIdle()
    val scrollable = findObject(By.scrollable(true)) ?: return
    val margin = scrollable.visibleBounds.width() / 8
    scrollable.setGestureMargin(margin)
    repeat(3) { scrollable.fling(Direction.DOWN) }
    repeat(2) { scrollable.fling(Direction.UP) }
    waitForIdle()
}
