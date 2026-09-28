package com.qingyi5427.ngaqing.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.design.NgaBackdropScope
import com.qingyi5427.ngaqing.ui.design.ngaBackdropSource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BoardScreen(
    nav: NavHostController,
    viewModel: BoardViewModel = hiltViewModel(),
    showBottomBar: Boolean = true,
    selectedBoardKey: String? = null,
    onOpenBoard: (Board) -> Unit = { nav.navigate(Routes.threadRoute(it.fid, it.name, it.stid)) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteBoards by viewModel.favoriteBoards.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val hasDock = showBottomBar && !LocalRootNavigationRail.current
    val listState = remember(viewModel) {
        LazyListState(viewModel.initialScrollIndex, viewModel.initialScrollOffset)
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
    NgaBackdropScope {
    Box(
        Modifier.fillMaxSize()
            .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        AppTopBar("版块") {
            IconButton(onClick = { nav.navigate(Routes.searchRoute(null)) }) {
                Icon(Icons.Filled.Search, contentDescription = "全站搜索")
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).ngaBackdropSource()
        ) {
            when (val current = state) {
                is BoardUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is BoardUiState.Error -> ErrorState(current.msg, viewModel::load)
                is BoardUiState.Success -> if (current.groups.isEmpty() && favoriteBoards.isEmpty()) {
                    BoardMessage("暂无版块", "下拉刷新以重新获取版块列表。")
                } else BoardList(
                    current.groups,
                    favoriteBoards,
                    selectedBoardKey,
                    listState,
                    viewModel::reorderFavoriteBoards,
                    onOpenBoard,
                    onSearch = { nav.navigate(Routes.searchRoute(null)) },
                    showBottomBar = hasDock
                )
            }
        }
    }
    if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) { AppBottomBar(nav, Routes.BOARDS) }
    }
    }
}

@Composable
private fun BoardList(
    groups: List<BoardGroup>,
    favoriteBoards: List<Board>,
    selectedBoardKey: String?,
    listState: LazyListState,
    onFavoriteOrderChanged: (List<Board>) -> Unit,
    onOpenBoard: (Board) -> Unit,
    onSearch: () -> Unit,
    showBottomBar: Boolean
) {
    val categories = groups.groupBy { it.categoryName }
    val dockClearance = 96.dp + with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    var collapsed by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val orderedFavoriteBoards = remember { mutableStateOf(favoriteBoards) }
    var draggingFavoriteKey by remember { mutableStateOf<String?>(null) }
    val dragScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(favoriteBoards) {
        if (draggingFavoriteKey == null) orderedFavoriteBoards.value = favoriteBoards
    }
    fun moveFavoriteForAccessibility(key: String, step: Int): Boolean {
        val boards = orderedFavoriteBoards.value
        val from = boards.indexOfFirst { favoriteBoardKey(it) == key }
        val to = from + step
        if (from !in boards.indices || to !in boards.indices) return false
        val moved = moveFavoriteBoard(boards, key, favoriteBoardKey(boards[to]))
        orderedFavoriteBoards.value = moved
        onFavoriteOrderChanged(moved)
        return true
    }
    val sections = buildList {
        var nextIndex = 2 // The introduction and search entry are stable list items.
        if (orderedFavoriteBoards.value.isNotEmpty()) {
            add(BoardDirectorySection("favorite-boards", "收藏版块", orderedFavoriteBoards.value.size, nextIndex))
            nextIndex += 1 + if ("favorite-boards" in collapsed) 0 else orderedFavoriteBoards.value.size
            nextIndex++ // Gap after favorites.
        }
        categories.forEach { (categoryName, categoryGroups) ->
            val categoryKey = "category-$categoryName"
            val boardCount = categoryGroups.sumOf { it.children.size + if (it.parent == null) 0 else 1 }
            add(BoardDirectorySection(categoryKey, categoryName, boardCount, nextIndex))
            nextIndex++
            if (categoryKey !in collapsed) categoryGroups.forEachIndexed { groupIndex, group ->
                val groupKey = "group-${group.categoryName}-${group.groupName}"
                if (group.parent != null || group.groupName != categoryName) nextIndex++
                if (groupKey !in collapsed) nextIndex += group.children.size
                if (groupIndex != categoryGroups.lastIndex) nextIndex++
            }
        }
    }
    val defaultSectionKey = sections.firstOrNull { section ->
            if (section.key == "favorite-boards") orderedFavoriteBoards.value.any {
                favoriteBoardKey(it) == selectedBoardKey
            } else categories[section.label].orEmpty().any { group ->
                group.parent?.let { favoriteBoardKey(it) == selectedBoardKey } == true ||
                    group.children.any { favoriteBoardKey(it) == selectedBoardKey }
            }
        }?.key
    val activeSectionKey by remember(listState, sections, defaultSectionKey) {
        derivedStateOf {
            val visibleIndexes = listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
            sections.lastOrNull { it.headerIndex in visibleIndexes }?.key
                ?: sections.lastOrNull { it.headerIndex <= listState.firstVisibleItemIndex }?.key
                ?: defaultSectionKey
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val wideDirectory = maxWidth >= 720.dp
    Row(Modifier.fillMaxSize()) {
    if (wideDirectory) {
        BoardDirectoryRail(sections, activeSectionKey) { section ->
            if (section.key in collapsed) collapsed = collapsed - section.key
            dragScope.launch { listState.animateScrollToItem(section.headerIndex) }
        }
        Spacer(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
    }
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(bottom = if (showBottomBar) dockClearance else 24.dp)
    ) {
        item(key = "directory-intro", contentType = "intro") {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 10.dp)) {
                Text("版块目录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${groups.sumOf { it.children.size + if (it.parent == null) 0 else 1 }} 个版块 · 找到感兴趣的讨论",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item(key = "directory-search", contentType = "search") {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(onClick = onSearch)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "搜索全站主题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (orderedFavoriteBoards.value.isNotEmpty()) {
            val favoritesKey = "favorite-boards"
            val favoritesExpanded = favoritesKey !in collapsed
            item(key = favoritesKey, contentType = "section-header") {
                CategoryHeader("收藏版块", favoritesExpanded, count = orderedFavoriteBoards.value.size,
                    detail = "长按拖动以调整顺序") {
                    collapsed = collapsed.toggle(favoritesKey)
                }
            }
            if (favoritesExpanded) orderedFavoriteBoards.value.forEach { board ->
                val boardKey = favoriteBoardKey(board)
                val itemKey = "favorite-$boardKey"
                item(key = itemKey, contentType = "board-row") {
                    var dragOffset by remember(boardKey) { mutableFloatStateOf(0f) }
                    var lastSwapTarget by remember(boardKey) { mutableStateOf<String?>(null) }
                    var orderChanged by remember(boardKey) { mutableStateOf(false) }
                    val dragging = draggingFavoriteKey == boardKey
                    val orderIndex = orderedFavoriteBoards.value.indexOfFirst { favoriteBoardKey(it) == boardKey }
                    BoardRow(
                        board = board,
                        isChild = false,
                        selected = boardKey == selectedBoardKey,
                        showDragHandle = true,
                        accessibilityActions = buildList {
                            if (orderIndex > 0) add(CustomAccessibilityAction("上移${board.name}") {
                                moveFavoriteForAccessibility(boardKey, -1)
                            })
                            if (orderIndex in 0 until orderedFavoriteBoards.value.lastIndex) {
                                add(CustomAccessibilityAction("下移${board.name}") {
                                    moveFavoriteForAccessibility(boardKey, 1)
                                })
                            }
                        },
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = dragOffset
                                shadowElevation = if (dragging) 4.dp.toPx() else 0f
                                shape = RoundedCornerShape(12.dp)
                            }
                            .background(
                                if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                else Color.Transparent
                            )
                            .pointerInput(boardKey) {
                                fun finishDrag() {
                                    val finalOrder = orderedFavoriteBoards.value
                                    dragOffset = 0f
                                    lastSwapTarget = null
                                    draggingFavoriteKey = null
                                    if (orderChanged) onFavoriteOrderChanged(finalOrder)
                                    orderChanged = false
                                }
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        lastSwapTarget = null
                                        orderChanged = false
                                        draggingFavoriteKey = boardKey
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val layoutInfo = listState.layoutInfo
                                        val draggedInfo = layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == itemKey }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val draggedCenter = draggedInfo.offset +
                                            draggedInfo.size / 2f + dragOffset
                                        val targetInfo = layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            val key = info.key as? String
                                            (key?.startsWith("favorite-fid:") == true ||
                                                key?.startsWith("favorite-stid:") == true) &&
                                                draggedCenter >= info.offset &&
                                                draggedCenter <= info.offset + info.size
                                        }
                                        if (targetInfo == null || targetInfo.key == itemKey) {
                                            lastSwapTarget = null
                                        } else if (targetInfo.key != lastSwapTarget) {
                                            lastSwapTarget = targetInfo.key as String
                                            val targetBoardKey = (targetInfo.key as String)
                                                .removePrefix("favorite-")
                                            val moved = moveFavoriteBoard(
                                                orderedFavoriteBoards.value,
                                                boardKey,
                                                targetBoardKey
                                            )
                                            if (moved !== orderedFavoriteBoards.value) {
                                                dragOffset += draggedInfo.offset - targetInfo.offset
                                                orderedFavoriteBoards.value = moved
                                                orderChanged = true
                                            }
                                        }
                                        val edge = 72.dp.toPx()
                                        val scrollBy = when {
                                            draggedCenter < layoutInfo.viewportStartOffset + edge -> -20.dp.toPx()
                                            draggedCenter > layoutInfo.viewportEndOffset - edge -> 20.dp.toPx()
                                            else -> 0f
                                        }
                                        if (scrollBy != 0f) {
                                            dragScope.launch { listState.scrollBy(scrollBy) }
                                        }
                                    },
                                    onDragEnd = ::finishDrag,
                                    onDragCancel = ::finishDrag
                                )
                            }
                    ) {
                        onOpenBoard(board)
                    }
                }
            }
            item(key = "favorite-gap", contentType = "gap") { Box(Modifier.fillMaxWidth().height(20.dp)) }
        }
        categories.entries.forEachIndexed { categoryIndex, (categoryName, categoryGroups) ->
            val categoryKey = "category-$categoryName"
            val categoryExpanded = categoryKey !in collapsed
            item(key = categoryKey, contentType = "section-header") {
                CategoryHeader(categoryName, categoryExpanded,
                    count = categoryGroups.sumOf { it.children.size + if (it.parent == null) 0 else 1 }) {
                    collapsed = collapsed.toggle(categoryKey)
                }
            }

            if (categoryExpanded) categoryGroups.forEachIndexed { groupIndex, group ->
                val groupKey = "group-${group.categoryName}-${group.groupName}"
                val groupExpanded = groupKey !in collapsed
                val parent = group.parent
                if (parent != null) {
                    item(key = "parent-${parent.fid}-${parent.stid.orEmpty()}", contentType = "board-row") {
                        BoardRow(
                            board = parent,
                            isChild = false,
                            selected = favoriteBoardKey(parent) == selectedBoardKey,
                            expanded = groupExpanded,
                            onToggle = { collapsed = collapsed.toggle(groupKey) },
                            onClick = { onOpenBoard(parent) }
                        )
                    }
                } else if (group.groupName != categoryName) {
                    item(key = groupKey, contentType = "group-header") {
                        GroupHeader(group.groupName, groupExpanded) {
                            collapsed = collapsed.toggle(groupKey)
                        }
                    }
                }

                if (groupExpanded) group.children.forEachIndexed { boardIndex, board ->
                    item(key = "board-${board.fid}-${board.stid.orEmpty()}", contentType = "board-row") {
                        BoardRow(
                            board,
                            isChild = parent != null,
                            selected = favoriteBoardKey(board) == selectedBoardKey
                        ) {
                            onOpenBoard(board)
                        }
                        if (boardIndex != group.children.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = if (parent != null) 98.dp else 76.dp)
                            )
                        }
                    }
                }

                if (groupIndex != categoryGroups.lastIndex) {
                    item(key = "group-gap-$categoryIndex-$groupIndex", contentType = "gap") {
                        Box(Modifier.fillMaxWidth().height(8.dp))
                    }
                }
            }
        }
    }
    }
    }
}

private data class BoardDirectorySection(
    val key: String,
    val label: String,
    val count: Int,
    val headerIndex: Int
)

@Composable
private fun BoardDirectoryRail(
    sections: List<BoardDirectorySection>,
    activeKey: String?,
    onSelect: (BoardDirectorySection) -> Unit
) {
    Column(
        Modifier.width(220.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text("目录概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("按分类浏览所有版块", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
        sections.forEach { section ->
            val active = section.key == activeKey
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelect(section) }
                    .semantics {
                        contentDescription = "跳转到${section.label}"
                        selected = active
                    }
                    .heightIn(min = 48.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(section.label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text("${section.count}", style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    name: String,
    expanded: Boolean,
    count: Int,
    detail: String? = null,
    onToggle: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .padding(start = 24.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("$count", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 10.dp))
                }
                if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CollapseButton(expanded, onToggle, name)
        }
    }
}

@Composable
private fun GroupHeader(name: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 24.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        CollapseButton(expanded, onToggle, name)
    }
}

@Composable
private fun BoardRow(
    board: Board,
    isChild: Boolean,
    selected: Boolean = false,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    showDragHandle: Boolean = false,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth()
            .background(if (selected) colorScheme.primaryContainer else colorScheme.surface)
            .drawBehind {
                if (selected) drawRect(colorScheme.primary, size = Size(3.dp.toPx(), size.height))
            }
            .semantics {
                this.selected = selected
                if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions
            }
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(start = if (isChild) 38.dp else 24.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoardIcon(board, if (isChild) 34.dp else 42.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(
                board.name,
                style = if (isChild) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else null,
                color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (board.info.isNotBlank()) {
                Text(
                    board.info,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (showDragHandle) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "长按拖动${board.name}排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (expanded != null && onToggle != null) {
            CollapseButton(expanded, onToggle, board.name)
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CollapseButton(expanded: Boolean, onToggle: () -> Unit, name: String) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown
            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "收起$name" else "展开$name",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Set<String>.toggle(key: String): Set<String> =
    if (key in this) this - key else this + key

@Composable
private fun BoardIcon(board: Board, size: Dp) {
    val shape = RoundedCornerShape(if (size > 36.dp) 12.dp else 10.dp)
    val background = MaterialTheme.colorScheme.surfaceVariant
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier.size(size).clip(shape).background(background),
        contentAlignment = Alignment.Center
    ) {
        if (board.iconUrl != null) {
            var loaded by remember(board.iconUrl) { mutableStateOf(false) }
            if (!loaded) BoardIconFallback(foreground)
            AsyncImage(
                model = board.iconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(3.dp)
                    .graphicsLayer { alpha = if (loaded) 1f else 0f },
                contentScale = ContentScale.Fit,
                onLoading = { loaded = false },
                onSuccess = { loaded = true },
                onError = { loaded = false }
            )
        } else {
            BoardIconFallback(foreground)
        }
    }
}

@Composable
private fun BoardIconFallback(color: Color) {
    Icon(
        imageVector = Icons.Outlined.Forum,
        contentDescription = null,
        tint = color,
        modifier = Modifier.fillMaxSize().padding(8.dp)
    )
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    BoardMessage("版块加载失败", message, retry)
}

@Composable
private fun BoardMessage(title: String, detail: String, retry: (() -> Unit)? = null) {
    NgaStatePanel(title, detail, actionLabel = if (retry == null) null else "重试", onAction = retry)
}
