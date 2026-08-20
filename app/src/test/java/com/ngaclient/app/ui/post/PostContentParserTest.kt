package com.ngaclient.app.ui.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostContentParserTest {
    @Test
    fun parsesTextAndFormatting() {
        val blocks = PostContentParser.parse("你好[b]论坛[/b][del]旧内容[/del]")
        assertTrue(blocks.filterIsInstance<PostBlock.Text>().any { it.text == "你好" })
        assertTrue(blocks.filterIsInstance<PostBlock.Text>().any { it.text == "论坛" && it.bold })
        assertTrue(blocks.filterIsInstance<PostBlock.Text>().any { it.text == "旧内容" && it.strike })
    }

    @Test
    fun normalizesLegacyAttachmentHost() {
        val image = PostContentParser.parse(
            "[img]https://img.nga.178.com/attachments/example.jpg[/img]"
        ).filterIsInstance<PostBlock.Img>().single()
        assertEquals("https://img.nga.cn/attachments/example.jpg", image.url)
    }

    @Test
    fun extractsQuoteAuthorAndFloor() {
        val quote = PostContentParser.parse(
            "[quote][pid=1,tid=2,reply=8]Reply[/pid][b]Post by [uid=42]Alice[/b]内容[/quote]",
            users = mapOf("42" to "Alice")
        ).filterIsInstance<PostBlock.Quote>().single()
        assertEquals("Alice", quote.refName)
        assertEquals(8, quote.floor)
        assertTrue(quote.blocks.filterIsInstance<PostBlock.Text>().any { it.text.contains("内容") })
    }

    @Test
    fun unknownSmileHasReadableFallback() {
        val text = PostContentParser.parse("[s:unknown:missing]")
            .filterIsInstance<PostBlock.Text>().single()
        assertEquals("[表情:missing]", text.text)
    }

    @Test
    fun hidesUnsupportedPresentationTagsInsideQuotes() {
        val text = PostContentParser.parse(
            "[quote][color=blue][size=110%]公告[/size][/color][/quote]"
        ).filterIsInstance<PostBlock.Quote>().single()
            .blocks.filterIsInstance<PostBlock.Text>().joinToString("") { it.text }

        assertEquals("公告", text)
    }
}
