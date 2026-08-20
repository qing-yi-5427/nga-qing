package com.qingyi5427.ngaqing.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BoardScreen(nav: NavHostController, viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteBoards by viewModel.favoriteBoards.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.initialScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.initialScrollOffset
    )
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
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("版块") {
            IconButton(onClick = { nav.navigate(Routes.searchRoute(null)) }) {
                Icon(Icons.Filled.Search, contentDescription = "全站搜索")
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val current = state) {
                is BoardUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is BoardUiState.Error -> ErrorState(current.msg, viewModel::load)
                is BoardUiState.Success -> BoardList(current.groups, favoriteBoards, listState, nav)
            }
        }
        AppBottomBar(nav, Routes.BOARDS)
    }
}

@Composable
private fun BoardList(
    groups: List<BoardGroup>,
    favoriteBoards: List<Board>,
    listState: LazyListState,
    nav: NavHostController
) {
    val categories = groups.groupBy { it.categoryName }
    var collapsed by rememberSaveable { mutableStateOf(emptySet<String>()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (favoriteBoards.isNotEmpty()) {
            val favoritesKey = "favorite-boards"
            val favoritesExpanded = favoritesKey !in collapsed
            item(key = favoritesKey, contentType = "section-header") {
                CategoryHeader("收藏版块", favoritesExpanded, prefix = "") {
                    collapsed = collapsed.toggle(favoritesKey)
                }
            }
            if (favoritesExpanded) favoriteBoards.forEach { board ->
                item(key = "favorite-${board.fid}-${board.stid.orEmpty()}", contentType = "board-row") {
                    BoardRow(board, isChild = false) {
                        nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                    }
                }
            }
            item(key = "favorite-gap", contentType = "gap") { Box(Modifier.fillMaxWidth().height(8.dp)) }
        }
        categories.entries.forEachIndexed { categoryIndex, (categoryName, categoryGroups) ->
            val categoryKey = "category-$categoryName"
            val categoryExpanded = categoryKey !in collapsed
            item(key = categoryKey, contentType = "section-header") {
                CategoryHeader(categoryName, categoryExpanded) {
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
                            expanded = groupExpanded,
                            onToggle = { collapsed = collapsed.toggle(groupKey) },
                            onClick = { nav.navigate(Routes.threadRoute(parent.fid, parent.name, parent.stid)) }
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
                        BoardRow(board, isChild = parent != null) {
                            nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
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

@Composable
private fun CategoryHeader(
    name: String,
    expanded: Boolean,
    prefix: String = "分类 · ",
    onToggle: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prefix$name",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        CollapseButton(expanded, onToggle, name)
    }
}

@Composable
private fun GroupHeader(name: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        CollapseButton(expanded, onToggle, name)
    }
}

@Composable
private fun BoardRow(
    board: Board,
    isChild: Boolean,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (isChild) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(
                start = if (isChild) 30.dp else 20.dp,
                end = 16.dp,
                top = if (isChild) 10.dp else 13.dp,
                bottom = if (isChild) 10.dp else 13.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChild) {
            Box(
                Modifier.width(2.dp).height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            )
            Box(Modifier.width(12.dp))
        }
        BoardIcon(board, if (isChild) 34.dp else 42.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(
                board.name,
                style = if (isChild) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (board.info.isNotBlank()) {
                Text(
                    board.info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (expanded != null && onToggle != null) {
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
    val specifiedColor = board.iconColor?.let(::Color)
    val background = specifiedColor ?: MaterialTheme.colorScheme.secondaryContainer
    val foreground = if (specifiedColor == null) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        Color.White
    }

    Box(
        Modifier.size(size).clip(shape).background(background),
        contentAlignment = Alignment.Center
    ) {
        if (board.iconUrl != null) {
            var loaded by remember(board.iconUrl) { mutableStateOf(false) }
            if (!loaded) BoardIconFallback(foreground)
            AsyncImage(
                model = board.iconUrl,
                contentDescription = "${board.name}图标",
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
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("版块加载失败", style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        androidx.compose.material3.TextButton(onClick = retry) { Text("重试") }
    }
}
