package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** NGA's app favorite endpoint is distinct from the older web favorite endpoint. */
@Singleton
class SignedFavoriteApi @Inject constructor(private val prefs: UserPreferences) {
    private class Attempt(val session: RequestPreferences) {
        val sent = AtomicBoolean(false)
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            val attempt = request.tag(Attempt::class.java)
                ?: throw IOException("NGA signed request missing session")
            if (!attempt.sent.compareAndSet(false, true)) {
                throw IOException("NGA signed request replay blocked")
            }
            if (request.url.host != "ngabbs.com" || !request.url.isHttps || request.url.port != 443) {
                throw IOException("NGA signed request origin changed")
            }
            val current = prefs.requestPreferencesOrNull()
                ?: runBlocking { prefs.requestPreferences() }
            if (current.uid != attempt.session.uid || current.cid != attempt.session.cid ||
                current.revision != attempt.session.revision
            ) {
                throw IOException("NGA account changed before signed request")
            }
            chain.proceed(request)
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listFolders(session: RequestPreferences): JSONObject = request(
        session, "nuke.php", "topic_favor_v2", "list_folder", "12",
        mapOf("uid" to session.uid, "page" to "1")
    )

    suspend fun listTopics(session: RequestPreferences, folder: String, page: Int): JSONObject = request(
        session, "app_api.php", "favor", "all", "12",
        mapOf("folder" to folder, "uid" to session.uid, "page" to page.toString())
    )

    suspend fun add(session: RequestPreferences, folder: String, tid: String): JSONObject = request(
        session, "nuke.php", "topic_favor_v2", "add", "12",
        mapOf("folder" to folder, "tid" to tid, "pid" to "0")
    )

    suspend fun remove(session: RequestPreferences, folder: String, tid: String): JSONObject = request(
        session, "nuke.php", "topic_favor_v2", "del", "14",
        mapOf("folder" to folder, "del" to tid)
    )

    suspend fun deleteFolder(session: RequestPreferences, folder: String): JSONObject = request(
        session, "nuke.php", "topic_favor_v2", "del_folder", "14",
        mapOf("folder" to folder)
    )

    private suspend fun request(
        session: RequestPreferences, path: String, library: String, action: String,
        output: String, fields: Map<String, String>
    ): JSONObject = withContext(Dispatchers.IO) {
        require(session.uid.isNotBlank() && session.cid.isNotBlank()) { "请先登录 NGA" }
        check(prefs.requestPreferences().let {
            it.uid == session.uid && it.cid == session.cid && it.revision == session.revision
        }) { "账号已切换，请重新操作" }
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signatureSource = "$APP_ID${session.uid}${session.cid}$timestamp$SECRET"
        val signature = MessageDigest.getInstance("MD5")
            .digest(signatureSource.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val form = FormBody.Builder()
            .add("__output", output)
            .add("__inchst", "utf-8")
            .add("app_id", APP_ID)
            .add("access_uid", session.uid)
            .add("access_token", session.cid)
            .add("t", timestamp)
            .add("sign", signature)
            .apply { fields.forEach { (key, value) -> add(key, value) } }
            .build()
        val url = "https://ngabbs.com/$path?__lib=$library&__act=$action"
        val request = Request.Builder().url(url).post(form)
            .tag(Attempt::class.java, Attempt(session))
            .header("User-Agent", USER_AGENT)
            .header("X-Requested-With", "gov.pianzong.androidnga")
            .header("X-USER-AGENT", "Nga_Official/7.17.17(HUAWEI NOH-AN00;Android 10)")
            .header("X-NGA-CHANNEL", "official")
            .header("X-NGA-VERSION-NAME", "7.17.17")
            .header("X-NGA-VERSION-CODE", "71717")
            .header("Referer", "https://ngabbs.com/")
            .header("Cookie", "ngaPassportUid=${session.uid}; ngaPassportCid=${session.cid}")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.code in 200..299) { "NGA 收藏接口 HTTP ${response.code}" }
            val raw = response.body?.string() ?: error("NGA 收藏接口响应为空")
            val json = JSONObject(raw)
            check(json.optString("code") == "0") {
                "NGA 收藏接口拒绝操作：${json.optString("msg").take(100)}"
            }
            check(prefs.requestPreferences().let {
                it.uid == session.uid && it.cid == session.cid && it.revision == session.revision
            }) { "账号已切换，请重新操作" }
            json
        }
    }

    private companion object {
        const val APP_ID = "1010"
        const val SECRET = "392e916a6d1d8b7523e2701470000c30bc2165a1"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; NOH-AN00 Build/HUAWEINOH-AN00; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 " +
            "Mobile Safari/537.36 Nga_Official/7.17.17"
    }
}
