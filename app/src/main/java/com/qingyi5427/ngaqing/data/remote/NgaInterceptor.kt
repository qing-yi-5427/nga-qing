package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NgaInterceptor @Inject constructor(
    private val prefs: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Coil 头像/正文图与 API 复用此客户端。稳定态必须走内存快照，不能让每张图片
        // 都 runBlocking 读取 DataStore；仅应用刚启动、首个快照尚未到达时同步加载一次。
        val currentPreferences = prefs.requestPreferencesOrNull()
            ?: runBlocking { prefs.requestPreferences() }
        val preferences = request.tag(RequestPreferences::class.java) ?: currentPreferences
        if (preferences.revision != currentPreferences.revision ||
            preferences.uid != currentPreferences.uid ||
            preferences.cid != currentPreferences.cid
        ) {
            throw IOException("NGA account changed before request was sent")
        }
        val host = preferences.ngaDomain
        val isSafeRead = request.method == "GET" || request.method == "HEAD"
        val session = NgaRequestSession(
            preferences.uid, preferences.cid, preferences.revision,
            !isSafeRead, NgaAuthPolicy.isTrusted(request.url)
        )
        if (!NgaAuthPolicy.isLoginUrl(request.url)) {
            return chain.proceed(authenticatedRequest(request, request.url.host, session))
        }

        val candidates = if (isSafeRead) {
            NgaDomains.failoverHosts(host)
        } else {
            listOf(NgaDomains.normalizeHost(host))
        }
        var lastFailure: IOException? = null
        candidates.forEachIndexed { index, candidate ->
            val attempt = authenticatedRequest(request, candidate, session)
            try {
                val response = chain.proceed(attempt)
                val retryServerFailure = response.code in 500..599 && index < candidates.lastIndex
                if (!retryServerFailure) return response
                response.close()
            } catch (error: IOException) {
                lastFailure = error
                if (index == candidates.lastIndex) throw error
            }
        }
        throw lastFailure ?: IOException("所有 NGA 入口域名均不可用")
    }

    private fun authenticatedRequest(
        source: Request,
        host: String,
        session: NgaRequestSession
    ): Request {
        val selectedUrl = if (NgaAuthPolicy.isLoginUrl(source.url)) {
            source.url.newBuilder().scheme("https").host(host).port(443).build()
        } else {
            source.url
        }
        val builder = source.newBuilder()
            .url(selectedUrl)
            .tag(NgaRequestSession::class.java, session)
            // A fixed forum origin keeps ordinary hotlinked attachments loadable.
            .header("Referer", "${NgaDomains.origin(host)}/")
        NgaAuthPolicy.removeCredentials(builder)
        if (NgaAuthPolicy.isTrusted(selectedUrl)) {
            builder.header("User-Agent", UA)
                .header("X-User-Agent", CLIENT_ID)
        }
        return builder.build()
    }

    companion object {
        const val CLIENT_ID = "nga-qing"
        const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
