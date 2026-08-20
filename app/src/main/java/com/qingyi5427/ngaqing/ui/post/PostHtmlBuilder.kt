package com.qingyi5427.ngaqing.ui.post

import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.data.remote.NgaResourceUrls
import com.qingyi5427.ngaqing.ui.util.formatDateTime

/**
 * Builds a single self-contained HTML document for the whole thread page.
 * NGA already returns post content as HTML, so we mostly wrap it with readable
 * styling and dark-mode CSS. Quotes ([quote] / [quotex]) are turned into clean
 * blocks showing only the quoted user, floor and content.
 */
fun buildPostHtml(posts: List<Post>, dark: Boolean, subject: String, users: Map<String, String> = emptyMap()): String {
    val bg = if (dark) "#0f0f12" else "#ffffff"
    val fg = if (dark) "#e2e2e6" else "#1b1b1f"
    val sub = if (dark) "#9a9aa2" else "#5a5a60"
    val card = if (dark) "#1a1a1f" else "#f4f4f7"
    val quoteBg = if (dark) "#232329" else "#ececf2"
    val link = if (dark) "#7aa2ff" else "#1a5cff"
    val border = if (dark) "#2a2a31" else "#e4e4ea"

    val css = """
        * { box-sizing: border-box; }
        body { margin:0; padding:8px; background:$bg; color:$fg;
               font-size:16px; line-height:1.6;
               font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; }
        a { color:$link; text-decoration:none; }
        img { max-width:100%; height:auto; border-radius:8px; display:block; margin:6px 0; }
        blockquote, .quote { margin:8px 0; padding:8px 12px; background:$quoteBg;
               border-left:3px solid $border; border-radius:6px; color:$sub; }
        blockquote .qhead { font-size:12px; color:$link; margin-bottom:4px; }
        hr { border:none; border-top:1px solid $border; margin:8px 0; }
        .post { background:$card; border:1px solid $border; border-radius:12px;
                padding:10px 12px; margin:10px 0; }
        .head { display:flex; justify-content:space-between; align-items:baseline;
                color:$sub; font-size:13px; margin-bottom:6px; }
        .author { color:$fg; font-weight:600; }
        .lou { color:$sub; font-size:12px; }
        .subject { font-weight:600; font-size:17px; margin:2px 0 8px; }
        .content { word-break:break-word; }
        .comments { margin-top:8px; padding-top:8px; border-top:1px dashed $border; }
        .comment { font-size:14px; color:$sub; margin:4px 0; }
        .comment .ca { color:$fg; font-weight:600; }
        .sig { margin-top:8px; color:$sub; font-size:12px; border-top:1px dotted $border; padding-top:6px; }
    """.trimIndent()

    val body = StringBuilder()
    // 标题只由顶栏（GlassTopBar）显示一处，正文不再重复渲染（避免同标题出现三次）
    for (p in posts) {
        body.append("<div class='post'>")
        body.append("<div class='head'><span class='author'>${escape(p.author)}</span>")
        body.append("<span class='lou'>#${p.lou} · ${formatDateTime(p.postDate)}</span></div>")
        body.append("<div class='content'>${quoteToHtml(p.content, users)}</div>")
        if (p.comments.isNotEmpty()) {
            body.append("<div class='comments'>")
            for (c in p.comments) {
                body.append("<div class='comment'><span class='ca'>${escape(c.author)}:</span> ${quoteToHtml(c.content, users)}</div>")
            }
            body.append("</div>")
        }
        if (p.signature.isNotBlank()) {
            body.append("<div class='sig'>${quoteToHtml(p.signature, users)}</div>")
        }
        body.append("</div>")
    }

    return """
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>$css</style></head>
        <body>${body.toString()}</body></html>
    """.trimIndent()
}

private val QUOTE_RE = Regex("""\[quote(?:x)?\]([\s\S]*?)\[/quote(?:x)?\]""")
private val PID_FLOOR_RE = Regex("""\[pid=\d+,tid=\d+(?:,reply=(\d+))?\]""")
private val PID_BLOCK_RE = Regex("""\[pid=\d+,tid=\d+(?:,reply=\d+)?\](?:Reply)?\[/pid\]""")
private val UID_RE = Regex("""\[uid=\d+\]""")
private val POSTBY_RE = Regex("""\[b\]\s*Post\s*by\s*\[uid=\d+\][\s\S]*?\[/b\]""", RegexOption.IGNORE_CASE)
private val SMILE_RE = Regex("""\[s:([^:\]]+):([^:\]]+)\]""")

/**
 * 把 NGA 内容转成可渲染 HTML：
 * - 引用块 [quote] 只保留「引用 @用户名（N楼）：内容」，剥掉 pid/tid/Reply/Post by 等元数据；
 * - [b]/[del]/[url]/[img] 等常见 BBCode；
 * - [s:组:名] 表情通过 [Smiles] 映射表转成表情图片；
 * - 原生 <img> 修正懒加载属性与协议相对地址。
 */
private fun quoteToHtml(content: String, users: Map<String, String>): String {
    var s = content
    s = fixRawImgTags(s)
    // 引用块：从最内层开始逐层处理（每次匹配最短的 [quote]...[/quote]），直到无嵌套
    var guard = 0
    while (guard++ < 20) {
        val prev = s
        s = QUOTE_RE.replace(s) { m -> renderQuote(m.groupValues[1], users) }
        if (s == prev) break
    }
    s = s.replace(Regex("""\[b\]([\s\S]*?)\[/b\]"""), "<b>\$1</b>")
    s = s.replace(Regex("""\[del\]([\s\S]*?)\[/del\]"""), "<del>\$1</del>")
    s = s.replace(Regex("""\[url=([^\]]+)\]([\s\S]*?)\[/url\]"""), "<a href=\"\$1\">\$2</a>")
    // [img]url[/img] and [img=w,h]url[/img] -> <img>
    s = s.replace(Regex("""\[img(?:=[0-9]+,[0-9]+)?\]([\s\S]*?)\[/img\]"""), "<img src=\"\$1\"/>")
    // 表情 BBcode -> 表情图片（映射表），未知表情兜底为可读文字
    s = SMILE_RE.replace(s) { m ->
        val group = m.groupValues[1]
        val name = m.groupValues[2]
        val file = SMILE_FILES["$group:$name"]
        if (file != null) "<img src=\"$SMILE_BASE$file\"/>" else "[表情:$name]"
    }
    return s
}

/** 渲染单个引用块：只保留「引用 @用户名（N楼）：内容」。 */
private fun renderQuote(inner: String, users: Map<String, String>): String {
    val floor = PID_FLOOR_RE.find(inner)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val uid = UID_RE.find(inner)?.groupValues?.get(0)?.removePrefix("[uid=")?.removeSuffix("]")
    val name = if (uid != null) users[uid] ?: uid else null
    var b = inner
        .replace(PID_BLOCK_RE, "")
        .replace(POSTBY_RE, "")
        .replace(META_TAG_RE, "")
    b = quoteToHtml(b, users).trim()
    val head = buildString {
        append("引用")
        if (name != null) append(" @$name")
        if (floor != null) append("（${floor}楼）")
    }
    return if (head == "引用") "<blockquote>$b</blockquote>"
    else "<blockquote><div class='qhead'>$head：</div>$b</blockquote>"
}

private val META_TAG_RE = Regex("""\[(?:uid|tid|fid|stid|pid)=[^\]]*\]""", RegexOption.IGNORE_CASE)

private val RAW_IMG_RE = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
private val ATTR_RE = Regex("""(\w[\w-]*)\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

/**
 * 把正文里原生 <img> 标签整理成 WebView 能直接显示的 <img src>：
 * - 优先用真实地址（src / data-src 等，http(s) 或 // 协议相对均可）；
 * - 协议相对地址（//host/...）补 https:，避免被当作相对路径；
 * - 把已失效的旧图床主机（img*.nga.178.com / img*.ngacn.cc）归一化到 img*.nga.cn；
 * - 去掉 onerror/onload 等无关属性，避免干扰。
 */
private fun fixRawImgTags(html: String): String {
    return RAW_IMG_RE.replace(html) { m ->
        val attrs = ATTR_RE.findAll(m.groupValues[1])
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val candidates = listOf("src", "data-src", "data-original", "ess-data", "file", "data-url")
        var url: String? = null
        for (k in candidates) {
            val v = attrs[k]
            if (!v.isNullOrBlank() && (v.startsWith("http") || v.startsWith("//"))) {
                url = v
                break
            }
        }
        if (url == null) return@replace m.value
        val fixed = if (url.startsWith("//")) "https:$url" else url
        "<img src=\"${NgaResourceUrls.normalizeLegacyImageHosts(fixed)}\"/>"
    }
}

private fun escape(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
