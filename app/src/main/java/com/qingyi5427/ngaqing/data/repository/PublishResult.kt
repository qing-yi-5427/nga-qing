package com.qingyi5427.ngaqing.data.repository

import org.json.JSONObject

/** Only a confirmed NGA success envelope may cause the editor to discard its draft. */
internal fun parseExplicitPublishSuccess(response: String, action: String): String {
    val root = runCatching { JSONObject(response) }.getOrElse {
        throw IllegalStateException("无法确认发布是否成功，请刷新确认后再决定是否重试", it)
    }
    if (root.has("error") && !root.isNull("error")) {
        val error = root.opt("error")
        val message = when (error) {
            is JSONObject -> error.keys().asSequence().joinToString(" ") { error.optString(it) }
            else -> error?.toString().orEmpty()
        }
        if (message.isNotBlank() && message != "{}" && message != "[]") {
            throw IllegalStateException(message.substringAfter(':', message))
        }
    }
    val data = root.optJSONObject("data")
        ?: throw IllegalStateException("无法确认发布是否成功，请刷新确认后再决定是否重试")
    val status = data.optString("0", "")
    val message = data.optString("1", "").trim()
    val expected = if (action == "new") {
        Regex("^(?:主题发布|发帖|发表)成功[！!。.]?$")
    } else {
        Regex("^回复成功[！!。.]?$")
    }
    if (status == "0" && expected.matches(message)) return message
    throw IllegalStateException("服务器没有返回明确的成功结果，请刷新确认后再决定是否重试")
}
