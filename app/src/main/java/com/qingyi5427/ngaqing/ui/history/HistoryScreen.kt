package com.qingyi5427.ngaqing.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.design.NgaGlassSurface
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative
import com.qingyi5427.ngaqing.ui.util.formatAuthorName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavHostController, viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(HistorySort.RECENT) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val compactHeight = LocalConfiguration.current.screenHeightDp < 480 ||
        WindowInsets.ime.getBottom(LocalDensity.current) > 0

    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .imePadding().navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppTopBar(
                title = "浏览历史",
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "清空历史")
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "排序")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf(
                                HistorySort.RECENT to "按浏览时间",
                                HistorySort.TITLE to "按标题",
                                HistorySort.AUTHOR to "按作者"
                            ).forEach { (sort, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    selectedSort = sort
                                    viewModel.setSort(sort)
                                    sortMenu = false
                                })
                            }
                        }
                    }
                }
            )
            if (!compactHeight) {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
                    Text("阅读轨迹", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${history.size} 篇记录 · 继续上次的阅读位置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            } else {
                Text("${history.size} 篇记录", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp))
            }
            NgaGlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp,
                vertical = if (compactHeight) 4.dp else 10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setQuery(it)
                },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("搜索浏览历史") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp))
            if (history.isEmpty()) {
                NgaStatePanel(
                    title = if (query.isNotBlank()) "没有匹配的记录" else "还没有浏览记录",
                    description = if (query.isNotBlank()) "试试更短的关键词。"
                    else "打开帖子后会自动记录，并在下次继续上次阅读位置。"
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    val groups = if (selectedSort == HistorySort.RECENT) {
                        history.groupBy { historyGroup(it.lastVisited) }
                    } else {
                        linkedMapOf("全部记录" to history)
                    }
                    groups.forEach { (label, groupItems) ->
                        item(key = "header:$label", contentType = "history-header") {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 20.dp, vertical = 15.dp)
                            )
                        }
                    items(groupItems, key = { it.tid }, contentType = { "history-row" }) { item ->
                        val dismiss = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.remove(item.tid)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismiss,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(end = 22.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text("删除", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        ) {
                            HistoryRow(
                                item,
                                onClick = { nav.navigate(Routes.postRoute(item.tid)) },
                                onDelete = { viewModel.remove(item.tid) }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("清空浏览历史？") },
            text = { Text("阅读位置也会一起清除，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消") } }
        )
    }
}

private fun historyGroup(timestamp: Long): String {
    val now = java.util.Calendar.getInstance()
    val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val dayDiff = ((now.timeInMillis - date.timeInMillis) / 86_400_000L).toInt()
    return when {
        now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR) -> "今天"
        dayDiff <= 1 -> "昨天"
        now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) ->
            "${date.get(java.util.Calendar.MONTH) + 1}月"
        else -> "${date.get(java.util.Calendar.YEAR)}年${date.get(java.util.Calendar.MONTH) + 1}月"
    }
}

@Composable
private fun HistoryRow(item: HistoryEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1f).clickable(onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildList {
                    if (item.author.isNotBlank()) add(formatAuthorName(item.author))
                    add(formatRelative(item.lastVisited))
                    if (item.lastFloor > 0) add("读到 ${item.lastFloor} 楼")
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "删除浏览记录 ${item.title}")
        }
    }
}
