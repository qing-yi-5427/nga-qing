package com.qingyi5427.ngaqing.ui.post

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
}
