package com.qingyi5427.ngaqing.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.util.formatAuthorName

@Composable
fun FavoritesScreen(nav: NavHostController, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<FavoriteEntity?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("收藏")
        Box(Modifier.weight(1f)) {
            if (favorites.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 80.dp)
                    )
                    Text(
                        "还没有收藏",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        "阅读帖子时点右上角收藏，稍后可以从这里继续。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(favorites, key = { it.tid }, contentType = { "favorite-row" }) { favorite ->
                        SwipeToDeleteFavorite(
                            favorite,
                            onOpen = { nav.navigate(Routes.postRoute(favorite.tid)) },
                            onDelete = { pendingDelete = favorite }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                }
            }
        }
        AppBottomBar(nav, Routes.FAVORITES)
    }

    pendingDelete?.let { favorite ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
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
                    }
                ) {
                    Text("取消收藏", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("保留") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFavorite(
    item: FavoriteEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
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
        FavoriteRow(item, onOpen, onDelete)
    }
}

@Composable
private fun FavoriteRow(item: FavoriteEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.author.isNotBlank()) {
                Text(
                    formatAuthorName(item.author),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
