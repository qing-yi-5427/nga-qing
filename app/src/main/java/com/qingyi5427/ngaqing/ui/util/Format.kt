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

private const val ANONY_STEMS = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥"
private const val ANONY_SURNAMES =
    "王李张刘陈杨黄吴赵周徐孙马朱胡林郭何高罗郑梁谢宋唐许邓冯韩曹曾彭萧蔡潘田董袁于余叶蒋杜苏魏程吕丁沈任姚卢傅钟姜崔谭廖范汪陆金石戴贾韦夏邱方侯邹熊孟秦白江阎薛尹段雷黎史龙陶贺顾毛郝龚邵万钱严赖覃洪武莫孔汤向常温康施文牛樊葛邢安齐易乔伍庞颜倪庄聂章鲁岳翟殷詹申欧耿关兰焦俞左柳甘祝包宁尚符舒阮柯纪梅童凌毕单季裴霍涂成苗谷盛曲翁冉骆蓝路游辛靳管柴蒙鲍华喻祁蒲房滕屈饶解牟艾尤阳时穆农司卓古吉缪简车项连芦麦褚娄窦戚岑景党宫费卜冷晏席卫米柏宗瞿桂全佟应臧闵苟邬边卞姬师和仇栾隋商刁沙荣巫寇桑郎甄丛仲虞敖巩明佘池查麻苑迟邝"
private val ANONY_ID_RE = Regex("""^#anony_[0-9a-f]{11,}$""", RegexOption.IGNORE_CASE)

/** 把 NGA 的匿名哈希转换为官方前端同规则的稳定六字代号。 */
fun formatAuthorName(raw: String): String {
    val name = raw.trim()
    if (name.isEmpty()) return "匿名"
    if (!ANONY_ID_RE.matches(name)) return name

    var offset = 6
    val alias = buildString(6) {
        repeat(6) { index ->
            val value = if (index == 0 || index == 3) {
                name.substring(offset + 1, offset + 2).toIntOrNull(16)
            } else {
                name.substring(offset, offset + 2).toIntOrNull(16)
            }
            val alphabet = if (index == 0 || index == 3) ANONY_STEMS else ANONY_SURNAMES
            val char = value?.let(alphabet::getOrNull) ?: return name
            append(char)
            offset += 2
        }
    }
    return "匿名 · $alias"
}
