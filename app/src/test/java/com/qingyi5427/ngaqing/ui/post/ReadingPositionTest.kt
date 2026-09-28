package com.qingyi5427.ngaqing.ui.post

import com.qingyi5427.ngaqing.data.model.Post
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPositionTest {
    @Test fun visibleFloorWinsOverPrefetchedPage() {
        assertEquals(ReadingPosition(page = 1, floor = 12), restoreReadingPosition(12, 2, 7))
    }

    @Test fun explicitPageWithoutVisibleFloorRestoresItsStart() {
        assertEquals(ReadingPosition(page = 3, floor = 60), restoreReadingPosition(null, 3, null))
    }

    @Test fun savedVisibleFloorWinsOverOlderHistory() {
        assertEquals(ReadingPosition(page = 4, floor = 91), restoreReadingPosition(91, 1, 20))
    }

    @Test fun offlineBannerDoesNotAdvanceDisplayedPageAcrossFloorBoundary() {
        val posts = listOf(
            Post("1", "10", "作者", "2", "标题", 1L, 29, "正文"),
            Post("1", "11", "作者", "2", "", 2L, 30, "正文")
        )
        assertEquals(1, readingPageForVisibleIndex(posts, 1, true, 2, 3))
        assertEquals(2, readingPageForVisibleIndex(posts, 2, true, 1, 3))
        assertEquals(2, readingPageForVisibleIndex(posts, 1, false, 1, 3))
    }

    @Test fun offlineBannerOffsetsFloorJumpAndReadPositionTogether() {
        val posts = listOf(
            Post("1", "10", "作者", "2", "标题", 1L, 29, "正文"),
            Post("1", "11", "作者", "2", "", 2L, 30, "下一页正文")
        )
        val targetIndex = posts.indexOfFirst { it.lou >= 30 }
        val lazyIndex = postLazyItemIndex(targetIndex, fromCache = true)
        assertEquals(2, lazyIndex)
        assertEquals(targetIndex, visiblePostIndex(lazyIndex, fromCache = true))
        assertEquals(30, posts[visiblePostIndex(lazyIndex, fromCache = true)].lou)
        assertEquals(2, readingPageForVisibleIndex(posts, lazyIndex, true, 1, 3))
        assertEquals(1, postLazyItemIndex(targetIndex, fromCache = false))
    }

    @Test fun largePageCountsKeepTheDockLabelWithinItsTouchTarget() {
        assertEquals("1/8", compactPageLabel(1, 8, largeText = false))
        assertEquals("1234", compactPageLabel(1234, 12345, largeText = false))
        assertEquals("1", compactPageLabel(1, 12345, largeText = true))
        assertEquals("12345", compactPageLabel(12345, 123456, largeText = true))
        assertEquals("页", compactPageLabel(123456, 1234567, largeText = true))
    }
}
