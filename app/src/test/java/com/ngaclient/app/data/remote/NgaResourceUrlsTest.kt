package com.ngaclient.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NgaResourceUrlsTest {
    @Test
    fun `legacy attachment hosts use the canonical attachment host`() {
        assertEquals(
            "https://img.nga.cn/attachments/mon_202608/example.jpg?raw=1",
            NgaResourceUrls.normalizeLegacyImageHosts(
                "http://img6.ngacn.cc/attachments/mon_202608/example.jpg?raw=1"
            )
        )
    }

    @Test
    fun `non-attachment image hosts retain their server number`() {
        assertEquals(
            "https://img4.nga.cn/ngabbs/post/smile/ac0.png",
            NgaResourceUrls.normalizeLegacyImageHosts(
                "https://img4.nga.178.com/ngabbs/post/smile/ac0.png"
            )
        )
    }

    @Test
    fun `forum and current image hosts remain unchanged`() {
        assertEquals(
            "https://nga.178.com/thread.php?fid=-7 https://img.nga.cn/attachments/a.png",
            NgaResourceUrls.normalizeLegacyImageHosts(
                "https://nga.178.com/thread.php?fid=-7 https://img.nga.cn/attachments/a.png"
            )
        )
    }

    @Test
    fun `invalid board identifiers do not produce resource URLs`() {
        assertNull(NgaResourceUrls.boardIcon("not-a-number", null))
    }
}
