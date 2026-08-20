package com.qingyi5427.ngaqing.ui

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val BOARDS = "boards"
    const val THREADS = "threads/{fid}/{name}?stid={stid}"
    const val POSTS = "posts/{tid}"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val SEARCH = "search?fid={fid}&stid={stid}"

    fun threadRoute(fid: String, name: String, stid: String? = null): String =
        "threads/$fid/${Uri.encode(name)}" + if (stid.isNullOrBlank()) "" else "?stid=${Uri.encode(stid)}"

    fun postRoute(tid: String): String =
        "posts/$tid"

    fun searchRoute(fid: String?, stid: String? = null): String = buildString {
        append("search")
        val params = buildList {
            if (!fid.isNullOrBlank()) add("fid=${Uri.encode(fid)}")
            if (!stid.isNullOrBlank()) add("stid=${Uri.encode(stid)}")
        }
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
}
