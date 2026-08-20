package com.ngaclient.app.data.local

import java.util.Locale

data class NgaDomainOption(
    val host: String,
    val label: String,
    val description: String
)

/** NGA's interchangeable forum origins, shared by API requests and WebViews. */
object NgaDomains {
    const val DEFAULT_HOST = "bbs.nga.cn"

    val options = listOf(
        NgaDomainOption(DEFAULT_HOST, "主站（推荐）", "bbs.nga.cn"),
        NgaDomainOption("ngabbs.com", "App 链接域名", "ngabbs.com"),
        NgaDomainOption("nga.178.com", "备用域名", "nga.178.com"),
        NgaDomainOption("bbs.ngacn.cc", "旧域名兼容", "bbs.ngacn.cc，可能跳转到主站")
    )

    fun normalizeHost(raw: String?): String {
        val host = raw.orEmpty().trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .lowercase(Locale.ROOT)
        return options.firstOrNull { it.host == host }?.host ?: DEFAULT_HOST
    }

    /** 只有论坛入口域名可以互换；img.nga.cn 等附件/CDN 域名必须保留原主机。 */
    fun isForumHost(host: String): Boolean = options.any { it.host == host.lowercase(Locale.ROOT) }

    fun origin(host: String?): String = "https://${normalizeHost(host)}"

    fun url(host: String?, path: String = ""): String =
        "${origin(host)}/${path.trimStart('/')}"
}
