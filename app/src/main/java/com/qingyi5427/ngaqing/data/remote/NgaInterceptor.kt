package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.NgaDomains
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
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
        val uid = runBlocking { prefs.uid.first() }
        val cid = runBlocking { prefs.cid.first() }
        val host = runBlocking { prefs.ngaDomain.first() }
        if (!NgaDomains.isForumHost(request.url.host)) {
            return chain.proceed(authenticatedRequest(request, request.url.host, uid, cid))
        }

        val isSafeRead = request.method == "GET" || request.method == "HEAD"
        val candidates = if (isSafeRead) {
            NgaDomains.failoverHosts(host)
        } else {
            listOf(NgaDomains.normalizeHost(host))
        }
        var lastFailure: IOException? = null
        candidates.forEachIndexed { index, candidate ->
            val attempt = authenticatedRequest(request, candidate, uid, cid)
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
        source: okhttp3.Request,
        host: String,
        uid: String,
        cid: String
    ): okhttp3.Request {
        val selectedUrl = if (NgaDomains.isForumHost(source.url.host)) {
            source.url.newBuilder().scheme("https").host(host).port(443).build()
        } else {
            source.url
        }
        val builder = source.newBuilder()
            .url(selectedUrl)
            .header("User-Agent", UA)
            .header("X-User-Agent", CLIENT_ID)
            .header("Referer", "${NgaDomains.origin(host)}/")
        if (uid.isNotBlank() && cid.isNotBlank()) {
            builder.header("Cookie", "ngaPassportUid=$uid; ngaPassportCid=$cid")
        }
        return builder.build()
    }

    companion object {
        const val CLIENT_ID = "nga-qing"
        const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
