package com.qingyi5427.ngaqing.ui.post

import com.qingyi5427.ngaqing.data.model.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderProgressTest {
    private fun floor(number: Int) = Post("tid", "pid-$number", "作者", "42", "主题",
        1_790_540_000L, number, "正文")

    @Test fun firstFloorStartsAtZeroAndOfflineBannerIsNotAReply() {
        val posts = listOf(floor(0), floor(1))
        assertEquals(0f, readingFloorProgress(posts, 0, 0, 100, false, 91)!!, 0.0001f)
        assertEquals(0f, readingFloorProgress(posts, 0, 0, 40, true, 91)!!, 0.0001f)
        assertEquals(0.5f / 91f,
            readingFloorProgress(posts, 1, 50, 100, true, 91)!!, 0.0001f)
    }

    @Test fun loadedPageCountAndFooterCannotResetOrFakeGlobalProgress() {
        val firstPage = listOf(floor(0), floor(1))
        val appended = firstPage + floor(2)
        val before = readingFloorProgress(firstPage, 1, 20, 100, false, 91)!!
        val after = readingFloorProgress(appended, 1, 20, 100, false, 91)!!
        assertEquals(before, after, 0.0001f)
        assertEquals(30f / 91f,
            readingFloorProgress(listOf(floor(30)), 0, 0, 100, false, 91)!!, 0.0001f)
        assertEquals(2f / 91f,
            readingFloorProgress(firstPage, 2, 0, 0, false, 91)!!, 0.0001f)
    }

    @Test fun unknownTotalDoesNotReportProgress() {
        assertNull(readingFloorProgress(listOf(floor(0)), 0, 0, 100, false, 0))
        assertNull(readingFloorProgress(emptyList(), 0, 0, 100, false, 91))
    }
}
