package com.qingyi5427.ngaqing.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FormatTest {
    private val now = 1_800_000_000_000L

    @Test
    fun epochSecondsAndMillisecondsFormatIdentically() {
        val twoHoursAgoMillis = now - 2 * 60 * 60 * 1000L

        assertEquals("2 小时前", formatRelative(twoHoursAgoMillis, now))
        assertEquals("2 小时前", formatRelative(twoHoursAgoMillis / 1000L, now))
    }

    @Test
    fun futureTimestampIsNotReportedAsJustNow() {
        assertNotEquals("刚刚", formatRelative(now + 2 * 60 * 1000L, now))
    }

    @Test
    fun invalidTimestampStaysEmpty() {
        assertEquals("", formatRelative(0L, now))
    }
}
