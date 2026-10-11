package com.qingyi5427.ngaqing.data.repository

import org.json.JSONObject

/** A folder owned by the NGA account. Its numeric id is unrelated to local labels. */
data class ServerFavoriteFolder(
    val id: String,
    val name: String,
    val count: Int,
    val isDefault: Boolean
)

data class ServerFavoriteSelection(
    val folders: List<ServerFavoriteFolder>,
    val selectedIds: Set<String>,
    val hasLocalCopy: Boolean
)

internal data class ServerFavoriteFolderPage(
    val folders: List<ServerFavoriteFolder>, val hasMore: Boolean
)

internal fun parseServerFavoriteFolders(response: String): List<ServerFavoriteFolder> =
    parseServerFavoriteFolderPage(response).folders

internal fun parseServerFavoriteFolderPage(response: String): ServerFavoriteFolderPage {
    val root = runCatching { JSONObject(response) }.getOrElse {
        throw IllegalStateException("无法读取 NGA 收藏文件夹", it)
    }
    if (root.has("error") && !root.isNull("error")) {
        val error = root.opt("error")
        if (error !is JSONObject || error.length() > 0) {
            throw IllegalStateException("NGA 未提供收藏文件夹，请检查登录状态后重试")
        }
    }
    val entries = root.optJSONObject("data")?.optJSONObject("0")
        ?: throw IllegalStateException("NGA 收藏文件夹响应格式异常")
    val folders = buildList {
        val keys = entries.keys()
        while (keys.hasNext()) {
            val entry = entries.optJSONObject(keys.next())
                ?: throw IllegalStateException("NGA 收藏文件夹响应格式异常")
            val id = entry.optString("id").trim()
            check(id.toIntOrNull()?.let { it > 0 } == true) { "NGA 收藏文件夹 ID 无效" }
            val name = entry.optString("name").trim()
            check(name.isNotEmpty()) { "NGA 收藏文件夹名称为空" }
            val count = entry.optString("length", "0").toIntOrNull()
                ?: throw IllegalStateException("NGA 收藏文件夹数量无效")
            val default = entry.opt("default")?.toString()?.lowercase() !in
                setOf(null, "0", "false", "null", "")
            add(ServerFavoriteFolder(id, name, count.coerceAtLeast(0), default))
        }
    }
    check(folders.distinctBy { it.id }.size == folders.size) { "NGA 收藏文件夹 ID 重复" }
    val moreValue = root.optJSONObject("data")?.opt("1")
    val hasMore = moreValue?.toString()?.lowercase() !in
        setOf(null, "0", "false", "null", "")
    return ServerFavoriteFolderPage(
        folders.sortedWith(compareByDescending<ServerFavoriteFolder> { it.isDefault }.thenBy { it.id.toInt() }),
        hasMore
    )
}
