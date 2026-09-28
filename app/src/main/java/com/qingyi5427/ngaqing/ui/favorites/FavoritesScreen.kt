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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.design.NgaBackdropScope
import com.qingyi5427.ngaqing.ui.design.ngaBackdropSource
import com.qingyi5427.ngaqing.ui.util.formatAuthorName

@Composable
fun FavoritesScreen(nav: NavHostController, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasDock = !LocalRootNavigationRail.current && WindowInsets.ime.getBottom(LocalDensity.current) == 0
    val dockClearance = 96.dp + with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    var pendingDelete by remember { mutableStateOf<FavoriteEntity?>(null) }
    var pendingMove by remember { mutableStateOf<FavoriteEntity?>(null) }
    var folderInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
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
            .imePadding()
            .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        AppTopBar("收藏", actions = {
            IconButton(onClick = viewModel::syncFromServer, enabled = !syncing) {
                if (syncing) CircularProgressIndicator(Modifier.padding(10.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Sync, contentDescription = "与 NGA 收藏同步")
            }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "排序") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(
                        FavoriteSort.NEWEST to "按收藏时间",
                        FavoriteSort.TITLE to "按标题",
                        FavoriteSort.AUTHOR to "按作者"
                    ).forEach { (sort, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            sortMenu = false
                            viewModel.setSort(sort)
                        })
                    }
                }
            }
        })
        LazyColumn(Modifier.weight(1f).ngaBackdropSource(), contentPadding = PaddingValues(bottom = if (hasDock) dockClearance else 24.dp)) {
            item(key = "favorites-tools", contentType = "tools") {
                FavoritesTools(favorites.size, query, folders, selectedFolder,
                    onQueryChange = { query = it; viewModel.setQuery(it) },
                    onFolderSelect = { selectedFolder = it; viewModel.setFolder(it) })
            }
            if (favorites.isEmpty()) {
                item(key = "favorites-empty", contentType = "empty") {
                    FavoritesEmpty(
                        filtered = query.isNotBlank() || selectedFolder != null
                    )
                }
            } else {
                item(key = "favorites-list-label", contentType = "section-label") {
                    Text("收藏的讨论", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp))
                }
                items(favorites, key = { it.tid }, contentType = { "favorite-row" }) { favorite ->
                    SwipeToDeleteFavorite(
                        favorite,
                        onOpen = { nav.navigate(Routes.postRoute(favorite.tid)) },
                        onDelete = { pendingDelete = favorite },
                        onMove = {
                            pendingMove = favorite
                            folderInput = favorite.folder
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
        }
        }
    }
    if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) { AppBottomBar(nav, Routes.FAVORITES) }
    }
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
    count: Int,
    query: String,
    folders: List<String>,
    selectedFolder: String?,
    onQueryChange: (String) -> Unit,
    onFolderSelect: (String?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
        Text("稍后阅读", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("$count 篇主题 · 按分组整理你的收藏", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        placeholder = { Text("搜索收藏") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (listOf<String?>(null) + folders).forEach { folder ->
            FilterChip(
                selected = selectedFolder == folder,
                onClick = { onFolderSelect(folder) },
                label = { Text(folder ?: "全部") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(end = 8.dp).heightIn(min = 48.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(top = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFavorite(
    item: FavoriteEntity,
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
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "取消收藏",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) {
        FavoriteRow(item, onOpen, onDelete, onMove)
    }
}

@Composable
private fun FavoriteRow(item: FavoriteEntity, onOpen: () -> Unit, onDelete: () -> Unit, onMove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .heightIn(min = 72.dp)
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                if (item.author.isNotBlank()) {
                    Text(
                        formatAuthorName(item.author),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onMove,
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(max = 140.dp)) {
                    Text(item.folder, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = "取消收藏 ${item.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
