package com.qingyi5427.ngaqing.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.chrome.EditorialRootTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.rememberScrollDockState
import com.qingyi5427.ngaqing.ui.design.NgaBackdropScope
import com.qingyi5427.ngaqing.ui.design.ngaBackdropSource
import com.qingyi5427.ngaqing.ui.util.formatAuthorName
import com.qingyi5427.ngaqing.ui.util.formatRelative

@Composable
fun FavoritesScreen(nav: NavHostController, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val visitedTids by viewModel.visitedTids.collectAsStateWithLifecycle()
    val policy = LocalV8LayoutPolicy.current
    val compactHeight = policy.compactHeight || LocalConfiguration.current.screenHeightDp < 480
    val context = LocalContext.current
    val hasDock = !LocalRootNavigationRail.current && WindowInsets.ime.getBottom(LocalDensity.current) == 0
    val dockState = rememberScrollDockState()
    val dockClearance = 96.dp + with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    var pendingDelete by remember { mutableStateOf<FavoriteEntity?>(null) }
    var pendingMove by remember { mutableStateOf<FavoriteEntity?>(null) }
    var folderInput by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(searchOpen) {
        if (!searchOpen) dockState.show()
    }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var readFilter by rememberSaveable { mutableStateOf("全部") }
    val gridState = rememberLazyGridState()
    LaunchedEffect(query, selectedFolder) {
        viewModel.setQuery(query)
        viewModel.setFolder(selectedFolder)
    }
    val visibleFavorites = remember(favorites, visitedTids, readFilter) {
        favorites.filter { favorite -> when (readFilter) {
            "未读" -> favorite.tid !in visitedTids
            "已读" -> favorite.tid in visitedTids
            else -> true
        } }
    }
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.consumeSyncMessage()
        }
    }
    if (syncError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSyncError,
            shape = RoundedCornerShape(12.dp),
            title = { Text("收藏同步失败") },
            text = { Text(syncError.orEmpty()) },
            confirmButton = { TextButton(onClick = {
                viewModel.dismissSyncError()
                viewModel.syncFromServer()
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试") } },
            dismissButton = { TextButton(onClick = viewModel::dismissSyncError, modifier = Modifier.heightIn(min = 48.dp)) { Text("稍后处理") } }
        )
    }
    if (actionError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            shape = RoundedCornerShape(12.dp),
            title = { Text("收藏删除未同步") },
            text = { Text(actionError.orEmpty()) },
            confirmButton = { TextButton(onClick = viewModel::retryFavorite, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试同步") } },
            dismissButton = { TextButton(onClick = viewModel::dismissActionError, modifier = Modifier.heightIn(min = 48.dp)) { Text("稍后处理") } }
        )
    }
    NgaBackdropScope {
    Box(
        Modifier.fillMaxSize()
            .then(if (hasDock) Modifier.nestedScroll(dockState.nestedScrollConnection) else Modifier)
            .imePadding()
            .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        val pagePadding = if (policy.availableWidthDp >= 840f) 34.dp
            else if (policy.mediumComfortTypography) 30.dp else 26.dp
        EditorialRootTopBar(nav, Routes.FAVORITES, "阅读资料库",
            Modifier.padding(horizontal = pagePadding))
        FavoritesTools(query, folders, selectedFolder, readFilter, favorites.size,
            comfortable = policy.mediumComfortTypography,
            compact = compactHeight,
            pagePadding = pagePadding,
            visibleCount = visibleFavorites.size,
            syncing = syncing,
            onReadFilter = { readFilter = it },
            onClearQuery = { query = ""; viewModel.setQuery("") },
            onFolderSelect = { selectedFolder = it; viewModel.setFolder(it) },
            onSearch = { searchOpen = true },
            onSync = viewModel::syncFromServer,
            onSort = viewModel::setSort)
        BoxWithConstraints(Modifier.weight(1f)) {
            val twoColumns = policy.favoriteColumns == 2 && maxWidth >= 620.dp
            if (visibleFavorites.isEmpty()) {
                FavoritesEmpty(filtered = query.isNotBlank() || selectedFolder != null || readFilter != "全部")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (twoColumns) 2 else 1),
                    modifier = Modifier.fillMaxSize().ngaBackdropSource(),
                    state = gridState,
                    contentPadding = PaddingValues(start = pagePadding, end = pagePadding,
                        top = 8.dp, bottom = if (hasDock) dockClearance else 24.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(visibleFavorites, key = { it.tid }) { favorite ->
                        val open = { nav.navigate(Routes.postRoute(favorite.tid)) }
                        val delete = { pendingDelete = favorite }
                        val move = { pendingMove = favorite; folderInput = favorite.folder }
                        if (twoColumns) FavoriteCard(favorite, policy.mediumComfortTypography,
                            favorite.tid in visitedTids, open, delete, move)
                        else SwipeToDeleteFavorite(favorite,
                            comfortable = policy.mediumComfortTypography,
                            visited = favorite.tid in visitedTids,
                            onOpen = open, onDelete = delete, onMove = move)
                    }
                }
            }
        }
    }
    if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) {
        AppBottomBar(nav, Routes.FAVORITES, visible = dockState.visible)
    }
    }
    }

    if (searchOpen) {
        AlertDialog(
            onDismissRequest = { searchOpen = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("搜索收藏") },
            text = {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; viewModel.setQuery(it) },
                    placeholder = { Text("输入标题或作者") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { searchOpen = false }) { Text("完成") } },
            dismissButton = { TextButton(onClick = {
                query = ""
                viewModel.setQuery("")
                searchOpen = false
            }) { Text("清除") } }
        )
    }

    pendingDelete?.let { favorite ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(12.dp),
            title = { Text("取消收藏？") },
            text = {
                Text(
                    favorite.title.ifBlank { "（无标题）" },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.remove(favorite.tid)
                        pendingDelete = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("取消收藏", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("保留") }
            }
        )
    }

    pendingMove?.let { favorite ->
        AlertDialog(
            onDismissRequest = { pendingMove = null },
            shape = RoundedCornerShape(12.dp),
            title = { Text("移动收藏") },
            text = {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("分组名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.move(favorite.tid, folderInput)
                    pendingMove = null
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("移动") }
            },
            dismissButton = { TextButton(onClick = { pendingMove = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消") } }
        )
    }
}

@Composable
private fun FavoritesEmpty(filtered: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text(if (filtered) "没有匹配的收藏" else "还没有收藏",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            if (filtered) "调整搜索词或分组后再试。"
            else "阅读帖子时点右上角收藏，稍后可以从这里继续。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FavoritesTools(
    query: String,
    folders: List<String>,
    selectedFolder: String?,
    readFilter: String,
    count: Int,
    comfortable: Boolean,
    compact: Boolean,
    pagePadding: Dp,
    visibleCount: Int,
    syncing: Boolean,
    onReadFilter: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFolderSelect: (String?) -> Unit,
    onSearch: () -> Unit,
    onSync: () -> Unit,
    onSort: (FavoriteSort) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = pagePadding,
        vertical = if (compact) 2.dp else 10.dp)) {
        if (!compact) {
            Text("COLLECTION  ·  KEEP READING", fontSize = 11.sp, lineHeight = 15.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)
        }
        Row(Modifier.fillMaxWidth().padding(top = if (compact) 3.dp else 9.dp),
            verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("收藏", fontSize = if (comfortable) 40.sp else if (compact) 28.sp else 35.sp,
                        lineHeight = if (comfortable) 46.sp else if (compact) 34.sp else 40.sp,
                        fontWeight = FontWeight.Medium)
                    Text(".", fontSize = if (comfortable) 40.sp else if (compact) 28.sp else 35.sp,
                        lineHeight = if (comfortable) 46.sp else if (compact) 34.sp else 40.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (!compact) Text("值得留下的内容，随时接着读。",
                    fontSize = if (comfortable) 14.sp else 13.sp,
                    lineHeight = if (comfortable) 20.sp else 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$count", fontSize = if (comfortable) 32.sp else 28.sp,
                    lineHeight = if (comfortable) 37.sp else 33.sp,
                    fontWeight = FontWeight.Medium)
                Text("篇收藏内容", fontSize = 11.sp, lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = if (compact) 10.dp else 21.dp))
        Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("全部", "未读", "已读").forEach { filter ->
                        val selected = readFilter == filter
                        Surface(onClick = { onReadFilter(filter) },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (selected) 2.dp else 0.dp,
                            modifier = Modifier.heightIn(min = 43.dp)) {
                            Box(Modifier.padding(horizontal = if (comfortable) 17.dp else 12.dp),
                                contentAlignment = Alignment.Center) {
                                Text(filter, fontSize = if (comfortable) 13.sp else 12.sp,
                                    lineHeight = 16.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!compact && comfortable) Text("${visibleCount} 个结果",
                fontSize = 12.sp, lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.outline)
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "搜索、排序与同步收藏",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("搜索收藏") }, leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }, onClick = { menuOpen = false; onSearch() })
                    if (query.isNotBlank() || selectedFolder != null) {
                        DropdownMenuItem(text = { Text("清除搜索与分组") }, onClick = {
                            menuOpen = false; onClearQuery(); onFolderSelect(null)
                        })
                    }
                    DropdownMenuItem(text = { Text(if (syncing) "正在同步" else "同步 NGA 收藏") },
                        leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                        enabled = !syncing,
                        onClick = { menuOpen = false; onSync() })
                    HorizontalDivider()
                    listOf(FavoriteSort.NEWEST to "按最近收藏", FavoriteSort.TITLE to "按标题",
                        FavoriteSort.AUTHOR to "按作者").forEach { (sort, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            menuOpen = false; onSort(sort)
                        })
                    }
                    if (folders.isNotEmpty()) {
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("全部分组") }, onClick = {
                            menuOpen = false; onFolderSelect(null)
                        })
                        folders.forEach { folder ->
                            DropdownMenuItem(text = { Text("分组 · $folder") }, onClick = {
                                menuOpen = false; onFolderSelect(folder)
                            })
                        }
                    }
                }
            }
        }
        if (!compact && (query.isNotBlank() || selectedFolder != null)) {
            TextButton(onClick = {
                onClearQuery(); onFolderSelect(null)
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(listOfNotNull(query.takeIf { it.isNotBlank() }?.let { "搜索：$it" },
                    selectedFolder?.let { "分组：$it" }).joinToString("  ·  ") + "  ×",
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFavorite(
    item: FavoriteEntity,
    comfortable: Boolean,
    visited: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .background(if (state.targetValue == SwipeToDismissBoxValue.EndToStart)
                        MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                    .padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("取消收藏", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    ) {
        FavoriteCard(item, comfortable, visited, onOpen, onDelete, onMove)
    }
}

@Composable
private fun FavoriteCard(
    item: FavoriteEntity,
    comfortable: Boolean,
    visited: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = if (comfortable) 137.dp else 126.dp)
    ) {
        Column(Modifier.padding(start = 17.dp, end = 9.dp, top = 13.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.folder.ifBlank { "默认分组" },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = if (comfortable) 13.sp else 12.sp,
                    lineHeight = if (comfortable) 18.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(if (visited) "已读" else "稍后再读",
                        fontSize = 11.sp, lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(7.dp))
            }
            Text(item.title.ifBlank { "（无标题）" },
                fontSize = if (comfortable) 17.sp else 15.sp,
                lineHeight = if (comfortable) 25.sp else 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = 10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val byline = listOfNotNull(
                    item.author.takeIf { it.isNotBlank() }?.let(::formatAuthorName),
                    item.ts.takeIf { it > 0L }?.let(::formatRelative)
                ).joinToString(" · ")
                Text(byline,
                    fontSize = if (comfortable) 12.sp else 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "管理收藏 ${item.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("移动到其他分组") }, onClick = {
                            menuOpen = false; onMove()
                        })
                        DropdownMenuItem(text = { Text("取消收藏") }, onClick = {
                            menuOpen = false; onDelete()
                        })
                    }
                }
            }
        }
    }
}
