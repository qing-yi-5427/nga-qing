package com.qingyi5427.ngaqing.ui

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val ADD_ACCOUNT = "login/add-account"
    const val BOARDS = "boards"
    const val THREADS = "threads/{fid}/{name}?stid={stid}"
    const val POSTS = "posts/{tid}"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val SEARCH = "search?fid={fid}&stid={stid}"
    const val NEW_TOPIC = "new-topic/{fid}/{name}?stid={stid}"
    const val COMMUNITY = "community"
    const val USER = "user/{uid}/{name}"
    const val WEB_EDITOR = "web-editor?url={url}&title={title}"

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

    fun newTopicRoute(fid: String, name: String, stid: String? = null): String =
        "new-topic/$fid/${Uri.encode(name)}" +
            if (stid.isNullOrBlank()) "" else "?stid=${Uri.encode(stid)}"

    fun userRoute(uid: String, name: String): String = "user/${Uri.encode(uid)}/${Uri.encode(name)}"

    fun webEditorRoute(url: String, title: String): String =
        "web-editor?url=${Uri.encode(url)}&title=${Uri.encode(title)}"

    fun webNewTopicRoute(domain: String, fid: String, stid: String? = null): String =
        webEditorRoute(
            "https://$domain/post.php?action=new&fid=${Uri.encode(fid)}" +
                if (stid.isNullOrBlank()) "" else "&stid=${Uri.encode(stid)}",
            "高级发帖"
        )

    fun webReplyRoute(domain: String, tid: String, pid: String? = null): String =
        webEditorRoute(
            "https://$domain/post.php?action=${if (pid.isNullOrBlank()) "reply" else "quote"}" +
                "&tid=${Uri.encode(tid)}" + if (pid.isNullOrBlank()) "" else "&pid=${Uri.encode(pid)}",
            "高级回复"
        )
}
