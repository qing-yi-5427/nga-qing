package com.qingyi5427.ngaqing.data.repository

import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.ThreadPage

/** Collect a complete server snapshot before any local favorite is changed. */
internal suspend fun collectFavoritePages(fetch: suspend (Int) -> ThreadPage): List<ThreadItem> {
    val all = linkedMapOf<String, ThreadItem>()
    var pageNumber = 1
    var totalRows = 0
    while (pageNumber <= 1000) {
        val page = fetch(pageNumber)
        page.error?.let { throw IllegalStateException("第 $pageNumber 页：$it") }
        totalRows = maxOf(totalRows, page.totalRows)
        val before = all.size
        page.threads.forEach { if (it.tid.isNotBlank()) all.putIfAbsent(it.tid, it) }
        if (page.threads.isEmpty()) {
            check(totalRows == 0 || all.size >= totalRows) {
                "NGA 收藏分页提前结束，已获取 ${all.size}/$totalRows；请稍后手动重试"
            }
            return all.values.toList()
        }
        check(all.size > before) { "NGA 收藏分页重复，已停止同步；请稍后手动重试" }
        if (totalRows > 0 && all.size >= totalRows) return all.values.toList()
        pageNumber++
    }
    throw IllegalStateException("NGA 收藏页数异常，已停止同步")
}
