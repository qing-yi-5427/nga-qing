package com.qingyi5427.ngaqing.ui.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.chrome.EditorialRootTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.rememberScrollDockState
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.util.formatAuthorName
import com.qingyi5427.ngaqing.ui.util.formatRelative
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiscoverScreen(nav: NavHostController, viewModel: DiscoverViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteBoards.collectAsStateWithLifecycle()
    val policy = LocalV8LayoutPolicy.current
    val hasDock = !LocalRootNavigationRail.current
    val dockState = rememberScrollDockState()
    // Each layout keeps its own position while the same destination changes width.
    val narrowListState = rememberLazyListState()
    val wideDiscussionListState = rememberLazyListState()
    val wideContextListState = rememberLazyListState()
    val bottom = if (hasDock) 96.dp else 24.dp
    val pagePadding = when {
        policy.availableWidthDp >= 840f -> 34.dp
        policy.mediumComfortTypography -> 30.dp
        else -> 26.dp
    }
    Box(
            Modifier.fillMaxSize()
                .then(if (hasDock) Modifier.nestedScroll(dockState.nestedScrollConnection) else Modifier)
                .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            )) {
                EditorialRootTopBar(nav, Routes.DISCOVER, "你的社区",
                    Modifier.padding(horizontal = pagePadding))
                when (val current = state) {
                    DiscoverState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is DiscoverState.Error -> NgaStatePanel(
                        "发现页加载失败", discoverErrorText(current.message), "重试", viewModel::refresh
                    )
                    is DiscoverState.Ready -> BoxWithConstraints(Modifier.fillMaxSize()) {
                        val wide = policy.homeTwoRegions && maxWidth >= 680.dp
                        val onDiscussion: (ThreadItem) -> Unit = { item ->
                            current.discussionBoard?.let { board ->
                                // Keep the source topic-list entry for the wide reading pane.
                                nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                            }
                            nav.navigate(Routes.postRoute(item.tid))
                        }
                        if (wide) {
                            Column(Modifier.fillMaxSize()) {
                                DiscoverMasthead(policy.mediumComfortTypography,
                                    onSearch = { nav.navigate(Routes.searchRoute(null)) },
                                    modifier = Modifier.padding(horizontal = pagePadding))
                                Row(Modifier.fillMaxSize().padding(horizontal = pagePadding),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    LazyColumn(Modifier.weight(0.58f).fillMaxHeight(),
                                        state = wideDiscussionListState,
                                        contentPadding = PaddingValues(bottom = bottom)) {
                                        item { SectionHeading("正在讨论", "查看全部") {
                                            current.discussionBoard?.let { board ->
                                                nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                                            }
                                        } }
                                        if (current.fromCache) item {
                                            Text("显示离线缓存 · 点此刷新", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.clickable(onClick = viewModel::refresh).padding(bottom = 8.dp))
                                        }
                                        if (current.discussions.isEmpty()) {
                                            item { InlineState(discoverErrorText(current.discussionError ?: "暂无讨论"), viewModel::refresh) }
                                        } else item {
                                            DiscussionGroup(current.discussions.take(16), policy.mediumComfortTypography,
                                                onDiscussion)
                                        }
                                    }
                                    LazyColumn(Modifier.weight(0.42f).fillMaxHeight(),
                                        state = wideContextListState,
                                        contentPadding = PaddingValues(bottom = bottom)) {
                                        item { BoardSection(favorites.ifEmpty { current.boards }.take(4),
                                            favoriteBased = favorites.isNotEmpty(), fourColumns = false,
                                            onOpen = { board -> nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid)) },
                                            onAll = { nav.navigate(Routes.BOARDS) }) }
                                        if (current.boardError != null) item {
                                            InlineState(discoverErrorText(current.boardError), viewModel::refresh)
                                        }
                                        item { ContinueReading(history.firstOrNull(),
                                            onOpen = { tid -> nav.navigate(Routes.postRoute(tid)) },
                                            onHistory = { nav.navigate(Routes.HISTORY) }) }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxSize(),
                                state = narrowListState,
                                contentPadding = PaddingValues(start = pagePadding,
                                    end = pagePadding, bottom = bottom)) {
                                item { DiscoverMasthead(policy.mediumComfortTypography,
                                    onSearch = { nav.navigate(Routes.searchRoute(null)) }) }
                                item { BoardSection(favorites.ifEmpty { current.boards }.take(4),
                                    favoriteBased = favorites.isNotEmpty(),
                                    fourColumns = maxWidth >= 380.dp && maxWidth < 600.dp &&
                                        LocalDensity.current.fontScale < 1.3f,
                                    onOpen = { board -> nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid)) },
                                    onAll = { nav.navigate(Routes.BOARDS) }) }
                                if (current.boardError != null) item {
                                    InlineState(discoverErrorText(current.boardError), viewModel::refresh)
                                }
                                item { ContinueReading(history.firstOrNull(),
                                    onOpen = { tid -> nav.navigate(Routes.postRoute(tid)) },
                                    onHistory = { nav.navigate(Routes.HISTORY) }) }
                                item { SectionHeading("正在讨论", "查看全部") {
                                    current.discussionBoard?.let { board ->
                                        nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                                    }
                                } }
                                if (current.fromCache) item {
                                    Text("显示离线缓存 · 点此刷新", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.clickable(onClick = viewModel::refresh).padding(bottom = 8.dp))
                                }
                                if (current.discussions.isEmpty()) {
                                    item { InlineState(discoverErrorText(current.discussionError ?: "暂无讨论"), viewModel::refresh) }
                                } else item {
                                    DiscussionGroup(current.discussions.take(16), policy.mediumComfortTypography,
                                        onDiscussion)
                                }
                            }
                        }
                    }
                }
            }
            if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) {
                AppBottomBar(nav, Routes.DISCOVER, visible = dockState.visible)
            }
    }
}

@Composable
private fun DiscoverMasthead(comfortable: Boolean, onSearch: () -> Unit,
    modifier: Modifier = Modifier) {
    val date = remember { SimpleDateFormat("M 月 d 日 · EEEE", Locale.CHINA).format(Date()) }
    Column(modifier.fillMaxWidth().padding(top = if (comfortable) 14.dp else 11.dp,
        bottom = if (comfortable) 20.dp else 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, fontSize = if (comfortable) 13.sp else 12.sp,
                lineHeight = if (comfortable) 18.sp else 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (comfortable) {
                Text("  ·  今日正在发生", fontSize = 12.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
            Text("发现", fontSize = if (comfortable) 40.sp else 35.sp,
                lineHeight = if (comfortable) 46.sp else 42.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text(".", fontSize = if (comfortable) 40.sp else 35.sp,
                lineHeight = if (comfortable) 46.sp else 42.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Search, contentDescription = "搜索帖子、版块与用户",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onAction: () -> Unit) {
    val policy = LocalV8LayoutPolicy.current
    val comfortable = policy.mediumComfortTypography
    Row(Modifier.fillMaxWidth().padding(top = if (policy.availableWidthDp >= 840f) 6.dp else 16.dp,
        bottom = if (policy.availableWidthDp >= 840f) 2.dp else if (comfortable) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = if (comfortable) 19.sp else if (policy.availableWidthDp >= 840f) 15.sp else 18.sp,
            lineHeight = if (comfortable) 28.sp else 25.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Row(Modifier.clickable(onClick = onAction).heightIn(min = 48.dp)
            .padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, fontSize = if (comfortable) 13.sp else 12.sp,
                lineHeight = if (comfortable) 18.sp else 16.sp,
                color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun ContinueReading(history: HistoryEntity?, onOpen: (String) -> Unit, onHistory: () -> Unit) {
    val policy = LocalV8LayoutPolicy.current
    val comfortable = policy.mediumComfortTypography
    SectionHeading("继续阅读", "阅读记录", onHistory)
    if (history == null) {
        InlineState("读过的帖子会出现在这里")
        return
    }
    val wide = policy.availableWidthDp >= 840f
    val shape = RoundedCornerShape(if (comfortable) 20.dp else 19.dp)
    Box(Modifier.fillMaxWidth().heightIn(min = if (comfortable) 165.dp else if (wide) 123.dp else 145.dp)
        .shadow(8.dp, shape, ambientColor = Color(0x30385D90), spotColor = Color(0x20385D90))
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFF364C73), Color(0xFF7084B0), Color(0xFFA8B9D5))))
        .clickable { onOpen(history.tid) }) {
        Box(Modifier.matchParentSize()) {
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 47.dp, y = 78.dp)
                .rotate(24f).size(206.dp)
                .border(23.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(52.dp)))
        }
        Column(Modifier.fillMaxWidth().padding(if (comfortable) 20.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Book, contentDescription = null,
                    tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(17.dp))
                Text("上次看到这里", fontSize = if (comfortable) 12.sp else 11.sp,
                    lineHeight = if (comfortable) 16.sp else 14.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(start = 7.dp).weight(1f))
                if (history.lastFloor > 0) Text("第 ${history.lastFloor} 楼",
                    fontSize = 11.sp, lineHeight = 14.sp,
                    color = Color.White.copy(alpha = 0.83f))
            }
            Text(history.title.ifBlank { "（无标题）" },
                fontSize = if (comfortable) 21.sp else if (wide) 18.sp else 20.sp,
                lineHeight = if (comfortable) 30.sp else if (wide) 26.sp else 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                color = Color.White, modifier = Modifier.padding(top = if (wide) 12.dp else 11.dp))
            Row(Modifier.fillMaxWidth().padding(top = if (wide) 10.dp else 8.dp),
                horizontalArrangement = Arrangement.End) {
                Text("继续阅读  →", fontSize = if (comfortable) 12.sp else 11.sp,
                    lineHeight = if (comfortable) 16.sp else 14.sp,
                    color = Color.White.copy(alpha = 0.88f))
            }
        }
    }
}

@Composable
private fun BoardSection(
    boards: List<Board>, favoriteBased: Boolean, fourColumns: Boolean,
    onOpen: (Board) -> Unit, onAll: () -> Unit
) {
    val comfortable = LocalV8LayoutPolicy.current.mediumComfortTypography
    SectionHeading(if (favoriteBased) "常逛版块" else "浏览版块", "全部版块", onAll)
    boards.chunked(if (fourColumns) 4 else 2).forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEachIndexed { columnIndex, board ->
                val symbol = when ((rowIndex * (if (fourColumns) 4 else 2) + columnIndex) % 4) {
                    0 -> Icons.Outlined.Devices
                    1 -> Icons.Outlined.SportsEsports
                    2 -> Icons.Outlined.Lightbulb
                    else -> Icons.Outlined.Code
                }
                Surface(onClick = { onOpen(board) }, modifier = Modifier.weight(1f)
                    .heightIn(min = if (fourColumns) 96.dp else if (comfortable) 88.dp else 63.dp),
                    shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    val icon: @Composable () -> Unit = {
                        if (board.iconUrl != null) AsyncImage(board.iconUrl, contentDescription = null,
                            modifier = Modifier.size(if (fourColumns) 28.dp else 27.dp))
                        else Icon(symbol, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (fourColumns) 28.dp else 27.dp))
                    }
                    if (fourColumns) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            icon()
                            Text(board.name, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            if (board.info.isNotBlank()) Text(board.info, fontSize = 11.sp, lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.outline, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(horizontal = if (comfortable) 18.dp else 11.dp,
                            vertical = if (comfortable) 15.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (comfortable) 13.dp else 9.dp)) {
                            icon()
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(board.name, fontSize = if (comfortable) 15.sp else 13.sp,
                                    lineHeight = if (comfortable) 20.sp else 17.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (board.info.isNotBlank()) Text(board.info,
                                    fontSize = if (comfortable) 12.sp else 11.sp,
                                    lineHeight = if (comfortable) 16.sp else 14.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(if (comfortable) 17.dp else 13.dp))
                        }
                    }
                }
            }
            repeat((if (fourColumns) 4 else 2) - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DiscussionGroup(
    items: List<ThreadItem>, comfortable: Boolean, onOpen: (ThreadItem) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            items.forEachIndexed { index, item ->
                DiscussionRow(item, comfortable) { onOpen(item) }
                if (index < items.lastIndex) HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun DiscussionRow(item: ThreadItem, comfortable: Boolean, onOpen: () -> Unit) {
    val palette = listOf(
        Color(0xFFDEEBFF) to Color(0xFF426CAE),
        Color(0xFFFFE5DD) to Color(0xFFBB715E),
        Color(0xFFE8DFF7) to Color(0xFF8367B2),
        Color(0xFFDCEFE5) to Color(0xFF478B72)
    )
    val color = palette[(item.tid.hashCode() and Int.MAX_VALUE) % palette.size]
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen)
        .padding(horizontal = if (comfortable) 18.dp else 14.dp,
            vertical = if (comfortable) 17.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(if (comfortable) 14.dp else 10.dp),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.size(if (comfortable) 34.dp else 30.dp)
            .background(color.first, CircleShape), contentAlignment = Alignment.Center) {
            Text(formatAuthorName(item.author).firstOrNull()?.toString() ?: "?",
                fontSize = if (comfortable) 14.sp else 12.sp,
                lineHeight = if (comfortable) 18.sp else 16.sp,
                fontWeight = FontWeight.SemiBold, color = color.second)
        }
        Column(Modifier.weight(1f)) {
            Text(item.subject.ifBlank { "（无标题）" },
                fontSize = if (comfortable) 15.5.sp else 14.sp,
                lineHeight = if (comfortable) 22.sp else 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${formatAuthorName(item.author)} · ${formatRelative(item.lastPostDate)}",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                Text(" ${item.replies}", fontSize = 11.sp, lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun InlineState(message: String, retry: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (retry != null) Text("重试", color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = retry).padding(top = 10.dp))
        }
    }
}

/** Keep transport diagnostics out of the public home page while retaining retry. */
internal fun discoverErrorText(message: String): String {
    val networkFailure = listOf("unable to resolve host", "unknownhostexception",
        "failed to connect", "sockettimeoutexception", "timeout")
        .any { message.contains(it, ignoreCase = true) }
    return if (networkFailure) "当前无法连接 NGA，请检查网络后重试" else message
}
