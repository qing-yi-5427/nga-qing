package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A network interceptor runs once for every actual exchange, including redirect targets.
 * Application interceptors only see the original call and cannot protect redirect hops.
 */
@Singleton
class NgaCredentialInterceptor internal constructor(
    private val currentPreferences: () -> RequestPreferences
) : Interceptor {
    @Inject constructor(prefs: UserPreferences) : this({
        prefs.requestPreferencesOrNull() ?: runBlocking { prefs.requestPreferences() }
    })

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val session = request.tag(NgaRequestSession::class.java)
        val trusted = NgaAuthPolicy.isTrusted(request.url)

        // Never let a redirect or transparent connection retry replay a write.
        session?.markNetworkAttempt()
        if (!trusted) session?.leaveTrustedOrigins()

        val authenticatedSession = session?.takeIf {
            trusted && it.mayAuthenticate() && it.uid.isNotBlank() && it.cid.isNotBlank()
        }
        if (authenticatedSession != null) {
            val current = currentPreferences()
            if (current.revision != authenticatedSession.revision ||
                current.uid != authenticatedSession.uid || current.cid != authenticatedSession.cid
            ) {
                throw IOException("NGA account changed while request was in flight")
            }
        }

        val builder = request.newBuilder()
        NgaAuthPolicy.removeCredentials(builder)
        if (authenticatedSession != null) {
            builder.header(
                "Cookie",
                "ngaPassportUid=${authenticatedSession.uid}; ngaPassportCid=${authenticatedSession.cid}"
            )
        }
        return chain.proceed(builder.build())
    }
}
