package com.qingyi5427.ngaqing.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Board(
    val fid: String,
    val name: String,
    val info: String = "",
    val groupName: String = "",
    val categoryName: String = "",
    val stid: String? = null,
    /** 版块图标 URL（app_api other.forum_icon_list，图片 icon 时非空） */
    val iconUrl: String? = null,
    /** 版块主题色（ARGB int，无图片图标时由 NGA 指定，可作兜底背景） */
    val iconColor: Int? = null
)

@Immutable
data class BoardGroup(
    val categoryName: String,
    val groupName: String,
    /** 与分组同名、可直接进入的母板块；纯分类分组没有母板块。 */
    val parent: Board? = null,
    /** 母板块下的子板块，或纯分类分组下的全部板块。 */
    val children: List<Board> = emptyList()
)

@Immutable
data class ThreadItem(
    val tid: String,
    val subject: String,
    val author: String,
    val authorId: String,
    val postDate: Long,
    val lastPostDate: Long = postDate,
    val replies: Int,
    val lastPoster: String = "",
    val type: String = "",
    val recommend: Int = 0,
    val forumName: String = "",
    val avatar: String = ""
)

@Immutable
data class PostComment(
    val author: String,
    val content: String,
    val postDate: Long
)

@Immutable
data class Post(
    val tid: String,
    val pid: String,
    val author: String,
    val authorId: String,
    val subject: String,
    val postDate: Long,
    val lou: Int,
    val content: String,
    val signature: String = "",
    val comments: List<PostComment> = emptyList(),
    val avatar: String = "",
    val fid: String = ""
)

@Immutable
data class ThreadPage(
    val threads: List<ThreadItem> = emptyList(),
    /** 当前板块由 thread.php 返回的真实下级板块 / 主题合集。 */
    val subBoards: List<Board> = emptyList(),
    val totalRows: Int = 0,
    val error: String? = null,
    val raw: String = ""
)

@Immutable
data class PostPage(
    val posts: List<Post> = emptyList(),
    val subject: String = "",
    val author: String = "",
    val authorId: String = "",
    val fid: String = "",
    val totalRows: Int = 0,
    val error: String? = null,
    val raw: String = "",
    val users: Map<String, String> = emptyMap()  // uid -> username，用于引用/评论显示作者名
)
