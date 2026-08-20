package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.NgaDomains
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
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
        val selectedUrl = if (NgaDomains.isForumHost(request.url.host)) {
            request.url.newBuilder()
                .scheme("https")
                .host(NgaDomains.normalizeHost(host))
                .port(443)
                .build()
        } else {
            request.url
        }

        val builder = request.newBuilder()
            .url(selectedUrl)
            .header("User-Agent", UA)
            .header("X-User-Agent", CLIENT_ID)
            .header("Referer", "${NgaDomains.origin(host)}/")

        if (uid.isNotBlank() && cid.isNotBlank()) {
            builder.header("Cookie", "ngaPassportUid=$uid; ngaPassportCid=$cid")
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val CLIENT_ID = "nga-qing"
        const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
