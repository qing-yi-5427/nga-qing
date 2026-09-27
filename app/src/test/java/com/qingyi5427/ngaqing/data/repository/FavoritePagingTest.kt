package com.qingyi5427.ngaqing.data.repository

import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.ThreadPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FavoritePagingTest {
    @Test fun collectsAllPagesUsingServerTotal() = runBlocking {
        val fetched = mutableListOf<Int>()
        val rows = collectFavoritePages { page ->
            fetched += page
            when (page) {
                1 -> page(3, "1", "2")
                2 -> page(3, "3")
                else -> error("unexpected page")
            }
        }
        assertEquals(listOf(1, 2), fetched)
        assertEquals(listOf("1", "2", "3"), rows.map { it.tid })
    }

    @Test fun secondPageFailureNeverReturnsPartialSnapshot() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                collectFavoritePages { page ->
                    if (page == 1) page(3, "1", "2") else ThreadPage(error = "服务端错误")
                }
            }
        }
    }

    @Test fun earlyEmptyPageAndNonAdjacentDuplicateFail() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                collectFavoritePages { page ->
                    if (page == 1) page(3, "1", "2") else page(3)
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                collectFavoritePages { page ->
                    when (page) {
                        1 -> page(4, "1")
                        2 -> page(4, "2")
                        else -> page(4, "1")
                    }
                }
            }
        }
    }

    private fun page(total: Int, vararg ids: String): ThreadPage = ThreadPage(
        threads = ids.map { ThreadItem(it, "topic $it", "author", "uid", 0L, replies = 0) },
        totalRows = total
    )
}
