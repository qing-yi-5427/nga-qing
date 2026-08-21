package com.qingyi5427.ngaqing.ui.post

import androidx.compose.runtime.Immutable
import com.qingyi5427.ngaqing.data.remote.NgaResourceUrls

/**
 * 帖子正文的 Compose 可渲染节点。
 */
@Immutable
sealed interface PostBlock {
    data class Text(val text: String, val bold: Boolean = false, val strike: Boolean = false) : PostBlock
    data class Img(val url: String, val emote: Boolean = false) : PostBlock
    data class Media(val url: String, val kind: MediaKind) : PostBlock
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
        """\[quote(?:x)?\][\s\S]*?\[/quote(?:x)?\]|""" +
            """\[s:[^:\]]+:[^:\]]+\]|""" +
            """\[b\][\s\S]*?\[/b\]|\[del\][\s\S]*?\[/del\]|""" +
            """\[url(?:=[^\]]*)?\][\s\S]*?\[/url\]|""" +
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

    fun parse(content: String, users: Map<String, String> = emptyMap()): List<PostBlock> {
        var s = content
        // HTML <img> 归一为统一标记
        s = HTML_IMG_RE.replace(s) { m -> "$IMG_MARK${extractImgUrl(m.value)}$IMG_MARK" }
        // BBCode [img] 归一
        s = IMG_BB_RE.replace(s) { m -> "$IMG_MARK${m.groupValues[1].trim()}$IMG_MARK" }
        // 兜底：裸 IMG 标记里再统一
        return parseInner(s, users, bold = false, strike = false)
    }

    private fun parseInner(input: String, users: Map<String, String>, bold: Boolean, strike: Boolean): List<PostBlock> {
        val out = mutableListOf<PostBlock>()
        var pos = 0
        for (m in COMBINED_RE.findAll(input)) {
            if (m.range.first > pos) {
                out += PostBlock.Text(cleanText(input.substring(pos, m.range.first)), bold, strike)
            }
            val v = m.value
            when {
                v.startsWith("[quote", ignoreCase = true) -> out += renderQuote(v, users)
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
                    if (bm != null) out += parseInner(bm.groupValues[1], users, true, strike)
                }
                v.startsWith("[del]", ignoreCase = true) -> {
                    val dm = DEL_RE.find(v)
                    if (dm != null) out += parseInner(dm.groupValues[1], users, bold, true)
                }
                v.startsWith("[url", ignoreCase = true) -> {
                    val um = URL_RE.find(v) ?: URL_PLAIN_RE.find(v)
                    val inner = um?.groupValues?.getOrNull(um.groupValues.size - 1) ?: ""
                    out += parseInner(inner, users, bold, strike)
                }
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
                    if (bm != null) out += parseInner(bm.groupValues[1], users, true, strike)
                }
                v.startsWith("<i", ignoreCase = true) -> {
                    val im = HTML_ITALIC_RE.find(v)
                    if (im != null) out += parseInner(im.groupValues[1], users, bold, strike)
                }
            }
            pos = m.range.last + 1
        }
        if (pos < input.length) out += PostBlock.Text(cleanText(input.substring(pos)), bold, strike)
        return out
    }

    private fun renderQuote(quote: String, users: Map<String, String>): PostBlock.Quote {
        val m = QUOTE_RE.find(quote) ?: return PostBlock.Quote(emptyList())
        val inner = m.groupValues[1]
        val floor = PID_FLOOR_RE.find(inner)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val uid = UID_RE.find(inner)?.groupValues?.get(0)?.removePrefix("[uid=")?.removeSuffix("]")
        val name = if (uid != null) users[uid] ?: uid else null
        var b = inner
            .replace(PID_BLOCK_RE, "")
            .replace(POSTBY_RE, "")
            .replace(META_TAG_RE, "")
        return PostBlock.Quote(parseInner(b, users, bold = false, strike = false), name, floor)
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
        t = t.replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'")
        return t
    }

}
