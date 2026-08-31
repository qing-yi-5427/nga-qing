package com.qingyi5427.ngaqing.data.remote

import android.webkit.CookieManager
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

internal data class NgaPassportCookies(val uid: String = "", val cid: String = "")

/**
 * CookieManager may return both a host cookie and NGA's parent-domain guest cookie with
 * the same name. A real NGA UID is numeric; never let the guest marker replace it.
 */
internal fun parseNgaPassportCookies(raw: String): NgaPassportCookies {
    val uids = mutableListOf<String>()
    val cids = mutableListOf<String>()
    raw.split(";").forEach { part ->
        val kv = part.trim().split("=", limit = 2)
        if (kv.size != 2 || kv[1].isBlank()) return@forEach
        when (kv[0]) {
            "ngaPassportUid" -> uids += kv[1]
            "ngaPassportCid" -> cids += kv[1]
        }
    }
    return NgaPassportCookies(
        uid = uids.firstOrNull { value -> value.all(Char::isDigit) }.orEmpty(),
        cid = cids.maxByOrNull(String::length).orEmpty()
    )
}

/**
 * Reads NGA passport cookies from the system CookieManager after the user logs in
 * via the WebView, then persists them through [UserPreferences].
 *
 * NGA 的帖子文字走 OkHttp（由 [NgaInterceptor] 自动带 cookie），但帖子正文里的图片是
 * WebView 自己发起的请求，必须依靠系统 [CookieManager] 里的 passport cookie。因此登录
 * 成功后要把 uid/cid 写回 CookieManager，并覆盖到图片子域名；否则图片会 403/裂图。
 */
@Singleton
class LoginHelper @Inject constructor(
    private val prefs: UserPreferences
) {

    /** Hosts that may hold the NGA passport cookies. */
    private val hosts = NgaDomains.options.map { NgaDomains.origin(it.host) }

    /**
     * Hosts that need the passport cookie for WebView image requests.
     * 主站和附件/表情图床都要写，避免某些子域名读不到 cookie 导致图片裂图。
     */
    private val cookieHosts = listOf(
        *hosts.toTypedArray(),
        "https://img.nga.cn",
        "https://img4.nga.cn",
        "https://img9.nga.cn"
    ).distinct()

    /**
     * Reads NGA passport cookies from the system CookieManager and persists them.
     *
     * NGA's QR login only writes the passport cookies after the page is refreshed,
     * so we must re-check on every page load (see [extraUrls] for the live WebView URL).
     *
     * @param extraUrls additional URLs to read cookies from (e.g. the WebView's current
     *   page, which may differ from the hardcoded hosts after redirects).
     * @return true if valid uid + cid cookies were found and saved.
     */
    suspend fun captureAndSave(extraUrls: List<String> = emptyList()): Boolean = withContext(Dispatchers.IO) {
        val cm = CookieManager.getInstance()
        // Ensure the WebView's in-memory cookies are flushed/synced before reading.
        runCatching { cm.flush() }
        var uid = ""
        var cid = ""
        for (url in (hosts + extraUrls).distinct()) {
            val cookie = cm.getCookie(url) ?: continue
            val parsed = parseNgaPassportCookies(cookie)
            if (parsed.uid.isNotBlank()) uid = parsed.uid
            if (parsed.cid.isNotBlank()) cid = parsed.cid
            if (uid.isNotBlank() && cid.isNotBlank()) break
        }
        if (uid.isNotBlank() && cid.isNotBlank()) {
            prefs.saveAuth(uid, cid, "")
            syncCookiesToWebView(uid, cid)
            true
        } else {
            false
        }
    }

    /**
     * 从持久化存储里把 passport cookie 同步回系统 CookieManager。
     * 在 Application / Activity 启动时调用，保证杀进程重进后 WebView 图片仍能带 cookie。
     */
    suspend fun syncAuthCookies(): Unit = withContext(Dispatchers.IO) {
        val uid = prefs.uid.first()
        val cid = prefs.cid.first()
        if (uid.isNotBlank() && cid.isNotBlank()) {
            syncCookiesToWebView(uid, cid)
        }
    }

    /** 同时清除原生请求凭证与 WebView cookie，避免退出后被旧 cookie 自动登录。 */
    suspend fun clearAuth(): Unit = withContext(Dispatchers.IO) {
        prefs.clearAuth()
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        runCatching { cm.flush() }
    }

    /** 为添加另一个账号清理网页会话，但保留 DataStore 中已有的账号列表。 */
    suspend fun prepareAdditionalLogin(): Unit = withContext(Dispatchers.IO) {
        val cm = CookieManager.getInstance()
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                cm.removeAllCookies {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        runCatching { cm.flush() }
    }

    /**
     * CookieManager#setCookie accepts one Set-Cookie value, not an HTTP Cookie header.
     * UID and CID must therefore be written separately and awaited before a WebView loads.
     */
    private suspend fun syncCookiesToWebView(uid: String, cid: String) {
        val cm = CookieManager.getInstance()
        withContext(Dispatchers.Main.immediate) { cm.setAcceptCookie(true) }

        // NGA may create a persistent guest UID on .nga.cn. Host-only auth cookies do not
        // replace it, so both values are sent and read.php can treat the request as logged out.
        // Overwrite the parent-domain pair first so bbs.nga.cn and all image subdomains agree.
        setCookie(cm, "https://bbs.nga.cn", "ngaPassportUid=$uid; Domain=.nga.cn; Path=/; Secure; SameSite=Lax")
        setCookie(cm, "https://bbs.nga.cn", "ngaPassportCid=$cid; Domain=.nga.cn; Path=/; Secure; SameSite=Lax")

        for (host in cookieHosts) {
            setCookie(cm, host, "ngaPassportUid=$uid; Path=/; Secure; SameSite=Lax")
            setCookie(cm, host, "ngaPassportCid=$cid; Path=/; Secure; SameSite=Lax")
        }
        withContext(Dispatchers.IO) { runCatching { cm.flush() } }
    }

    private suspend fun setCookie(cm: CookieManager, url: String, value: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                cm.setCookie(url, value) { success ->
                    if (continuation.isActive) continuation.resume(success)
                }
            }
        }

    fun isLoggedInFlow() = prefs.isLoggedIn
}
