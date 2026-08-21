package com.qingyi5427.ngaqing.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTagTest {
    @Test
    fun `relative legacy flash video receives attachment host`() {
        assertEquals(
            "[flash]https://img.nga.cn/attachments/mon_202608/20/example.mp4[/flash]",
            absolutizeNgaMediaTags(
                "[flash]./mon_202608/20/example.mp4[/flash]",
                "https://img.nga.cn/attachments/"
            )
        )
    }

    @Test
    fun `attachments path is not duplicated`() {
        assertEquals(
            "[flash=audio]https://img.nga.cn/attachments/example.mp3[/flash]",
            absolutizeNgaMediaTags(
                "[flash=audio]./attachments/example.mp3[/flash]",
                "https://img.nga.cn/attachments/"
            )
        )
    }
}
