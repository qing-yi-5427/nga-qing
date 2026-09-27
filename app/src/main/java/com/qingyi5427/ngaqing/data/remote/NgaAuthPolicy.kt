package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.NgaDomains
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Only these exact HTTPS origins are allowed to receive NGA account credentials. */
internal object NgaAuthPolicy {
    private val imageHosts = setOf("img.nga.cn", "img4.nga.cn", "img9.nga.cn")
    private val credentialHeaders = listOf(
        "Cookie", "Authorization", "Proxy-Authorization",
        "X-Auth-Token", "X-Access-Token", "X-Api-Key"
    )

    fun isTrusted(url: HttpUrl): Boolean =
        url.isHttps && url.port == 443 &&
            (NgaDomains.isForumHost(url.host) || url.host in imageHosts)

    fun isLoginUrl(url: HttpUrl): Boolean =
        url.isHttps && url.port == 443 && NgaDomains.isForumHost(url.host)

    fun removeCredentials(builder: Request.Builder): Request.Builder = builder.apply {
        credentialHeaders.forEach(::removeHeader)
    }
}

/** A request keeps the account that was active when it started, including across redirects. */
internal class NgaRequestSession(
    val uid: String,
    val cid: String,
    val revision: Long,
    val unsafeMethod: Boolean,
    initiallyTrusted: Boolean
) {
    private val sentUnsafeRequest = AtomicBoolean(false)
    private val mayAuthenticate = AtomicBoolean(initiallyTrusted)

    fun mayAuthenticate(): Boolean = mayAuthenticate.get()

    fun leaveTrustedOrigins() {
        mayAuthenticate.set(false)
    }

    @Throws(IOException::class)
    fun markNetworkAttempt() {
        if (unsafeMethod && !sentUnsafeRequest.compareAndSet(false, true)) {
            throw IOException("NGA write request was already sent; automatic replay blocked")
        }
    }
}
