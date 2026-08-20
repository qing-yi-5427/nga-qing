package com.qingyi5427.ngaqing.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
    timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
}

/** Accept both NGA epoch seconds and local epoch milliseconds. */
fun normalizeTimestampMillis(ts: Long): Long = when {
    ts <= 0L -> 0L
    ts < 100_000_000_000L -> ts * 1000L
    else -> ts
}

fun formatDateTime(ts: Long): String {
    val millis = normalizeTimestampMillis(ts)
    return if (millis <= 0) "" else fmt.format(Date(millis))
}

fun formatRelative(ts: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val millis = normalizeTimestampMillis(ts)
    if (millis <= 0) return ""
    val diff = nowMillis - millis
    val min = 60_000L
    val hour = 60 * min
    val day = 24 * hour
    return when {
        // A timestamp substantially in the future is malformed or clock-skewed;
        // showing its date is more honest than incorrectly labelling it "刚刚".
        diff < -min -> fmt.format(Date(millis))
        diff < min -> "刚刚"
        diff < hour -> "${diff / min} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 30 * day -> "${diff / day} 天前"
        else -> fmt.format(Date(millis))
    }
}

/** Strip HTML tags and decode entities for plain text display (e.g. list titles). */
fun stripHtml(s: String): String =
    android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
