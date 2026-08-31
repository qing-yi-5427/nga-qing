package com.qingyi5427.ngaqing.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative

@Composable
fun CommunityScreen(nav: NavHostController, viewModel: CommunityViewModel = hiltViewModel()) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val watched by viewModel.watched.collectAsStateWithLifecycle()
    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                "消息与关注",
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (tab != CommunityTab.WATCHING) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    CommunityTab.NOTIFICATIONS to "提醒",
                    CommunityTab.MESSAGES to "私信",
                    CommunityTab.WATCHING to "关注"
                ).forEach { (value, label) ->
                    FilterChip(selected = tab == value, onClick = { viewModel.select(value) }, label = { Text(label) })
                }
            }
            if (tab == CommunityTab.WATCHING) {
                if (watched.isEmpty()) EmptyCommunity("还没有关注主题")
                else LazyColumn(Modifier.fillMaxSize()) {
                    items(watched, key = { it.tid }) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { nav.navigate(Routes.postRoute(item.tid)) }
                                .padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title.ifBlank { "（无标题）" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (item.lastKnownReplies > item.lastSeenReplies) "有新回复" else "暂无新回复",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.unwatch(item) }) {
                                Icon(Icons.Filled.Close, contentDescription = "取消关注")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            } else when (val current = state) {
                CommunityState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is CommunityState.Error -> EmptyCommunity(current.message)
                is CommunityState.Content -> {
                    if (current.items.isEmpty()) EmptyCommunity(if (tab == CommunityTab.MESSAGES) "暂无私信" else "暂无提醒")
                    else LazyColumn(Modifier.fillMaxSize()) {
                        items(current.items, key = { it.id }) { item ->
                            CommunityRow(item) {
                                if (item.tid.isNotBlank()) nav.navigate(Routes.postRoute(item.tid))
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(item: CommunityItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(enabled = item.tid.isNotBlank(), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.summary.isNotBlank()) Text(item.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val meta = listOfNotNull(item.actor.takeIf { it.isNotBlank() }, item.createdAt.takeIf { it > 0 }?.let(::formatRelative))
        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyCommunity(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
