package com.ngaclient.app.data.repository

import com.ngaclient.app.data.model.Board
import com.ngaclient.app.data.remote.NgaResourceUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardStructureTest {

    @Test
    fun `same-name forum becomes parent and remaining forums become children`() {
        val parent = Board(fid = "-7", name = "网事杂谈")
        val child = Board(fid = "-7955747", name = "晴风村")

        val group = buildBoardGroup("网事杂谈", "网事杂谈", listOf(parent, child))

        assertEquals(parent, group.parent)
        assertEquals(listOf(child), group.children)
    }

    @Test
    fun `category-only group keeps every forum as a child`() {
        val boards = listOf(
            Board(fid = "334", name = "消费电子 IT新闻"),
            Board(fid = "510", name = "硬件配置")
        )

        val group = buildBoardGroup("网事杂谈", "IT软硬件", boards)

        assertNull(group.parent)
        assertEquals(boards, group.children)
    }

    @Test
    fun `forum and collection icon paths use their canonical identifiers`() {
        assertEquals(
            "https://img4.nga.cn/ngabbs/nga_classic/f/app/-7.png",
            NgaResourceUrls.boardIcon("-7", null)
        )
        assertEquals(
            "https://img4.nga.cn/proxy/cache_attach/ficon/39827852v.png",
            NgaResourceUrls.boardIcon("-7", "39827852")
        )
    }
}
