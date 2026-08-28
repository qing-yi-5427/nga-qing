package com.qingyi5427.ngaqing.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.theme.LocalGlassPalette
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative
import com.qingyi5427.ngaqing.ui.util.formatAuthorName
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ThreadListScreen(
    nav: NavHostController,
    viewModel: ThreadListViewModel = hiltViewModel(),
    root: RootViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blockedUsers by root.blacklistUsers.collectAsStateWithLifecycle(emptySet())
    val blockedKeywords by root.blacklistKeywords.collectAsStateWithLifecycle(emptySet())
    val subBoards by viewModel.subBoards.collectAsStateWithLifecycle()
    val recommendedOnly by viewModel.recommendedOnly.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isFavoriteBoard by viewModel.isFavoriteBoard.collectAsStateWithLifecycle()
    val visitedTids by viewModel.visitedTids.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.initialScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.initialScrollOffset
    )
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo.size
            if (total > 0 && visible < total && info.visibleItemsInfo.last().index >= total - 5) {
                viewModel.loadMore()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    viewModel.saveScrollPosition(
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset
                    )
                }
            }
    }

    DisposableEffect(listState) {
        onDispose {
            viewModel.saveScrollPosition(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    SwipeBackContainer(
        onBack = { nav.popBackStack() },
        customGestureTopInset = 120.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = viewModel.name,
                onTitleClick = {
                    if (listState.firstVisibleItemIndex > 0) {
                        scope.launch { listState.animateScrollToItem(0) }
                    } else {
                        viewModel.refresh()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavoriteBoard) {
                        Icon(
                            if (isFavoriteBoard) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavoriteBoard) "取消收藏版块" else "收藏版块",
                            tint = if (isFavoriteBoard) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { nav.navigate(Routes.searchRoute(viewModel.fid, viewModel.stid)) }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索本版")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("按最后回复排序") },
                                leadingIcon = {
                                    if (sort == ThreadSort.LAST_REPLY) Icon(Icons.Filled.Check, null)
                                },
                                onClick = {
                                    menuOpen = false
                                    if (sort != ThreadSort.LAST_REPLY) {
                                        viewModel.setSort(ThreadSort.LAST_REPLY)
                                        viewModel.resetScrollPosition()
                                        scope.launch { listState.scrollToItem(0) }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("按发帖时间排序") },
                                leadingIcon = {
                                    if (sort == ThreadSort.POST_DATE) Icon(Icons.Filled.Check, null)
                                },
                                onClick = {
                                    menuOpen = false
                                    if (sort != ThreadSort.POST_DATE) {
                                        viewModel.setSort(ThreadSort.POST_DATE)
                                        viewModel.resetScrollPosition()
                                        scope.launch { listState.scrollToItem(0) }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.refresh()
                                }
                            )
                        }
                    }
                }
            )
            ForumLevelNavigation(
                subBoards = subBoards,
                recommendedOnly = recommendedOnly,
                onRecommendedChange = { enabled ->
                    if (enabled != recommendedOnly) {
                        viewModel.setRecommendedOnly(enabled)
                        viewModel.resetScrollPosition()
                        scope.launch { listState.scrollToItem(0) }
                    }
                },
                onOpenBoard = { board ->
                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                }
            )
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f)
            ) {
                when (val current = state) {
                    is ThreadUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is ThreadUiState.Error -> LoadError(current.msg, viewModel::loadInitial)
                    is ThreadUiState.Success -> {
                        val filtered = current.threads.filterNot { thread ->
                            blockedUsers.contains(thread.author) ||
                                blockedKeywords.any { thread.subject.contains(it, ignoreCase = true) }
                        }
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("没有符合当前条件的主题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filtered, key = { it.tid }, contentType = { "thread-row" }) { thread ->
                                    ThreadRow(thread, isVisited = thread.tid in visitedTids) {
                                        viewModel.saveScrollPosition(
                                            listState.firstVisibleItemIndex,
                                            listState.firstVisibleItemScrollOffset
                                        )
                                        nav.navigate(Routes.postRoute(thread.tid))
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                                when {
                                    current.isLoadingMore -> item {
                                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                    current.loadError != null -> item {
                                        Column(
                                            Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(current.loadError, color = MaterialTheme.colorScheme.error)
                                            TextButton(onClick = viewModel::retryMore) { Text("重试") }
                                        }
                                    }
                                    current.page >= current.totalPages -> item {
                                        Text(
                                            "已经到底了",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth().padding(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumLevelNavigation(
    subBoards: List<Board>,
    recommendedOnly: Boolean,
    onRecommendedChange: (Boolean) -> Unit,
    onOpenBoard: (Board) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = !recommendedOnly,
            onClick = { onRecommendedChange(false) },
            label = { Text("全部") }
        )
        Spacer(Modifier.size(8.dp))
        FilterChip(
            selected = recommendedOnly,
            onClick = { onRecommendedChange(true) },
            label = { Text("精华区") }
        )
        subBoards.forEach { board ->
            Spacer(Modifier.size(8.dp))
            FilterChip(
                selected = false,
                onClick = { onOpenBoard(board) },
                label = { Text(board.name, maxLines = 1) }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ThreadRow(item: ThreadItem, isVisited: Boolean, onClick: () -> Unit) {
    val palette = LocalGlassPalette.current
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                item.subject.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isVisited) FontWeight.Normal else FontWeight.Medium,
                color = if (isVisited) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.replies >= 30) {
                Spacer(Modifier.size(10.dp))
                Text(
                    if (item.replies >= 100) "爆" else "热门",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.hotFg,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp))
                        .background(palette.hotBg)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatAuthorName(item.author),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                " · ${formatRelative(item.lastPostDate)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                item.replies.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun LoadError(message: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("主题加载失败", style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(onClick = retry) { Text("重试") }
    }
}
