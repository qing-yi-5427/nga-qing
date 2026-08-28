package com.qingyi5427.ngaqing.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatAuthorTest {
    @Test
    fun convertsNgaAnonymousHashToStableAlias() {
        assertEquals(
            "匿名 · 壬柯顾己蓝应",
            formatAuthorName("#anony_8905635a3dc3a79511fc6217423df746")
        )
    }

    @Test
    fun preservesRegularAuthorName() {
        assertEquals("普通用户", formatAuthorName("普通用户"))
    }
}
