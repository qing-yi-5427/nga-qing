package com.qingyi5427.ngaqing.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ServerFavoriteFolderTest {
    @Test fun parsesAccountFoldersWithRealIdsAndDefaultFlag() {
        val folders = parseServerFavoriteFolders(
            """{"data":{"0":{"7":{"id":"7","name":"待读","length":"2"},"1":{"id":1,"name":"默认","length":12,"default":1}}}}"""
        )
        assertEquals(listOf("1", "7"), folders.map { it.id })
        assertEquals(listOf("默认", "待读"), folders.map { it.name })
        assertEquals(listOf(12, 2), folders.map { it.count })
        assertTrue(folders.first().isDefault)
    }

    @Test fun emptyFolderCollectionIsRepresentedAsEmpty() {
        assertTrue(parseServerFavoriteFolders("""{"data":{"0":{}}}""").isEmpty())
    }

    @Test fun paginationFlagAndFalseDefaultAreReadWithoutInventingFolders() {
        val first = parseServerFavoriteFolderPage(
            """{"data":{"0":{"5":{"id":"5","name":"资料","length":1,"default":0}},"1":1}}"""
        )
        assertTrue(first.hasMore)
        assertEquals(false, first.folders.single().isDefault)
        val final = parseServerFavoriteFolderPage("""{"data":{"0":{},"1":0}}""")
        assertEquals(false, final.hasMore)
        assertTrue(final.folders.isEmpty())
    }

    @Test fun errorsAndMalformedEnvelopesNeverInventDefaultFolder() {
        listOf(
            """{"error":{"0":"未登录"},"data":{"0":{}}}""",
            """{"data":{"0":null}}""",
            """{"data":{"0":{"x":{"name":"missing id"}}}}""",
            """{"data":{"0":{"3":{"id":"3","name":"","length":1}}}}""",
            """{"data":{"0":{"3":{"id":"3","name":"x","length":"bad"}}}}""",
            "<html>login required</html>"
        ).forEach { raw ->
            assertThrows(IllegalStateException::class.java) { parseServerFavoriteFolders(raw) }
        }
    }
}
