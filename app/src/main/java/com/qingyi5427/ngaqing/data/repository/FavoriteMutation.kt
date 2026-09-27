package com.qingyi5427.ngaqing.data.repository

import org.json.JSONObject

internal fun parseFavoriteMutationResponse(response: String, add: Boolean) {
    val root = runCatching { JSONObject(response) }.getOrElse {
        throw IllegalStateException("无法确认 NGA 收藏操作结果，请手动刷新后重试", it)
    }
    if (root.has("error") && !root.isNull("error")) {
        val error = root.opt("error")
        if (error !is JSONObject || error.length() > 0) {
            throw IllegalStateException(error?.toString()?.take(200) ?: "NGA 收藏操作失败")
        }
    }
    val data = root.optJSONObject("data")
        ?: throw IllegalStateException("无法确认 NGA 收藏操作结果，请手动刷新后重试")
    val message = data.optString("1", "").trim()
    val success = if (add) {
        Regex("^(?:收藏|添加收藏|操作)成功[！!。.]?$")
    } else {
        Regex("^(?:取消收藏|删除收藏|移除收藏|删除|操作)成功[！!。.]?$")
    }
    check(data.optString("0", "") == "0" && success.matches(message)) {
        "无法确认 NGA 收藏操作结果，请手动刷新后重试"
    }
}
