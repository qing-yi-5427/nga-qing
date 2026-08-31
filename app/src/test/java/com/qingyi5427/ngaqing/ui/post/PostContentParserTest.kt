package com.qingyi5427.ngaqing.ui.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostContentParserTest {
    @Test
    fun parsesCodeBlockWithoutTreatingUrlsAsLinks() {
        val blocks = PostContentParser.parse("前文[code]val url = \"https://example.com\"[/code]后文")

        assertTrue(blocks.any { it is PostBlock.Code && it.text.contains("val url") })
        assertTrue(blocks.none { it is PostBlock.Link })
    }

    @Test
    fun rendersListItemsAsBullets() {
        val blocks = PostContentParser.parse("[list][*]第一项[*]第二项[/list]")
        val text = blocks.filterIsInstance<PostBlock.Text>().joinToString("") { it.text }

        assertTrue(text.contains("• 第一项"))
        assertTrue(text.contains("• 第二项"))
    }
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
    fun parsesLegacyFlashMp4AsVideoAttachment() {
        val media = PostContentParser.parse(
            "[flash]https://img.nga.cn/attachments/mon_202608/20/example.mp4[/flash]"
        ).filterIsInstance<PostBlock.Media>().single()

        assertEquals("https://img.nga.cn/attachments/mon_202608/20/example.mp4", media.url)
        assertEquals(MediaKind.Video, media.kind)
    }

    @Test
    fun respectsExplicitAudioAttachmentType() {
        val media = PostContentParser.parse(
            "[flash=audio]https://img.nga.cn/attachments/example.bin[/flash]"
        ).filterIsInstance<PostBlock.Media>().single()

        assertEquals(MediaKind.Audio, media.kind)
    }

    @Test
    fun hidesUnsupportedPresentationTagsInsideQuotes() {
        val text = PostContentParser.parse(
            "[quote][color=blue][size=110%]公告[/size][/color][/quote]"
        ).filterIsInstance<PostBlock.Quote>().single()
            .blocks.filterIsInstance<PostBlock.Text>().joinToString("") { it.text }

        assertEquals("公告", text)
    }

    @Test
    fun parsesNgaTableWithoutLeakingBbcodeOrNumericEntities() {
        val table = PostContentParser.parse(
            """[table][tr][td]&#8203;提车记录[/td][td][b]插混顶配[/b][/td][/tr]""" +
                """[tr][td][url=https://example.com]车版专栏[/url][/td][td]纯电中配[/td][/tr][/table]"""
        ).filterIsInstance<PostBlock.Table>().single()

        assertEquals(
            listOf(
                listOf("提车记录", "插混顶配"),
                listOf("车版专栏", "纯电中配")
            ),
            table.rows
        )
    }

    @Test
    fun decodesDecimalAndHexEntitiesInRegularText() {
        val text = PostContentParser.parse("A&#8203;B &#x4E2D;&#25991;")
            .filterIsInstance<PostBlock.Text>().single().text

        assertEquals("AB 中文", text)
    }

    @Test
    fun parsesCollapsibleSectionAndNestedTable() {
        val collapse = PostContentParser.parse(
            """[collapse=对表样本]说明[table][tr][td]车型[/td][td]占比[/td][/tr][/table][/collapse]"""
        ).filterIsInstance<PostBlock.Collapse>().single()

        assertEquals("对表样本", collapse.title)
        assertTrue(collapse.blocks.filterIsInstance<PostBlock.Text>().any { it.text == "说明" })
        assertEquals(
            listOf(listOf("车型", "占比")),
            collapse.blocks.filterIsInstance<PostBlock.Table>().single().rows
        )
    }

    @Test
    fun parsesStandaloneLegacyReplyHeader() {
        val blocks = PostContentParser.parse(
            """Reply to [pid=879834194,47452804,1]Reply[/pid] """ +
                """Post by [uid=67309777]leecervel[/uid] (2026-08-27 20:43)""" +
                """今年论坛还有不少人在说。""",
            replyTargets = mapOf(
                "879834194" to PostReplyTarget(refName = "leecervel", floor = 3)
            )
        )

        val reply = blocks.filterIsInstance<PostBlock.ReplyTo>().single()
        assertEquals("leecervel", reply.refName)
        assertEquals(3, reply.floor)
        assertTrue(blocks.filterIsInstance<PostBlock.Text>().any { it.text.contains("今年论坛") })
    }

    @Test
    fun parsesBoldWrappedLegacyReplyHeader() {
        val reply = PostContentParser.parse(
            """[b]Reply to [pid=1,2,8]Reply[/pid] Post by [uid=42]Alice[/uid] (2026-08-27)[/b]正文""",
            replyTargets = mapOf("1" to PostReplyTarget(refName = "Alice", floor = 8))
        ).filterIsInstance<PostBlock.ReplyTo>().single()

        assertEquals("Alice", reply.refName)
        assertEquals(8, reply.floor)
    }

    @Test
    fun doesNotTreatLegacyReplyPageNumberAsFloor() {
        val reply = PostContentParser.parse(
            """Reply to [pid=879834879,47452804,1]Reply[/pid] """ +
                """Post by [uid=123]周期天王[/uid] (2026-08-27)正文"""
        ).filterIsInstance<PostBlock.ReplyTo>().single()

        assertEquals("周期天王", reply.refName)
        assertEquals(null, reply.floor)
    }

    @Test
    fun parsesEscapedBareHttpsLink() {
        val blocks = PostContentParser.parse("查看 https\\://example.com/path?q=1 然后继续")
        val link = blocks.filterIsInstance<PostBlock.Link>().single()

        assertEquals("https://example.com/path?q=1", link.url)
        assertEquals(link.url, link.label)
        assertTrue(blocks.filterIsInstance<PostBlock.Text>().any { it.text.contains("然后继续") })
    }

    @Test
    fun parsesNamedAndPlainBbcodeLinks() {
        val links = PostContentParser.parse(
            """[url=https://example.com/a]示例页面[/url] [url]https://example.com/b[/url]"""
        ).filterIsInstance<PostBlock.Link>()

        assertEquals(PostBlock.Link("示例页面", "https://example.com/a"), links[0])
        assertEquals(PostBlock.Link("https://example.com/b", "https://example.com/b"), links[1])
    }

    @Test
    fun excludesSentencePunctuationFromBareLink() {
        val blocks = PostContentParser.parse("地址：https://example.com/a。")

        assertEquals("https://example.com/a", blocks.filterIsInstance<PostBlock.Link>().single().url)
        assertEquals("。", blocks.filterIsInstance<PostBlock.Text>().last().text)
    }
}
