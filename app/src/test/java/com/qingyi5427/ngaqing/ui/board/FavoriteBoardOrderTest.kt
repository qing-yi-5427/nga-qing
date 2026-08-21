package com.qingyi5427.ngaqing.ui.board

import com.qingyi5427.ngaqing.data.model.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FavoriteBoardOrderTest {
    private val boards = listOf(
        Board(fid = "1", name = "一"),
        Board(fid = "2", stid = "20", name = "二"),
        Board(fid = "3", name = "三")
    )

    @Test
    fun `moves favorite board to target position`() {
        val moved = moveFavoriteBoard(boards, draggedKey = "fid:1", targetKey = "fid:3")

        assertEquals(listOf("二", "三", "一"), moved.map { it.name })
    }

    @Test
    fun `uses stid as the stable key when available`() {
        assertEquals("stid:20", favoriteBoardKey(boards[1]))
    }

    @Test
    fun `unknown drag target leaves order untouched`() {
        assertSame(boards, moveFavoriteBoard(boards, "fid:1", "fid:404"))
    }
}
