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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.EditorialRootTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.rememberScrollDockState
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
    val dockState = rememberScrollDockState()
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
            .then(if (hasDock) Modifier.nestedScroll(dockState.nestedScrollConnection) else Modifier)
            .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        if (showBottomBar) {
            EditorialRootTopBar(nav, Routes.BOARDS, "版块目录",
                Modifier.padding(horizontal = if (LocalV8LayoutPolicy.current.availableWidthDp >= 600f)
                    34.dp else 20.dp))
            BoardPageIntro { nav.navigate(Routes.searchRoute(null)) }
        } else BoardCompactTopBar { nav.navigate(Routes.searchRoute(null)) }
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
                    showBottomBar = hasDock,
                    rootScreen = showBottomBar
                )
            }
        }
    }
    if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) {
        AppBottomBar(nav, Routes.BOARDS, visible = dockState.visible)
    }
    }
    }
}

@Composable
private fun BoardPageIntro(onSearch: () -> Unit) {
    val comfortable = LocalV8LayoutPolicy.current.mediumComfortTypography
    val margin = if (LocalV8LayoutPolicy.current.availableWidthDp >= 600f) 34.dp else 20.dp
    Column(Modifier.fillMaxWidth().testTag("board-directory-root-header")
        .padding(horizontal = margin)
        .padding(top = if (comfortable) 14.dp else 11.dp, bottom = 16.dp)) {
        Text("COMMUNITY  ·  NGA QING", fontSize = 11.sp, lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 7.dp, bottom = 5.dp), verticalAlignment = Alignment.Bottom) {
            Text("版块", fontSize = if (comfortable) 40.sp else 35.sp,
                lineHeight = if (comfortable) 46.sp else 42.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text(".", fontSize = if (comfortable) 40.sp else 35.sp,
                lineHeight = if (comfortable) 46.sp else 42.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
        Text("按兴趣找到想看的讨论", fontSize = if (comfortable) 13.sp else 12.sp,
            lineHeight = if (comfortable) 18.sp else 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 15.dp)
            .heightIn(min = if (comfortable) 53.dp else 48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onSearch).testTag("board-directory-search")
            .padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(19.dp))
            Text("搜索帖子、版块与用户", fontSize = if (comfortable) 15.sp else 13.sp,
                lineHeight = if (comfortable) 20.sp else 18.sp,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun BoardCompactTopBar(onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp)
        .testTag("board-directory-compact-header"),
        verticalAlignment = Alignment.CenterVertically) {
        Text("版块目录", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = "全站搜索")
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
    showBottomBar: Boolean,
    rootScreen: Boolean
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
        var nextIndex = 0
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
    val localPolicy = decideV8LayoutPolicy(maxWidth.value, maxHeight.value,
        LocalDensity.current.fontScale)
    CompositionLocalProvider(LocalV8LayoutPolicy provides localPolicy) {
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
        contentPadding = PaddingValues(
            start = if (rootScreen) if (wideDirectory) 34.dp else 20.dp else 10.dp,
            end = if (rootScreen) if (wideDirectory) 34.dp else 20.dp else 10.dp,
            bottom = if (showBottomBar) dockClearance else 24.dp)
    ) {
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
                        bottomRounded = orderIndex == orderedFavoriteBoards.value.lastIndex,
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
                            bottomRounded = !groupExpanded || group.children.isEmpty(),
                            selected = favoriteBoardKey(parent) == selectedBoardKey,
                            expanded = groupExpanded,
                            onToggle = { collapsed = collapsed.toggle(groupKey) },
                            onClick = { onOpenBoard(parent) }
                        )
                    }
                } else if (group.groupName != categoryName) {
                    item(key = groupKey, contentType = "group-header") {
                        GroupHeader(group.groupName, groupExpanded,
                            bottomRounded = !groupExpanded || group.children.isEmpty()) {
                            collapsed = collapsed.toggle(groupKey)
                        }
                    }
                }

                if (groupExpanded) group.children.forEachIndexed { boardIndex, board ->
                    item(key = "board-${board.fid}-${board.stid.orEmpty()}", contentType = "board-row") {
                        BoardRow(
                            board,
                            isChild = parent != null,
                            bottomRounded = boardIndex == group.children.lastIndex,
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
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text("目录概览", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
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
    val comfortable = LocalV8LayoutPolicy.current.mediumComfortTypography
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)
        .clip(if (expanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            else RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .padding(start = 24.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = if (comfortable) 20.sp else 18.sp,
                        lineHeight = if (comfortable) 28.sp else 25.sp,
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun GroupHeader(name: String, expanded: Boolean, bottomRounded: Boolean,
    onToggle: () -> Unit) {
    val comfortable = LocalV8LayoutPolicy.current.mediumComfortTypography
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(if (bottomRounded) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                else RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(min = 48.dp)
            .padding(start = 24.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = if (comfortable) 15.sp else 14.sp,
            lineHeight = if (comfortable) 22.sp else 20.sp,
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
    bottomRounded: Boolean = false,
    selected: Boolean = false,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    showDragHandle: Boolean = false,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val comfortable = LocalV8LayoutPolicy.current.mediumComfortTypography
    Row(
        modifier.fillMaxWidth()
            .clip(if (bottomRounded) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                else RoundedCornerShape(0.dp))
            .background(if (selected) colorScheme.primaryContainer else colorScheme.surface)
            .drawBehind {
                if (selected) drawRect(colorScheme.primary, size = Size(3.dp.toPx(), size.height))
                if (!bottomRounded) drawLine(colorScheme.outlineVariant,
                    start = Offset(16.dp.toPx(), size.height - 0.5.dp.toPx()),
                    end = Offset(size.width - 16.dp.toPx(), size.height - 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx())
            }
            .semantics {
                this.selected = selected
                if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions
            }
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(start = if (LocalV8LayoutPolicy.current.availableWidthDp < 360f)
                if (isChild) 18.dp else 12.dp else if (isChild) 34.dp else 20.dp,
                end = if (LocalV8LayoutPolicy.current.availableWidthDp < 360f) 8.dp else 14.dp,
                top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoardIcon(board, if (isChild) 34.dp else 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 6.dp)) {
            Text(
                board.name,
                fontSize = if (comfortable) 15.sp else if (isChild) 14.sp else 15.sp,
                lineHeight = if (comfortable) 22.sp else 21.sp,
                fontWeight = if (selected) FontWeight.SemiBold else null,
                color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                maxLines = if (LocalDensity.current.fontScale >= 1.3f) 2 else 1,
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
