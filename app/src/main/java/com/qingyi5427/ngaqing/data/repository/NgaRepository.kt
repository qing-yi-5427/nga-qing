package com.qingyi5427.ngaqing.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.FavoriteBoardEntity
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.local.ResponseCacheEntity
import com.qingyi5427.ngaqing.data.local.DraftEntity
import com.qingyi5427.ngaqing.data.local.WatchedThreadEntity
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.data.model.Post
import com.qingyi5427.ngaqing.data.model.PostComment
import com.qingyi5427.ngaqing.data.model.PostPage
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.ThreadPage
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.data.model.UserProfile
import com.qingyi5427.ngaqing.data.remote.NgaApi
import com.qingyi5427.ngaqing.data.remote.NgaResourceUrls
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong

/**
 * Normalize the different avatar URL shapes returned by NGA's user dictionary.
 * Older accounts can still point at retired nga.178.com/ngacn.cc image hosts,
 * while some responses omit the scheme (or even the host) entirely.
 */
internal fun normalizeNgaAvatarUrl(raw: String): String {
    val value = raw.trim().replace("&amp;", "&").replace("\\/", "/")
    if (value.isBlank()) return ""

    // Older NGA accounts may return the avatar field as a JSON-like history object
    // instead of a URL. Use the first image URL, matching the official client's logic.
    val candidate = Regex("""https?://[^"\\\s}]+""", RegexOption.IGNORE_CASE)
        .find(value)?.value ?: value

    val absolute = when {
        candidate.startsWith("//") -> "https:$candidate"
        candidate.startsWith("http://", ignoreCase = true) ||
            candidate.startsWith("https://", ignoreCase = true) -> candidate
        Regex("""^img\d*\.(nga\.cn|nga\.178\.com|ngacn\.cc)/""", RegexOption.IGNORE_CASE)
            .containsMatchIn(candidate) -> "https://$candidate"
        candidate.startsWith("/") -> "https://img.nga.cn$candidate"
        else -> "https://img.nga.cn/${candidate.removePrefix("./")}"
    }

    val migrated = absolute
        .replace(
            Regex("""^http://img(\d*)\.(nga\.178\.com|ngacn\.cc|nga\.cn)""", RegexOption.IGNORE_CASE),
            "https://img\$1.nga.cn"
        )
        .replace(
            Regex("""^https://img(\d*)\.(nga\.178\.com|ngacn\.cc)""", RegexOption.IGNORE_CASE),
            "https://img\$1.nga.cn"
        )

    // The current img/img4 NGA avatar endpoints return HTTP 567 over TLS while the
    // identical public avatar responds with image/jpeg over HTTP. Cleartext access is
    // restricted to these two image hosts in network_security_config.xml.
    return migrated.replace(
        Regex("""^https://(img4?\.nga\.cn)(/avatars/)""", RegexOption.IGNORE_CASE)
    ) { match -> "http://${match.groupValues[1]}${match.groupValues[2]}" }
}

/** Resolve relative media URLs hidden inside NGA's legacy [flash] attachment tags. */
internal fun absolutizeNgaMediaTags(content: String, attachmentsPrefix: String): String {
    if (attachmentsPrefix.isBlank()) return content
    return content.replace(
        Regex(
            """(\[flash(?:=(?:video|audio))?\])\s*\./((?:attachments/|mon_\d{6}/)[^\[\r\n]+?)\s*(\[/flash\])""",
            RegexOption.IGNORE_CASE
        )
    ) { match ->
        val relative = if (attachmentsPrefix.endsWith("/attachments/", ignoreCase = true)) {
            match.groupValues[2].removePrefix("attachments/")
        } else {
            match.groupValues[2]
        }
        "${match.groupValues[1]}$attachmentsPrefix$relative${match.groupValues[3]}"
    }
}

internal fun buildBoardGroup(
    categoryName: String,
    groupName: String,
    boards: List<Board>
): BoardGroup {
    val parent = boards.firstOrNull {
        normalizeBoardName(it.name) == normalizeBoardName(groupName)
    }
    return BoardGroup(
        categoryName = categoryName,
        groupName = groupName,
        parent = parent,
        children = if (parent == null) boards else boards.filterNot {
            it.fid == parent.fid && it.stid == parent.stid
        }
    )
}

private fun normalizeBoardName(name: String): String =
    name.trim().removeSurrounding("《", "》").replace(" ", "")

/**
 * Single source of truth for NGA data: network + local cache (favorites).
 * All network responses are GBK-decoded Strings (see [com.qingyi5427.ngaqing.data.remote.GbkConverterFactory]).
 */
@Singleton
class NgaRepository @Inject constructor(
    private val api: NgaApi,
    private val prefs: UserPreferences,
    private val db: AppDatabase,
    @ApplicationContext private val context: Context
) {

    /** 缓存持久化不阻塞网络结果交付；同一把锁保证“清空缓存”不会和后台写入交错。 */
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheMutex = Mutex()
    private val lastCachePruneAt = AtomicLong(0L)
    private val cacheGeneration = AtomicLong(0L)

    // ---------- Board tree (app_api home category) ----------

    suspend fun getCategory(): Result<List<BoardGroup>> = runCatching {
        val cacheKey = "category"
        val network = runCatching { api.homeCategory() }
        val raw = network.getOrElse {
            db.responseCacheDao().get(cacheKey)?.payload ?: throw it
        }
        val parsed = parseCategoryOffMain(raw)
        if (network.isSuccess && parsed.isNotEmpty()) cacheResponse(cacheKey, raw)
        parsed
    }

    private fun parseCategory(json: String): List<BoardGroup> {
        val root = JSONObject(json)
        val data = root.optJSONObject("data") ?: return emptyList()
        // app_api 仍提供旧版图标表，可作为非数字 fid 的兼容兜底。
        // 常规 fid / 合集 stid 优先使用 NGA 当前稳定的标准图标路径。
        val other = root.optJSONObject("other")
        val iconMap = parseForumIcons(other)
        val iconBase = (other?.optString("forum_icon_pre", "") ?: "")
            .replaceFirst("http://", "https://")   // 图床同时支持 https，统一使用
            .trimEnd('/')
        val groups = mutableListOf<BoardGroup>()
        val catKeys = data.keys().asSequence().toList()
        for (catKey in catKeys) {
            val cat = data.optJSONObject(catKey) ?: continue
            val categoryName = cat.optString("name", catKey)
            val groupObj = cat.optJSONObject("groups") ?: continue
            val groupKeys = groupObj.keys().asSequence().toList()
            for (gKey in groupKeys) {
                val g = groupObj.optJSONObject(gKey) ?: continue
                val groupName = g.optString("name", gKey)
                val forumsObj = g.optJSONObject("forums") ?: continue
                val boards = mutableListOf<Board>()
                val fKeys = forumsObj.keys().asSequence().toList()
                for (fKey in fKeys) {
                    val f = forumsObj.optJSONObject(fKey) ?: continue
                    val stid = f.optString("stid", "").takeIf { it.isNotBlank() }
                    val fid = f.optString("fid", "")
                    val icon = iconMap[fid]
                    boards.add(
                        Board(
                            fid = fid,
                            name = f.optString("name", ""),
                            info = f.optString("info", ""),
                            groupName = groupName,
                            categoryName = categoryName,
                            stid = stid,
                            iconUrl = NgaResourceUrls.boardIcon(fid, stid)
                                ?: icon?.takeIf { !it.startsWith("-") }
                                    ?.let { if (iconBase.isNotBlank()) "$iconBase/$it.png" else null },
                            iconColor = icon?.takeIf { it.startsWith("-") }?.toIntOrNull()
                        )
                    )
                }
                if (boards.isNotEmpty()) {
                    // category 接口没有显式 parent_fid；母板块会以“与 group 同名的 forum”
                    // 出现在该组中。其余 group 只是不可点击的分类标题。
                    groups.add(buildBoardGroup(categoryName, groupName, boards))
                }
            }
        }
        return groups
    }

    /**
     * 解析 other.forum_icon_list 的字符串分组（f/s/c），返回 fid -> icon 字符串映射。
     * 配对规则：元素为 "?fid" 时，其前一个元素是对应 icon（正数=图片编号，负数=ARGB 颜色）。
     */
    private fun parseForumIcons(other: JSONObject?): Map<String, String> {
        if (other == null) return emptyMap()
        val list = other.optJSONObject("forum_icon_list") ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        for (gk in list.keys()) {
            val v = list.optString(gk, "")
            if (v.isBlank()) continue
            val parts = v.split(",")
            for (i in 1 until parts.size) {
                val p = parts[i].trim()
                if (p.startsWith("?")) {
                    map[p.substring(1)] = parts[i - 1].trim()
                }
            }
        }
        return map
    }

    // ---------- Thread list (thread.php) ----------

    suspend fun getThreads(
        fid: String,
        stid: String? = null,
        page: Int = 1,
        authorId: String? = null,
        recommendedOnly: Boolean = false,
        sortByPostDate: Boolean = false
    ): ThreadPage {
        val cacheKey = listOf(
            "threads", fid, stid.orEmpty(), page.toString(), authorId.orEmpty(),
            recommendedOnly.toString(), sortByPostDate.toString()
        ).joinToString(":")
        // NGA 偶发返回被截断的 JSON（响应不完整），解析/传输类失败自动重试
        var last: ThreadPage? = null
        repeat(3) { attempt ->
            val r = runCatching {
                parseThreadsOffMain(
                    api.threadList(
                        fid = fid.takeUnless { stid != null },
                        stid = stid,
                        page = page,
                        authorId = authorId,
                        recommend = 1.takeIf { recommendedOnly },
                        orderBy = "postdatedesc".takeIf { recommendedOnly || sortByPostDate },
                        user = 1.takeIf { recommendedOnly }
                    )
                )
            }.getOrElse { ThreadPage(error = it.message ?: "error", raw = "") }
            if (!isRetryableError(r.error)) {
                if (r.error == null && r.raw.isNotBlank()) cacheResponse(cacheKey, r.raw)
                return r
            }
            last = r
            if (attempt < 2) delay(600L * (attempt + 1))
        }
        val cached = db.responseCacheDao().get(cacheKey)
        if (cached != null) {
            val parsed = parseThreadsOffMain(cached.payload)
            if (parsed.error == null) return parsed.copy(fromCache = true, cachedAt = cached.updatedAt)
        }
        return last ?: ThreadPage(error = "加载失败", raw = "")
    }

    private fun parseThreads(json: String): ThreadPage {
        val clean = preprocess(json)
        return try {
            val root = JSONObject(clean)
            val data = root.optJSONObject("data")
            if (data == null) {
                return ThreadPage(error = errorOf(root, clean), raw = clean)
            }
            val forum = data.optJSONObject("__F")
            val forumName = forum?.optString("name") ?: ""
            val parentFid = forum?.optString("fid", "").orEmpty()
            val subBoards = parseSubBoards(forum?.opt("sub_forums"), parentFid, forumName)
            val tObj = data.optJSONObject("__T")
            if (tObj == null) {
                return ThreadPage(error = errorOf(root, clean), raw = clean)
            }
            val total = data.optInt("__ROWS", 0)
            // 用户字典 __U: uid -> {username, avatar,...}，用于帖子列表显示真实头像
            val uObj = data.optJSONObject("__U")
            val avatarByUid: Map<String, String> = if (uObj != null) {
                uObj.keys().asSequence().mapNotNull { k ->
                    val u = uObj.optJSONObject(k) ?: return@mapNotNull null
                    val av = normalizeNgaAvatarUrl(u.optString("avatar", ""))
                    if (av.isBlank()) null else k to av
                }.toMap()
            } else emptyMap()
            val threads = mutableListOf<ThreadItem>()
            val keys = tObj.keys().asSequence().toList()
            for (k in keys) {
                val t = tObj.optJSONObject(k) ?: continue
                val authorId = t.optString("authorid", "")
                threads.add(
                    ThreadItem(
                        tid = t.optString("tid", ""),
                        subject = stripTags(t.optString("subject", "")),
                        author = t.optString("author", ""),
                        authorId = authorId,
                        postDate = t.optLong("postdate", 0L),
                        lastPostDate = t.optLong("lastpost", t.optLong("postdate", 0L)),
                        replies = t.optInt("replies", 0),
                        lastPoster = t.optString("lastposter", ""),
                        type = t.optString("type", ""),
                        recommend = t.optInt("recommend", 0),
                        forumName = forumName,
                        avatar = avatarByUid[authorId] ?: ""
                    )
                )
            }
            ThreadPage(threads = threads, subBoards = subBoards, totalRows = total, raw = clean)
        } catch (e: Exception) {
            dumpJsonError(json, clean, e)
            ThreadPage(error = "JSON解析失败：${e.message}", raw = clean)
        }
    }

    /**
     * NGA 的 sub_forums 同时承载普通子板块和主题合集：
     * 普通 key 使用 fid；以 t 开头的 key 使用 stid。字段 0/1/2 分别为 id、名称、说明。
     */
    private fun parseSubBoards(value: Any?, parentFid: String, forumName: String): List<Board> {
        val obj = when (value) {
            is JSONObject -> value
            is String -> value.takeIf { it.isNotBlank() }?.let {
                runCatching { JSONObject(it) }.getOrNull()
            }
            else -> null
        } ?: return emptyList()

        return obj.keys().asSequence().mapNotNull { key ->
            val item = obj.optJSONObject(key) ?: return@mapNotNull null
            val id = item.optString("0", "").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val isCollection = key.startsWith("t", ignoreCase = true)
            Board(
                fid = if (isCollection) parentFid else id,
                stid = id.takeIf { isCollection },
                name = item.optString("1", "").ifBlank { return@mapNotNull null },
                info = item.optString("2", ""),
                groupName = forumName,
                categoryName = forumName,
                iconUrl = NgaResourceUrls.boardIcon(
                    if (isCollection) parentFid else id,
                    id.takeIf { isCollection }
                )
            )
        }.toList()
    }

    // ---------- Post page (read.php) ----------

    suspend fun getPosts(
        tid: String,
        page: Int = 1,
        authorId: String? = null
    ): PostPage {
        val cacheKey = "posts:$tid:$page:${authorId.orEmpty()}"
        // NGA 偶发返回被截断的 JSON（响应不完整），解析/传输类失败自动重试
        var last: PostPage? = null
        repeat(3) { attempt ->
            val r = runCatching {
                parsePostsOffMain(api.read(tid, page, authorId = authorId))
            }.getOrElse { PostPage(error = it.message ?: "error", raw = "") }
            if (!isRetryableError(r.error)) {
                if (r.error == null && r.raw.isNotBlank()) cacheResponse(cacheKey, r.raw)
                return r
            }
            last = r
            if (attempt < 2) delay(600L * (attempt + 1))
        }
        val cached = db.responseCacheDao().get(cacheKey)
        if (cached != null) {
            val parsed = parsePostsOffMain(cached.payload)
            if (parsed.error == null) return parsed.copy(fromCache = true, cachedAt = cached.updatedAt)
        }
        return last ?: PostPage(error = "加载失败", raw = "")
    }

    /** 判定错误是否属于「可重试」的解析/传输类失败（NGA 偶发截断响应）。业务错误（帖子不存在等）不重试。 */
    private fun isRetryableError(e: String?): Boolean {
        if (e.isNullOrBlank()) return false
        val m = e.lowercase()
        return m.contains("json") || m.contains("unterminated") ||
            m.contains("unexpected end") || m.contains("end of input") ||
            m.contains("eof") || m.contains("truncat") ||
            m.contains("timeout") || m.contains("connect") || m.contains("socket")
    }

    private fun parsePosts(json: String): PostPage {
        val clean = preprocess(json)
        return try {
            val root = JSONObject(clean)
            val data = root.optJSONObject("data")
            if (data == null) {
                return PostPage(error = errorOf(root, clean), raw = clean)
            }
            val rObj = data.optJSONObject("__R")
            if (rObj == null) {
                return PostPage(error = errorOf(root, clean), raw = clean)
            }
            val count = data.optInt("__R__ROWS", rObj.length())
            val totalRows = data.optInt("__ROWS", 0)
            val thread = data.optJSONObject("__T")
            val attachPrefix = resolveAttachPrefix(data)
            // 用户字典 __U: uid -> {username, avatar,...}，用于评论作者名 / 引用 @用户名 / 帖子头像
            val uObj = data.optJSONObject("__U")
            val nameByUid: Map<String, String> = if (uObj != null) {
                uObj.keys().asSequence().mapNotNull { k ->
                    val u = uObj.optJSONObject(k) ?: return@mapNotNull null
                    val nm = u.optString("username", "")
                    if (nm.isBlank()) null else k to nm
                }.toMap()
            } else emptyMap()
            val avatarByUid: Map<String, String> = if (uObj != null) {
                uObj.keys().asSequence().mapNotNull { k ->
                    val u = uObj.optJSONObject(k) ?: return@mapNotNull null
                    val av = normalizeNgaAvatarUrl(u.optString("avatar", ""))
                    if (av.isBlank()) null else k to av
                }.toMap()
            } else emptyMap()
            val posts = mutableListOf<Post>()
            for (i in 0 until count) {
                val r = rObj.optJSONObject(i.toString()) ?: continue
                val content = r.optString("content", r.optString("subject", ""))
                val authorId = r.optString("authorid", "")
                posts.add(
                    Post(
                        tid = r.optString("tid", ""),
                        pid = r.optString("pid", ""),
                        author = r.optString("author", "").ifBlank { nameByUid[authorId] ?: authorId },
                        authorId = authorId,
                        subject = r.optString("subject", ""),
                        postDate = parsePostDate(r.optString("postdate", "")),
                        lou = r.optInt("lou", i + 1),
                        content = rewriteImages(content, attachPrefix),
                        signature = rewriteImages(r.optString("signature", ""), attachPrefix),
                        comments = parseComments(r.optJSONObject("comment"), attachPrefix, nameByUid),
                        avatar = avatarByUid[r.optString("authorid", "")] ?: "",
                        fid = r.optString("fid", "")
                    )
                )
            }
            PostPage(
                posts = posts,
                subject = stripTags(thread?.optString("subject", "").orEmpty()),
                author = thread?.optString("author", "").orEmpty(),
                authorId = thread?.optString("authorid", "").orEmpty(),
                fid = thread?.optString("fid", "").orEmpty(),
                totalRows = totalRows,
                raw = clean,
                users = nameByUid
            )
        } catch (e: Exception) {
            dumpJsonError(json, clean, e)
            PostPage(error = "JSON解析失败：${e.message}", raw = clean)
        }
    }

    private fun parseComments(commObj: JSONObject?, attachPrefix: String, nameByUid: Map<String, String>): List<PostComment> {
        if (commObj == null) return emptyList()
        val out = mutableListOf<PostComment>()
        val keys = commObj.keys().asSequence().toList()
        for (k in keys) {
            val c = commObj.optJSONObject(k) ?: continue
            val uid = c.optString("authorid", "")
            val author = c.optString("author", "").ifBlank { nameByUid[uid] ?: uid }
            out.add(
                PostComment(
                    author = author,
                    content = rewriteImages(c.optString("content", ""), attachPrefix),
                    postDate = parsePostDate(c.optString("postdate", ""))
                )
            )
        }
        return out
    }

    // ---------- Search (forum.php) ----------

    suspend fun search(key: String, fid: String? = null, stid: String? = null): ThreadPage {
        val cacheKey = "search:${key.trim()}:${fid.orEmpty()}:${stid.orEmpty()}"
        return runCatching {
            val raw = api.search(key = key, fid = fid.takeUnless { stid != null }, stid = stid)
            parseThreadsOffMain(raw).also {
                if (it.error == null && it.raw.isNotBlank()) cacheResponse(cacheKey, it.raw)
            }
        }.getOrElse { error ->
            val cached = db.responseCacheDao().get(cacheKey)
            cached?.let { parseThreadsOffMain(it.payload).copy(fromCache = true, cachedAt = it.updatedAt) }
                ?: ThreadPage(error = error.message ?: "error", raw = "")
        }
    }

    // ---------- Reply (post.php) ----------

    /**
     * 发表回复。返回 Result 成功消息或错误信息。
     * 响应形如 {"data":{"0":0,"1":"回复成功"}} 或 {"error":{"0":"1:xxx"}}。
     */
    suspend fun reply(fid: String, tid: String, content: String): Result<String> = runCatching {
        val raw = api.reply(fid = fid, tid = tid, content = content)
        try {
            val root = JSONObject(preprocess(raw))
            val err = root.optJSONObject("error")
            if (err != null) {
                val msg = err.optString("0", raw.take(200))
                throw IllegalStateException(msg.removePrefix("1:"))
            }
            val data = root.optJSONObject("data")
            if (data != null) {
                // data 里通常有 {0:0, 1:"回复成功", ...}
                val ok = data.optString("1", data.optString("0", ""))
                if (ok.contains("成功")) return@runCatching ok
            }
            throw IllegalStateException("服务器没有返回明确的成功结果，请稍后刷新帖子确认")
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            // 非 JSON 只接受包含明确“回复…成功”的响应；WAF/验证页不得误报成功。
            val success = Regex("""回复[^\s<]{0,16}成功""").find(raw)?.value
            success ?: throw IllegalStateException("无法确认回复是否成功，请刷新帖子后再决定是否重试")
        }
    }

    suspend fun publish(
        action: String,
        fid: String,
        tid: String? = null,
        pid: String? = null,
        stid: String? = null,
        subject: String = "",
        content: String
    ): Result<String> = runCatching {
        require(action in setOf("new", "reply", "quote")) { "不支持的发布动作" }
        val raw = api.publish(
            action = action,
            fid = fid,
            stid = stid,
            tid = tid,
            pid = pid,
            subject = subject,
            content = content
        )
        withContext(Dispatchers.Default) {
            parsePublishResult(raw, if (action == "new") "主题发布成功" else "回复成功")
        }
    }

    private fun parsePublishResult(raw: String, fallbackSuccess: String): String {
        val clean = preprocess(raw)
        val root = runCatching { JSONObject(clean) }.getOrNull()
        val error = root?.optJSONObject("error")
        if (error != null) {
            val message = error.keys().asSequence().joinToString(" ") { error.optString(it) }
            throw IllegalStateException(message.substringAfter(':', message).ifBlank { "发布失败" })
        }
        val data = root?.optJSONObject("data")
        val values = data?.keys()?.asSequence()?.map { data.optString(it) }?.toList().orEmpty()
        val explicit = values.firstOrNull {
            it.contains("成功") || it.contains("发布") || it.contains("回复")
        }
        if (explicit != null || Regex("(?:发帖|发布|回复)[^<\\s]{0,16}成功").containsMatchIn(raw)) {
            return explicit ?: fallbackSuccess
        }
        // NGA 成功响应在不同版块可能只返回跳转地址或 tid/pid 数字。
        if (data != null && data.length() > 0 && values.none { it.contains("失败") || it.contains("错误") }) {
            return fallbackSuccess
        }
        throw IllegalStateException("服务器没有返回明确的成功结果，请刷新确认后再决定是否重试")
    }

    // ---------- Shared helpers ----------

    private fun cacheResponse(key: String, payload: String) {
        val generation = cacheGeneration.get()
        cacheScope.launch {
            runCatching {
                cacheMutex.withLock {
                    if (generation != cacheGeneration.get()) return@withLock
                    db.responseCacheDao().put(ResponseCacheEntity(key, payload))
                    val now = System.currentTimeMillis()
                    val previous = lastCachePruneAt.get()
                    if (now - previous >= CACHE_PRUNE_INTERVAL_MILLIS &&
                        lastCachePruneAt.compareAndSet(previous, now)
                    ) {
                        db.responseCacheDao().prune(now - CACHE_RETENTION_MILLIS)
                    }
                }
            }.onFailure { error ->
                Log.w("NgaCache", "后台缓存写入失败: $key", error)
            }
        }
    }

    suspend fun clearResponseCache() = withContext(Dispatchers.IO) {
        cacheGeneration.incrementAndGet()
        cacheMutex.withLock { db.responseCacheDao().clear() }
    }

    // ---------- Drafts ----------

    suspend fun draft(key: String): DraftEntity? = db.draftDao().get(key)

    fun drafts(): Flow<List<DraftEntity>> = db.draftDao().all()

    suspend fun saveDraft(item: DraftEntity) = db.draftDao().put(item)

    suspend fun deleteDraft(key: String) = db.draftDao().delete(key)

    // ---------- Watched threads ----------

    fun watchedThreads(): Flow<List<WatchedThreadEntity>> = db.watchedThreadDao().all()

    suspend fun isWatching(tid: String): Boolean = db.watchedThreadDao().get(tid) != null

    suspend fun watchThread(tid: String, title: String, fid: String, replies: Int) {
        db.watchedThreadDao().put(
            WatchedThreadEntity(
                tid = tid,
                title = title,
                fid = fid,
                lastKnownReplies = replies,
                lastSeenReplies = replies
            )
        )
    }

    suspend fun unwatchThread(tid: String) = db.watchedThreadDao().delete(tid)

    // ---------- Community / user ----------

    suspend fun notifications(since: Long = 0): Result<List<CommunityItem>> = runCatching {
        val raw = api.notifications(since = since)
        withContext(Dispatchers.Default) { parseCommunityItems(raw, "提醒") }
    }

    suspend fun privateMessages(page: Int = 1): Result<List<CommunityItem>> = runCatching {
        val raw = api.messages(page = page)
        withContext(Dispatchers.Default) { parseCommunityItems(raw, "私信") }
    }

    suspend fun userProfile(uid: String): Result<UserProfile> = runCatching {
        val raw = api.userInfo(uid = uid)
        withContext(Dispatchers.Default) {
            val root = JSONObject(preprocess(raw))
            val data = root.optJSONObject("data") ?: throw IllegalStateException(errorOf(root, root.toString()))
            val item = data.optJSONObject("0") ?: data
            UserProfile(
                uid = item.optString("uid", uid),
                username = item.optString("username", uid),
                avatar = normalizeNgaAvatarUrl(item.optString("avatar", "")),
                group = item.optString("group", ""),
                title = item.optString("title", ""),
                signature = item.optString("sign", ""),
                posts = item.optInt("posts", 0),
                reputation = item.optInt("rvrc", 0),
                followedBy = item.optInt("follow_by_num", 0),
                lastVisit = item.optLong("lastvisit", 0L)
            )
        }
    }

    suspend fun userTopics(uid: String, page: Int = 1): Result<List<ThreadItem>> = runCatching {
        val raw = api.userTopics(uid = uid, page = page)
        withContext(Dispatchers.Default) {
            val root = JSONObject(preprocess(raw))
            val data = root.optJSONObject("data") ?: throw IllegalStateException(errorOf(root, root.toString()))
            val items = mutableListOf<ThreadItem>()
            collectObjects(data).forEach { item ->
                val tid = item.optString("tid", "")
                val subject = stripTags(item.optString("subject", item.optString("title", "")))
                if (tid.isNotBlank() && subject.isNotBlank()) {
                    items += ThreadItem(
                        tid = tid,
                        subject = subject,
                        author = item.optString("author", item.optString("username", "")),
                        authorId = item.optString("authorid", uid),
                        postDate = item.optLong("postdate", item.optLong("time", 0L)),
                        lastPostDate = item.optLong("lastpost", item.optLong("postdate", 0L)),
                        replies = item.optInt("replies", 0),
                        forumName = item.optString("fname", item.optString("forum", ""))
                    )
                }
            }
            items.distinctBy { it.tid }
        }
    }

    private suspend fun parseCategoryOffMain(raw: String): List<BoardGroup> =
        withContext(Dispatchers.Default) { parseCategory(raw) }

    private suspend fun parseThreadsOffMain(raw: String): ThreadPage =
        withContext(Dispatchers.Default) { parseThreads(raw) }

    private suspend fun parsePostsOffMain(raw: String): PostPage =
        withContext(Dispatchers.Default) { parsePosts(raw) }

    private fun parseCommunityItems(raw: String, fallbackTitle: String): List<CommunityItem> {
        val root = JSONObject(preprocess(raw))
        val error = root.optJSONObject("error")
        if (error != null) throw IllegalStateException(errorOf(root, raw))
        val data = root.optJSONObject("data") ?: return emptyList()
        return collectObjects(data).mapIndexedNotNull { index, item ->
            val tid = item.optString("tid", item.optString("topic_id", ""))
            val pid = item.optString("pid", item.optString("post_id", ""))
            val title = stripTags(
                item.optString("subject", item.optString("title", item.optString("type", "")))
            ).ifBlank { fallbackTitle }
            val summary = stripTags(
                item.optString("content", item.optString("message", item.optString("msg", "")))
            )
            val actor = item.optString(
                "username",
                item.optString("author", item.optString("from", item.optString("from_username", "")))
            )
            if (title == fallbackTitle && summary.isBlank() && actor.isBlank() && tid.isBlank()) {
                return@mapIndexedNotNull null
            }
            CommunityItem(
                id = item.optString("id", item.optString("mid", "$fallbackTitle-$index-${tid}-${pid}")),
                title = title,
                summary = summary,
                actor = actor,
                tid = tid,
                pid = pid,
                createdAt = item.optLong("time", item.optLong("postdate", item.optLong("lastpost", 0L))),
                unread = item.optInt("read", item.optInt("is_read", 0)) == 0
            )
        }.distinctBy { it.id }
    }

    private fun collectObjects(value: Any?): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        fun visit(current: Any?) {
            when (current) {
                is JSONObject -> {
                    val hasIdentity = current.has("tid") || current.has("mid") || current.has("subject") ||
                        current.has("content") || current.has("username") || current.has("author")
                    if (hasIdentity) out += current
                    current.keys().asSequence().forEach { visit(current.opt(it)) }
                }
                is JSONArray -> for (i in 0 until current.length()) visit(current.opt(i))
            }
        }
        visit(value)
        return out
    }

    private fun resolveAttachPrefix(data: JSONObject): String {
        val g = data.optJSONObject("__GLOBAL") ?: return FALLBACK_ATTACH_PREFIX
        val v = g.optString("_ATTACH_BASE_VIEW", "").trim()
        val base = if (v.isNotBlank()) v else FALLBACK_ATTACH_PREFIX
        // 接口返回的 _ATTACH_BASE_VIEW 常是裸主机（如 "img.nga.cn/attachments/"），
        // 缺协议头会被 WebView 当成相对路径拼到 base URL，导致图片被 ORB 拦截。统一补成 https://。
        val withScheme = when {
            base.startsWith("//") -> "https:$base"
            base.startsWith("http") -> base
            else -> "https://$base"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    companion object {
        // NGA 当前可用图床根（2026-08 CDN 迁移后）。当接口未返回 _ATTACH_BASE_VIEW 时使用。
        private const val FALLBACK_ATTACH_PREFIX = "https://img.nga.cn/attachments/"
        private const val CACHE_RETENTION_MILLIS = 14L * 24 * 60 * 60 * 1000
        private const val CACHE_PRUNE_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    }

    private fun rewriteImages(html: String, attachPrefix: String): String {
        // 1) 把已失效的旧图床主机重写为当前可用主机。
        var s = NgaResourceUrls.normalizeLegacyImageHosts(html)
        // 2.5) 补齐缺失协议头的图床地址：NGA 接口常在 src/href 里直接返回裸主机
        //      img.nga.cn/attachments/...（无 https://），WebView 会把它当成相对路径
        //      拼到 base URL，结果变成 https://bbs.nga.cn/img.nga.cn/... 被 ORB 拦截。
        //      这里统一补成 https:// 绝对地址。（此格式不以 ./ 或 attachments/ 开头，
        //      因此下面基于前缀的替换规则匹配不到，必须单独处理。）
        s = ensureImageScheme(s)
        // 2) 把相对附件路径补全为绝对地址
        val prefix = NgaResourceUrls.normalizeLegacyImageHosts(attachPrefix).let { p ->
            if (p.isNotBlank() && !p.endsWith("/")) "$p/" else p
        }
        if (prefix.isNotBlank()) {
            // 相对附件：覆盖 src="./mon_YYYYMM/..."、src="mon_YYYYMM/..."、src="./attachments/" 等。
            // 注意：必须匹配完整的 src 值（到引号结束），否则只吃到第一个 '/' 会把后续路径甩到引号外。
            s = s.replace(Regex("""src=["']((?:\./)?(?:attachments|mon_\d{6})/[^"']+)["']""")) { m ->
                val rel = m.groupValues[1].removePrefix("./")
                "src=\"$prefix$rel\""
            }
            // [img]./attachments/...[/img] 或 [img]./mon_...[/img] 也要补全前缀
            s = s.replace(Regex("""\[img[^\]]*\]\./?(attachments/|mon_\d{6}/)([^\]]*)\[/img]""", RegexOption.IGNORE_CASE)) { m ->
                "[img]$prefix${m.groupValues[1]}${m.groupValues[2]}[/img]"
            }
            // NGA 仍使用历史 [flash] 标签承载 MP4/音频附件。先在数据层把相对附件
            // 地址补全，正文解析器才能把它渲染为可点击的媒体卡片。
            s = absolutizeNgaMediaTags(s, prefix)
        }
        // 3) NGA 的 GIF 缩略图路径是 xxx.gif.thumb_ss.jpg，直接请求会 404；还原成 xxx.gif
        s = s.replace(Regex("""(https?://\S+\.gif)\.(thumb_s|medium|thumb|thumb_ss)\.jpg""", RegexOption.IGNORE_CASE), "$1")
        // 4) 静态图缩略图链接（href 里的）还原原图尺寸
        s = s.replace(
            Regex("""(href=["']https?://\S+\.(?:png|jpg|jpeg|webp))\.(thumb_s|medium|thumb|thumb_ss)\.jpg["']""", RegexOption.IGNORE_CASE),
            "$1"
        )
        return s
    }

    /**
     * 把缺少协议头的图床地址补全为 https:// 绝对地址。
     * 例如 src="img.nga.cn/attachments/..." -> src="https://img.nga.cn/attachments/..."。
     * 仅作用于 img*.nga.cn / img*.nga.178.com / img*.ngacn.cc 这些图床主机，不影响主站域名。
     * 已带 http(s):// 的地址不会被重复处理。
     */
    private fun ensureImageScheme(text: String): String {
        // 属性形式：src="img..."  /  href="img..."
        var s = text.replace(
            Regex("""(src|href)=["']img(\d*)\.(nga\.178\.com|ngacn\.cc|nga\.cn)(/[^"']*)["']""", RegexOption.IGNORE_CASE)
        ) { m ->
            val attr = m.groupValues[1]
            val num = m.groupValues[2]
            val rest = m.groupValues[4]
            """$attr="https://img$num.nga.cn$rest""""
        }
        // [img] 形式：内部为裸主机时同样补全
        s = s.replace(
            Regex("""(\[img[^\]]*\])(https?://)?img(\d*)\.(nga\.178\.com|ngacn\.cc|nga\.cn)(/[^\]]*)(\[/img])""", RegexOption.IGNORE_CASE)
        ) { m ->
            val open = m.groupValues[1]
            val scheme = m.groupValues[2]
            val num = m.groupValues[3]
            val rest = m.groupValues[5]
            val close = m.groupValues[6]
            "$open${scheme ?: "https://"}img$num.nga.cn$rest$close"
        }
        return s
    }

    private fun parsePostDate(s: String): Long {
        if (s.isBlank()) return 0L
        // formats seen: "yyyy-MM-dd HH:mm", "yyyy-M-d H:m"
        val fmts = listOf("yyyy-MM-dd HH:mm", "yyyy-M-d H:mm", "yyyy-MM-dd HH:mm:ss")
        for (f in fmts) {
            try {
                val sdf = java.text.SimpleDateFormat(f, java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
                return sdf.parse(s)?.time ?: 0L
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    private fun preprocess(js: String): String {
        var s = js
        val prefix = "window.script_muti_get_var_store="
        if (s.startsWith(prefix)) s = s.substring(prefix.length)
        s = s.replace("/*\$js\$*/", "")
        // NGA 返回的是 JS 对象字面量，里面偶尔会出现无引号的特殊数字：
        //   +123（JSON 不支持前导 +）、01234（JSON 不支持前导 0）。
        // 必须用 [{,] / [,}] 做字段边界，防止在普通字符串内容里误替换导致 JSON 串被截断
        // （之前因此出现过 "Unterminated string at character 6109"）。
        s = s.replace(Regex("""([{,]\s*)"content"\s*:\s*\+(\d+)(\s*[,}])""")) { m ->
            "${m.groupValues[1]}\"content\":\"+${m.groupValues[2]}\"${m.groupValues[3]}"
        }
        s = s.replace(Regex("""([{,]\s*)"subject"\s*:\s*\+(\d+)(\s*[,}])""")) { m ->
            "${m.groupValues[1]}\"subject\":\"+${m.groupValues[2]}\"${m.groupValues[3]}"
        }
        s = s.replace(Regex("""([{,]\s*)"content"\s*:\s*(0\d+)(\s*[,}])""")) { m ->
            "${m.groupValues[1]}\"content\":\"${m.groupValues[2]}\"${m.groupValues[3]}"
        }
        s = s.replace(Regex("""([{,]\s*)"subject"\s*:\s*(0\d+)(\s*[,}])""")) { m ->
            "${m.groupValues[1]}\"subject\":\"${m.groupValues[2]}\"${m.groupValues[3]}"
        }
        s = s.replace(Regex("""([{,]\s*)"author"\s*:\s*(0\d+)(\s*[,}])""")) { m ->
            "${m.groupValues[1]}\"author\":\"${m.groupValues[2]}\"${m.groupValues[3]}"
        }
        val idx = s.indexOf("/*error fill content")
        if (idx >= 0) s = s.substring(0, idx)
        return s
    }

    /**
     * 解析失败时，把诊断信息（异常、截断字符位置附近的文本、原始响应尾部）直接打到 logcat，
     * 同时落盘到内部存储 /data/data/com.qingyi5427.ngaqing/files/ 作为后备（adb shell run-as 可读）。
     * 关键改进：从异常消息动态提取失败字符位置，而不是写死 offset。
     */
    private fun dumpJsonError(raw: String, processed: String, e: Exception) {
        try {
            val msg = e.message ?: e.toString()
            val target = Regex("""(?:character|index|position)\s*[:=]?\s*(\d+)""", setOf(RegexOption.IGNORE_CASE))
                .find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: (processed.length - 200)
            val snippet = if (processed.length <= 600) {
                processed
            } else {
                val cs = maxOf(0, target - 250)
                val ce = minOf(processed.length, target + 250)
                processed.substring(cs, ce)
            }
            val report = buildString {
                appendLine("=== NgaJson PARSE ERROR ===")
                appendLine("exception: $msg")
                appendLine("raw.length=${raw.length} processed.length=${processed.length} target=$target")
                appendLine("--- processed around char $target ---")
                appendLine(snippet)
                appendLine("--- raw tail last 1000 ---")
                appendLine(raw.takeLast(1000))
            }
            Log.e("NgaJson", "PARSE_ERROR_START >>>")
            report.lineSequence().forEach { Log.e("NgaJson", it) }
            Log.e("NgaJson", "<<< PARSE_ERROR_END")
            try {
                java.io.File(context.filesDir, "nga_debug_json_error.txt").writeText(report)
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }

    private fun errorOf(root: JSONObject, raw: String): String {
        val err = root.optJSONObject("error")
        if (err != null) {
            // error is object keyed by index -> "15:访客不能直接访问"
            val sb = StringBuilder()
            val keys = err.keys().asSequence().toList()
            for (k in keys) {
                sb.append(err.optString(k, "")).append(" ")
            }
            if (sb.isNotBlank()) return sb.toString().trim()
        }
        return "解析失败，请查看原始数据"
    }

    private fun stripTags(s: String): String =
        android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()

    // ---------- Favorites (Room) ----------

    fun favorites(): Flow<List<FavoriteEntity>> = db.favoriteDao().all()

    suspend fun isFavorite(tid: String): Boolean = db.favoriteDao().get(tid) != null

    suspend fun addFavorite(tid: String, title: String, fid: String = "", author: String = "") {
        db.favoriteDao().insert(
            FavoriteEntity(
                tid = tid,
                title = title,
                fid = fid,
                author = author
            )
        )
        runCatching { api.addServerFavorite(tid = tid) }
    }

    suspend fun removeFavorite(tid: String) {
        db.favoriteDao().deleteByTid(tid)
        runCatching { api.removeServerFavorite(tid = tid, tidArray = tid) }
    }

    /**
     * 把 NGA 账号中的主题收藏合并进本地收藏。服务端接口不提供本地分组信息，
     * 因此只补充缺失项目，不覆盖用户已设置的分组和本地时间。
     */
    suspend fun syncServerFavorites(): Result<Int> = runCatching {
        val page = parseThreadsOffMain(api.threadList(favor = 1, page = 1))
        page.error?.let { error(it) }
        var added = 0
        page.threads.forEach { thread ->
            if (thread.tid.isNotBlank() && db.favoriteDao().get(thread.tid) == null) {
                db.favoriteDao().insert(
                    FavoriteEntity(
                        tid = thread.tid,
                        title = thread.subject.ifBlank { "主题 ${thread.tid}" },
                        fid = "",
                        author = thread.author,
                        folder = "来自 NGA"
                    )
                )
                added++
            }
        }
        added
    }

    suspend fun moveFavorite(tid: String, folder: String) {
        db.favoriteDao().updateFolder(tid, folder.trim().ifBlank { "默认" })
    }

    // ---------- Reading history ----------

    fun history(): Flow<List<HistoryEntity>> = db.historyDao().all()

    suspend fun history(tid: String): HistoryEntity? = db.historyDao().get(tid)

    suspend fun recordHistory(tid: String, title: String, author: String, fid: String) {
        val old = db.historyDao().get(tid)
        db.historyDao().insert(
            HistoryEntity(
                tid = tid,
                title = title.ifBlank { old?.title.orEmpty() }.ifBlank { "（无标题）" },
                author = author.ifBlank { old?.author.orEmpty() },
                fid = fid.ifBlank { old?.fid.orEmpty() },
                lastVisited = System.currentTimeMillis(),
                lastFloor = old?.lastFloor ?: 0
            )
        )
    }

    suspend fun updateHistoryFloor(tid: String, floor: Int) {
        db.historyDao().updateFloor(tid, floor.coerceAtLeast(0))
    }

    suspend fun removeHistory(tid: String) = db.historyDao().deleteByTid(tid)

    suspend fun clearHistory() = db.historyDao().clear()

    // ---------- Favorite boards ----------

    private fun boardKey(fid: String, stid: String?): String =
        stid?.takeIf { it.isNotBlank() }?.let { "stid:$it" } ?: "fid:$fid"

    fun favoriteBoards(): Flow<List<Board>> = db.favoriteBoardDao().all().map { items ->
        items.map { item ->
            Board(
                fid = item.fid,
                stid = item.stid.takeIf { it.isNotBlank() },
                name = item.name,
                info = item.info,
                categoryName = "收藏版块",
                groupName = "收藏版块",
                iconUrl = NgaResourceUrls.boardIcon(item.fid, item.stid.takeIf { it.isNotBlank() })
            )
        }
    }

    suspend fun isFavoriteBoard(fid: String, stid: String?): Boolean =
        db.favoriteBoardDao().get(boardKey(fid, stid)) != null

    suspend fun addFavoriteBoard(fid: String, stid: String?, name: String, info: String = "") {
        db.favoriteBoardDao().insert(
            FavoriteBoardEntity(
                key = boardKey(fid, stid),
                fid = fid,
                stid = stid.orEmpty(),
                name = name,
                info = info
            )
        )
    }

    suspend fun removeFavoriteBoard(fid: String, stid: String?) {
        db.favoriteBoardDao().deleteByKey(boardKey(fid, stid))
    }

    suspend fun reorderFavoriteBoards(boards: List<Board>) {
        val newestOrder = System.currentTimeMillis()
        db.withTransaction {
            boards.forEachIndexed { index, board ->
                db.favoriteBoardDao().updateOrder(
                    key = boardKey(board.fid, board.stid),
                    orderValue = newestOrder - index
                )
            }
        }
    }

    // ---------- Login state ----------

    fun isLoggedIn() = prefs.isLoggedIn
    fun userName() = prefs.uname
}
