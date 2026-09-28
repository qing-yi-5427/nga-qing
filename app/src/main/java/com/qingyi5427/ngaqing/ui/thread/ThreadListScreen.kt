package com.qingyi5427.ngaqing.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.board.boardIdentityKey
import com.qingyi5427.ngaqing.ui.board.favoriteBoardKey
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaDimensions
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative
import com.qingyi5427.ngaqing.ui.util.formatAuthorName
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class HeaderScrollStep(val hiddenFraction: Float, val consumedY: Float)

/** Consume exactly the distance traveled by the full-height header, leaving the rest to the list. */
internal fun consumeHeaderScroll(availableY: Float, headerHeightPx: Int, hiddenFraction: Float): HeaderScrollStep {
    if (headerHeightPx <= 0) return HeaderScrollStep(hiddenFraction, 0f)
    val oldHidden = hiddenFraction.coerceIn(0f, 1f) * headerHeightPx
    val newHidden = (oldHidden - availableY).coerceIn(0f, headerHeightPx.toFloat())
    return HeaderScrollStep(newHidden / headerHeightPx, oldHidden - newHidden)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ThreadListScreen(
    nav: NavHostController,
    viewModel: ThreadListViewModel = hiltViewModel(),
    root: RootViewModel = hiltViewModel(),
    selectedTid: String? = null,
    onOpenPost: (String) -> Unit = { nav.navigate(Routes.postRoute(it)) },
    onOpenBoard: (Board) -> Unit = { nav.navigate(Routes.threadRoute(it.fid, it.name, it.stid)) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blockedUsers by root.blacklistUsers.collectAsStateWithLifecycle(emptySet())
    val blockedKeywords by root.blacklistKeywords.collectAsStateWithLifecycle(emptySet())
    val subBoards by viewModel.subBoards.collectAsStateWithLifecycle()
    val recommendedOnly by viewModel.recommendedOnly.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isFavoriteBoard by viewModel.isFavoriteBoard.collectAsStateWithLifecycle()
    val headerHiddenFraction by viewModel.headerHiddenFraction.collectAsStateWithLifecycle()
    val visitedTids by viewModel.visitedTids.collectAsStateWithLifecycle()
    // The same list appears in its own destination and beside a post. The ViewModel's
    // SavedStateHandle is the single scroll source, avoiding two stale saveable snapshots.
    val listState = remember(viewModel) {
        LazyListState(viewModel.initialScrollIndex, viewModel.initialScrollOffset)
    }
    var headerHeightPx by remember(viewModel) { mutableIntStateOf(0) }
    val headerScrollConnection = remember(viewModel) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (headerHeightPx <= 0 || abs(available.y) <= abs(available.x)) return Offset.Zero
                val step = consumeHeaderScroll(available.y, headerHeightPx, viewModel.headerHiddenFraction.value)
                if (step.consumedY == 0f) return Offset.Zero
                viewModel.setHeaderHiddenFraction(step.hiddenFraction)
                // The header consumes exactly its travel, so the first list row moves
                // once at the finger's speed instead of scrolling twice.
                return Offset(0f, step.consumedY)
            }
        }
    }
    val scope = rememberCoroutineScope()
    var sortMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.distinctUntilChanged().collect { last ->
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo.size
            if (total > 0 && visible < total && last >= total - 5) {
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
        Layout(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
                .navigationBarsPadding()
                .nestedScroll(headerScrollConnection)
                .clipToBounds(),
            content = {
                Column(
                    Modifier.onSizeChanged { headerHeightPx = it.height }
                        // The header itself remains a drag surface when a short window leaves
                        // little or no list viewport. The parent consumes the same travel.
                        .scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                        .testTag("thread-header")
                ) {
                    AppTopBar(
                        title = "NGA 清漪",
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        onTitleClick = {
                            viewModel.setHeaderHiddenFraction(0f)
                            if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
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
                            IconButton(onClick = {
                                viewModel.setHeaderHiddenFraction(0f)
                                viewModel.refresh()
                            }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "刷新主题")
                            }
                        }
                    )
                    ForumLevelNavigation(
                        boardName = viewModel.name,
                        subBoards = subBoards,
                        selectedBoardKey = boardIdentityKey(viewModel.fid, viewModel.stid),
                        recommendedOnly = recommendedOnly,
                        sort = sort,
                        sortMenuOpen = sortMenuOpen,
                        onSortMenuChange = { sortMenuOpen = it },
                        onSortChange = { newSort ->
                            if (sort != newSort) {
                                viewModel.setSort(newSort)
                                viewModel.resetScrollPosition()
                                scope.launch { listState.scrollToItem(0) }
                            }
                        },
                        onNewTopic = {
                            nav.navigate(Routes.newTopicRoute(viewModel.fid, viewModel.name, viewModel.stid))
                        },
                        onRecommendedChange = { enabled ->
                            if (enabled != recommendedOnly) {
                                viewModel.setRecommendedOnly(enabled)
                                viewModel.resetScrollPosition()
                                scope.launch { listState.scrollToItem(0) }
                            }
                        },
                        onOpenBoard = { board ->
                            onOpenBoard(board)
                        }
                    )
                }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.setHeaderHiddenFraction(0f)
                    viewModel.refresh()
                },
                modifier = Modifier.testTag("thread-list-viewport")
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
                            NgaStatePanel(
                                title = "暂无主题",
                                description = "没有符合当前条件的主题。可切换筛选或下拉刷新。"
                            )
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                                state = listState,
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                if (current.fromCache) {
                                    item(key = "offline-banner", contentType = "offline-banner") {
                                        Text(
                                            "当前显示离线缓存 · 下拉刷新可重试",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                                items(filtered, key = { it.tid }, contentType = { "thread-row" }) { thread ->
                                    ThreadRow(
                                        thread,
                                        isVisited = thread.tid in visitedTids,
                                        selected = thread.tid == selectedTid
                                    ) {
                                        viewModel.saveScrollPosition(
                                            listState.firstVisibleItemIndex,
                                            listState.firstVisibleItemScrollOffset
                                        )
                                        onOpenPost(thread.tid)
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 20.dp)
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
        ) { children, constraints ->
            val header = children[0].measure(
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
            )
            val hidden = (header.height * headerHiddenFraction).roundToInt().coerceIn(0, header.height)
            val viewportHeight = (constraints.maxHeight - header.height + hidden).coerceAtLeast(0)
            val list = children[1].measure(Constraints.fixed(constraints.maxWidth, viewportHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                header.place(0, -hidden)
                list.place(0, header.height - hidden)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ForumLevelNavigation(
    boardName: String,
    subBoards: List<Board>,
    selectedBoardKey: String,
    recommendedOnly: Boolean,
    sort: ThreadSort,
    sortMenuOpen: Boolean,
    onSortMenuChange: (Boolean) -> Unit,
    onSortChange: (ThreadSort) -> Unit,
    onNewTopic: () -> Unit,
    onRecommendedChange: (Boolean) -> Unit,
    onOpenBoard: (Board) -> Unit
) {
    var boardSheetOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Text(
            boardName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 5.dp)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ForumTextTab(
                selected = !recommendedOnly,
                onClick = { onRecommendedChange(false) },
                label = "全部主题"
            )
            ForumTextTab(
                selected = recommendedOnly,
                onClick = { onRecommendedChange(true) },
                label = "精华区"
            )
            if (subBoards.isNotEmpty()) {
                TextButton(onClick = { boardSheetOpen = true }, modifier = Modifier.heightIn(min = NgaDimensions.minimumTouch)) {
                    Text("子版块", maxLines = 1)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.weight(1f)) {
                TextButton(onClick = { onSortMenuChange(true) }, modifier = Modifier.heightIn(min = NgaDimensions.minimumTouch)) {
                    Text(if (sort == ThreadSort.LAST_REPLY) "最后回复 ↓" else "发帖时间 ↓")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { onSortMenuChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("按最后回复排序") },
                        leadingIcon = { if (sort == ThreadSort.LAST_REPLY) Icon(Icons.Filled.Check, null) },
                        onClick = { onSortMenuChange(false); onSortChange(ThreadSort.LAST_REPLY) }
                    )
                    DropdownMenuItem(
                        text = { Text("按发帖时间排序") },
                        leadingIcon = { if (sort == ThreadSort.POST_DATE) Icon(Icons.Filled.Check, null) },
                        onClick = { onSortMenuChange(false); onSortChange(ThreadSort.POST_DATE) }
                    )
                }
            }
            TextButton(onClick = onNewTopic, modifier = Modifier.heightIn(min = NgaDimensions.minimumTouch)) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(5.dp))
                Text("发主题")
            }
        }
    }
    if (boardSheetOpen) {
        ModalBottomSheet(onDismissRequest = { boardSheetOpen = false }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text(
                    "选择子版块",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                subBoards.forEach { board ->
                    val selected = favoriteBoardKey(board) == selectedBoardKey
                    Row(
                        Modifier.fillMaxWidth().semantics { this.selected = selected }
                            .clickable { boardSheetOpen = false; onOpenBoard(board) }
                            .heightIn(min = 56.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            board.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ForumTextTab(selected: Boolean, onClick: () -> Unit, label: String) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier.heightIn(min = NgaDimensions.minimumTouch)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = accent,
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                    )
                }
            }
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThreadRow(item: ThreadItem, isVisited: Boolean, selected: Boolean, onClick: () -> Unit) {
    val selectedAccent = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .drawBehind {
                if (selected) drawRect(selectedAccent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
            }
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .heightIn(min = NgaDimensions.minimumTouch)
            .padding(horizontal = 20.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                item.subject.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isVisited) FontWeight.Normal else FontWeight.SemiBold,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isVisited -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.replies >= 30) {
                Spacer(Modifier.size(10.dp))
                Text(
                    if (item.replies >= 100) "爆" else "热门",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${formatAuthorName(item.author)} · ${formatRelative(item.lastPostDate)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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
    NgaStatePanel("主题加载失败", message, "重试", retry)
}
