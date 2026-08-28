package com.qingyi5427.ngaqing.ui.post

import androidx.compose.runtime.Immutable
import com.qingyi5427.ngaqing.data.remote.NgaResourceUrls

/**
 * 帖子正文的 Compose 可渲染节点。
 */
@Immutable
sealed interface PostBlock {
    data class Text(val text: String, val bold: Boolean = false, val strike: Boolean = false) : PostBlock
    data class Link(val label: String, val url: String) : PostBlock
    data class Img(val url: String, val emote: Boolean = false) : PostBlock
    data class Media(val url: String, val kind: MediaKind) : PostBlock
    data class Table(val rows: List<List<String>>) : PostBlock
    data class Collapse(val title: String?, val blocks: List<PostBlock>) : PostBlock
    data class ReplyTo(val refName: String, val floor: Int?) : PostBlock
    data class Quote(
        val blocks: List<PostBlock>,
        val refName: String? = null,
        val floor: Int? = null
    ) : PostBlock
}

enum class MediaKind { Video, Audio, External }

/** 已在后台准备好的单层正文，避免列表组合阶段解析 HTML/BBCode。 */
@Immutable
data class PostRenderData(
    val body: List<PostBlock>,
    val signature: List<PostBlock>,
    val comments: List<List<PostBlock>>
)

@Immutable
data class PostReplyTarget(val refName: String, val floor: Int)

/**
 * 把 NGA 帖子正文（HTML + BBCode 混合：<img>/[img]/[quote]/[b]/[del]/[s:组:名]/<br/> 等）
 * 解析成 [PostBlock] 列表，供 Compose 直接渲染。
 *
 * 与 PostHtmlBuilder 的正则逻辑同源，但输出结构化节点而非 HTML：
 * - 引用块只保留「引用 @用户名（N楼）：内容」；
 * - 表情 [s:组:名] 通过 [Smiles] 映射表转图片 URL（未知兜底文字）；
 * - HTML <img> 支持 src / 协议相对 // 地址；
 * - 未知 HTML 标签剥除，常见实体解码。
 */
object PostContentParser {

    private const val REPLY_TO_PATTERN =
        """(?:\[b\]\s*)?Reply\s+to\s+\[pid=(\d+),(\d+),(\d+)\](?:Reply)?\[/pid\]\s*""" +
            """Post\s+by\s+\[uid=(\d+)\]([\s\S]*?)\[/uid\]\s*""" +
            """(?:\([^)]+\))?(?:\s*\[/b\])?"""
    private val REPLY_TO_RE = Regex(REPLY_TO_PATTERN, RegexOption.IGNORE_CASE)
    private val QUOTE_RE = Regex("""\[quote(?:x)?\]([\s\S]*?)\[/quote(?:x)?\]""", RegexOption.IGNORE_CASE)
    private val PID_FLOOR_RE = Regex("""\[pid=\d+,tid=\d+(?:,reply=(\d+))?\]""")
    private val PID_BLOCK_RE = Regex("""\[pid=\d+,tid=\d+(?:,reply=\d+)?\](?:Reply)?\[/pid\]""")
    private val UID_RE = Regex("""\[uid=\d+\]""")
    private val POSTBY_RE = Regex("""\[b\]\s*Post\s*by\s*\[uid=\d+\][\s\S]*?\[/b\]""", RegexOption.IGNORE_CASE)
    private val META_TAG_RE = Regex("""\[(?:uid|tid|fid|stid|pid)=[^\]]*\]""", RegexOption.IGNORE_CASE)
    private val SMILE_RE = Regex("""\[s:([^:\]]+):([^:\]]+)\]""")
    private val IMG_BB_RE = Regex("""\[img(?:=[0-9]+,[0-9]+)?\]([^\[\]]+)\[/img\]""", RegexOption.IGNORE_CASE)
    private val BOLD_RE = Regex("""\[b\]([\s\S]*?)\[/b\]""", RegexOption.IGNORE_CASE)
    private val DEL_RE = Regex("""\[del\]([\s\S]*?)\[/del\]""", RegexOption.IGNORE_CASE)
    private val URL_RE = Regex("""\[url=([^\]]+)\]([\s\S]*?)\[/url\]""", RegexOption.IGNORE_CASE)
    private val URL_PLAIN_RE = Regex("""\[url\]([\s\S]*?)\[/url\]""", RegexOption.IGNORE_CASE)
    private val FLASH_RE = Regex(
        """\[flash(?:=(video|audio))?\]([\s\S]*?)\[/flash\]""",
        RegexOption.IGNORE_CASE
    )
    private val TABLE_RE = Regex(
        """\[table(?:=[^\]]*)?\]([\s\S]*?)\[/table\]""",
        RegexOption.IGNORE_CASE
    )
    private val TABLE_ROW_RE = Regex(
        """\[tr(?:=[^\]]*)?\]([\s\S]*?)\[/tr\]""",
        RegexOption.IGNORE_CASE
    )
    private val TABLE_CELL_RE = Regex(
        """\[td(?:=[^\]]*)?\]([\s\S]*?)\[/td\]""",
        RegexOption.IGNORE_CASE
    )
    private val COLLAPSE_RE = Regex(
        """\[collapse(?:=([^\]]*))?\]([\s\S]*?)\[/collapse\]""",
        RegexOption.IGNORE_CASE
    )
    private val HTML_IMG_RE = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_BOLD_RE = Regex("""<b\b[^>]*>([\s\S]*?)</b\s*>""", RegexOption.IGNORE_CASE)
    private val HTML_ITALIC_RE = Regex("""<i\b[^>]*>([\s\S]*?)</i\s*>""", RegexOption.IGNORE_CASE)
    private val BR_RE = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val ANY_TAG_RE = Regex("""<[^>]+>""")
    private val PRESENTATION_TAG_RE = Regex(
        """\[/?(?:color|size|font|align|u)(?:=[^\]]*)?\]""",
        RegexOption.IGNORE_CASE
    )
    private val COMBINED_RE = Regex(
        REPLY_TO_PATTERN + "|" +
            """\[collapse(?:=[^\]]*)?\][\s\S]*?\[/collapse\]|""" +
            """\[table(?:=[^\]]*)?\][\s\S]*?\[/table\]|""" +
            """\[quote(?:x)?\][\s\S]*?\[/quote(?:x)?\]|""" +
            """\[s:[^:\]]+:[^:\]]+\]|""" +
            """\[b\][\s\S]*?\[/b\]|\[del\][\s\S]*?\[/del\]|""" +
            """\[url(?:=[^\]]*)?\][\s\S]*?\[/url\]|""" +
            """https?://[^\s\[\]<>\"']+|""" +
            """\[flash(?:=(?:video|audio))?\][\s\S]*?\[/flash\]|""" +
            """\u0001IMG\u0002[\s\S]*?\u0001IMG\u0002|""" +
            """<br\s*/?>|<b\b[^>]*>[\s\S]*?</b\s*>|<i\b[^>]*>[\s\S]*?</i\s*>""",
        RegexOption.IGNORE_CASE
    )
    private val IMG_ATTR_RE = Regex(
        """(?:src|data-src|data-original|ess-data|file|data-url)\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    private const val IMG_MARK = "\u0001IMG\u0002"

    fun parse(
        content: String,
        users: Map<String, String> = emptyMap(),
        replyTargets: Map<String, PostReplyTarget> = emptyMap()
    ): List<PostBlock> {
        var s = content
            .replace(Regex("""(?i)\b(https?)\\://"""), "$1://")
            .replace(Regex("""(?i)\b(https?):\\/\\/"""), "$1://")
        // HTML <img> 归一为统一标记
        s = HTML_IMG_RE.replace(s) { m -> "$IMG_MARK${extractImgUrl(m.value)}$IMG_MARK" }
        // BBCode [img] 归一
        s = IMG_BB_RE.replace(s) { m -> "$IMG_MARK${m.groupValues[1].trim()}$IMG_MARK" }
        // 兜底：裸 IMG 标记里再统一
        return parseInner(s, users, replyTargets, bold = false, strike = false)
    }

    private fun parseInner(
        input: String,
        users: Map<String, String>,
        replyTargets: Map<String, PostReplyTarget>,
        bold: Boolean,
        strike: Boolean
    ): List<PostBlock> {
        val out = mutableListOf<PostBlock>()
        var pos = 0
        for (m in COMBINED_RE.findAll(input)) {
            if (m.range.first > pos) {
                out += PostBlock.Text(cleanText(input.substring(pos, m.range.first)), bold, strike)
            }
            val v = m.value
            when {
                v.contains("Reply to", ignoreCase = true) ->
                    renderReplyTo(v, users, replyTargets)?.let(out::add)
                v.startsWith("[collapse", ignoreCase = true) ->
                    out += renderCollapse(v, users, replyTargets)
                v.startsWith("[table", ignoreCase = true) -> renderTable(v)?.let(out::add)
                v.startsWith("[quote", ignoreCase = true) -> out += renderQuote(v, users, replyTargets)
                v.startsWith("[s:", ignoreCase = true) -> {
                    val sm = SMILE_RE.find(v)
                    if (sm != null) {
                        val file = SMILE_FILES["${sm.groupValues[1]}:${sm.groupValues[2]}"]
                        if (file != null) out += PostBlock.Img(SMILE_BASE + file, emote = true)
                        else out += PostBlock.Text("[表情:${sm.groupValues[2]}]", bold, strike)
                    }
                }
                v.startsWith("[b]", ignoreCase = true) -> {
                    val bm = BOLD_RE.find(v)
                    if (bm != null) out += parseInner(bm.groupValues[1], users, replyTargets, true, strike)
                }
                v.startsWith("[del]", ignoreCase = true) -> {
                    val dm = DEL_RE.find(v)
                    if (dm != null) out += parseInner(dm.groupValues[1], users, replyTargets, bold, true)
                }
                v.startsWith("[url", ignoreCase = true) -> {
                    val assigned = URL_RE.find(v)
                    val plain = if (assigned == null) URL_PLAIN_RE.find(v) else null
                    val rawUrl = assigned?.groupValues?.get(1) ?: plain?.groupValues?.get(1).orEmpty()
                    val url = normalizeLinkUrl(rawUrl)
                    val label = cleanText(assigned?.groupValues?.get(2) ?: rawUrl).ifBlank { url }
                    if (url.isNotEmpty()) {
                        out += PostBlock.Link(label, url)
                    } else {
                        out += PostBlock.Text(label, bold, strike)
                    }
                }
                v.startsWith("http://", ignoreCase = true) ||
                    v.startsWith("https://", ignoreCase = true) -> renderBareLink(v, bold, strike, out)
                v.startsWith("[flash", ignoreCase = true) -> {
                    val fm = FLASH_RE.find(v)
                    if (fm != null) {
                        val rawUrl = fm.groupValues[2].trim().replace("&amp;", "&")
                        val url = NgaResourceUrls.normalizeLegacyImageHosts(
                            if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl
                        )
                        val explicitKind = fm.groupValues[1].lowercase()
                        val kind = when {
                            explicitKind == "video" -> MediaKind.Video
                            explicitKind == "audio" -> MediaKind.Audio
                            url.substringBefore('?').endsWith(".mp4", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".webm", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".mov", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".m3u8", ignoreCase = true) -> MediaKind.Video
                            url.substringBefore('?').endsWith(".mp3", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".m4a", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".aac", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".wav", ignoreCase = true) ||
                                url.substringBefore('?').endsWith(".ogg", ignoreCase = true) -> MediaKind.Audio
                            else -> MediaKind.External
                        }
                        if (url.startsWith("http://", ignoreCase = true) ||
                            url.startsWith("https://", ignoreCase = true)
                        ) {
                            out += PostBlock.Media(url, kind)
                        } else {
                            out += PostBlock.Text("[媒体附件暂不可用]", bold, strike)
                        }
                    }
                }
                v.startsWith("\u0001IMG\u0002", ignoreCase = true) -> {
                    val inner = v.removePrefix(IMG_MARK).removeSuffix(IMG_MARK).trim()
                    if (inner.isNotEmpty()) {
                        out += PostBlock.Img(NgaResourceUrls.normalizeLegacyImageHosts(inner))
                    }
                }
                v.startsWith("<br", ignoreCase = true) -> out += PostBlock.Text("\n", bold, strike)
                v.startsWith("<b", ignoreCase = true) -> {
                    val bm = HTML_BOLD_RE.find(v)
                    if (bm != null) out += parseInner(bm.groupValues[1], users, replyTargets, true, strike)
                }
                v.startsWith("<i", ignoreCase = true) -> {
                    val im = HTML_ITALIC_RE.find(v)
                    if (im != null) out += parseInner(im.groupValues[1], users, replyTargets, bold, strike)
                }
            }
            pos = m.range.last + 1
        }
        if (pos < input.length) out += PostBlock.Text(cleanText(input.substring(pos)), bold, strike)
        return out
    }

    private fun renderBareLink(
        raw: String,
        bold: Boolean,
        strike: Boolean,
        out: MutableList<PostBlock>
    ) {
        val urlText = raw.trimEnd('.', ',', ';', '!', '?', ')', ']', '}', '，', '。', '；', '！', '？', '）', '】')
        val suffix = raw.substring(urlText.length)
        val url = normalizeLinkUrl(urlText)
        if (url.isNotEmpty()) out += PostBlock.Link(url, url)
        else out += PostBlock.Text(urlText, bold, strike)
        if (suffix.isNotEmpty()) out += PostBlock.Text(suffix, bold, strike)
    }

    private fun normalizeLinkUrl(raw: String): String {
        val url = decodeEntities(raw).trim().replace("\\:", ":").replace("\\/", "/")
        return url.takeIf {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        }.orEmpty()
    }

    private fun renderReplyTo(
        reply: String,
        users: Map<String, String>,
        replyTargets: Map<String, PostReplyTarget>
    ): PostBlock.ReplyTo? {
        val match = REPLY_TO_RE.find(reply) ?: return null
        val target = replyTargets[match.groupValues[1]]
        val uid = match.groupValues[4]
        val inlineName = decodeEntities(match.groupValues[5]).trim()
        return PostBlock.ReplyTo(
            refName = inlineName.ifEmpty { target?.refName ?: users[uid] ?: uid },
            // pid 的第三个参数是页码，不是楼层；楼层只能由目标帖子的 PID 映射得到。
            floor = target?.floor
        )
    }

    private fun renderCollapse(
        collapse: String,
        users: Map<String, String>,
        replyTargets: Map<String, PostReplyTarget>
    ): PostBlock.Collapse {
        val match = COLLAPSE_RE.find(collapse)
            ?: return PostBlock.Collapse(title = null, blocks = emptyList())
        val title = decodeEntities(match.groupValues[1]).trim().ifEmpty { null }
        return PostBlock.Collapse(
            title = title,
            blocks = parseInner(
                match.groupValues[2], users, replyTargets, bold = false, strike = false
            )
        )
    }

    /**
     * NGA 的表格在窄屏上不适合照搬网页宽度，这里保留行列语义并交给 Compose
     * 做移动端排版。格式不完整时返回 null，让原文继续走普通文本兜底。
     */
    private fun renderTable(table: String): PostBlock.Table? {
        val body = TABLE_RE.find(table)?.groupValues?.getOrNull(1) ?: return null
        val rows = TABLE_ROW_RE.findAll(body).mapNotNull { rowMatch ->
            val cells = TABLE_CELL_RE.findAll(rowMatch.groupValues[1])
                .map { cleanTableCell(it.groupValues[1]) }
                .toList()
            cells.takeIf { it.isNotEmpty() }
        }.toList()
        return rows.takeIf { it.isNotEmpty() }?.let(PostBlock::Table)
    }

    private fun cleanTableCell(cell: String): String {
        var text = BR_RE.replace(cell, "\n")
        text = URL_RE.replace(text) { it.groupValues[2] }
        text = URL_PLAIN_RE.replace(text) { it.groupValues[1] }
        text = SMILE_RE.replace(text) { "[表情:${it.groupValues[2]}]" }
        text = IMG_BB_RE.replace(text, "[图片]")
        text = Regex(
            """\[/?(?:b|del|color|size|font|align|u|i)(?:=[^\]]*)?\]""",
            RegexOption.IGNORE_CASE
        ).replace(text, "")
        return cleanText(text).trim()
    }

    private fun renderQuote(
        quote: String,
        users: Map<String, String>,
        replyTargets: Map<String, PostReplyTarget>
    ): PostBlock.Quote {
        val m = QUOTE_RE.find(quote) ?: return PostBlock.Quote(emptyList())
        val inner = m.groupValues[1]
        val floor = PID_FLOOR_RE.find(inner)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val uid = UID_RE.find(inner)?.groupValues?.get(0)?.removePrefix("[uid=")?.removeSuffix("]")
        val name = if (uid != null) users[uid] ?: uid else null
        var b = inner
            .replace(PID_BLOCK_RE, "")
            .replace(POSTBY_RE, "")
            .replace(META_TAG_RE, "")
        return PostBlock.Quote(
            parseInner(b, users, replyTargets, bold = false, strike = false),
            name,
            floor
        )
    }

    /** 提取 <img> 标签里的真实地址（src / data-src / 协议相对 / 裸主机）。 */
    private fun extractImgUrl(tag: String): String {
        val m = IMG_ATTR_RE.find(tag) ?: return ""
        var url = m.groupValues[1]
        if (url.startsWith("//")) url = "https:$url"
        return url
    }

    private fun cleanText(s: String): String {
        var t = ANY_TAG_RE.replace(s, "")
        // NGA 的历史正文常混有只负责展示的 BBCode。当前原生渲染器不复刻
        // 任意字号/颜色，但也不应把这些标记当正文展示给用户。
        t = PRESENTATION_TAG_RE.replace(t, "")
        t = decodeEntities(t)
        // 零宽空格常由 NGA 编辑器插入用于占位，不应影响换行或被复制出来。
        t = t.replace("\u200B", "")
        return t
    }

    private fun decodeEntities(source: String): String {
        var text = source.replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'")
        text = Regex("""&#(?:x([0-9a-fA-F]+)|(\d+));""").replace(text) { match ->
            val codePoint = match.groupValues[1].takeIf(String::isNotEmpty)?.toIntOrNull(16)
                ?: match.groupValues[2].toIntOrNull()
            if (codePoint != null && Character.isValidCodePoint(codePoint)) {
                String(Character.toChars(codePoint))
            } else {
                match.value
            }
        }
        return text
    }

}
