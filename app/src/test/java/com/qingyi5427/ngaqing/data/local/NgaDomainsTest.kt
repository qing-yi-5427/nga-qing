package com.qingyi5427.ngaqing.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaDomainsTest {
    @Test
    fun `missing and unknown hosts use main site`() {
        assertEquals(NgaDomains.DEFAULT_HOST, NgaDomains.normalizeHost(null))
        assertEquals(NgaDomains.DEFAULT_HOST, NgaDomains.normalizeHost("example.com"))
    }

    @Test
    fun `stored URL is normalized to supported host`() {
        assertEquals("ngabbs.com", NgaDomains.normalizeHost("https://NGABBS.com/read.php"))
    }

    @Test
    fun `page URL uses selected origin`() {
        assertEquals(
            "https://nga.178.com/read.php?tid=123",
            NgaDomains.url("nga.178.com", "/read.php?tid=123")
        )
    }

    @Test
    fun `attachment hosts are not treated as interchangeable forum origins`() {
        assertTrue(NgaDomains.isForumHost("BBS.NGA.CN"))
        assertTrue(NgaDomains.isForumHost("ngabbs.com"))
        assertFalse(NgaDomains.isForumHost("img.nga.cn"))
    }
}
