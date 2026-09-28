package com.qingyi5427.ngaqing.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(nav: NavHostController, viewModel: CommunityViewModel = hiltViewModel()) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val watched by viewModel.watched.collectAsStateWithLifecycle()
    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .navigationBarsPadding()) {
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
            if (LocalConfiguration.current.screenHeightDp >= 480) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
                    Text("社区动态", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("提醒、私信与关注主题", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                listOf(
                    CommunityTab.NOTIFICATIONS to "提醒",
                    CommunityTab.MESSAGES to "私信",
                    CommunityTab.WATCHING to "关注"
                ).forEach { (value, label) ->
                    Column(Modifier.weight(1f).clickable { viewModel.select(value) }
                        .heightIn(min = 52.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tab == value) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (tab == value) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 13.dp))
                        Box(Modifier.fillMaxWidth().heightIn(min = 2.dp).background(
                            if (tab == value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (tab == CommunityTab.WATCHING) {
                if (watched.isEmpty()) EmptyCommunity("还没有关注主题")
                else LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                ) {
                    items(watched, key = { it.tid }) { item ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                .clickable { nav.navigate(Routes.postRoute(item.tid)) }
                                .padding(start = 0.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title.ifBlank { "（无标题）" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (item.lastKnownReplies > item.lastSeenReplies) "有新回复" else "暂无新回复",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.unwatch(item) }) {
                                Icon(Icons.Filled.Close, contentDescription = "取消关注 ${item.title}")
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            } else when (val current = state) {
                CommunityState.Loading -> Column(Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Text("正在加载消息", modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is CommunityState.Error -> EmptyCommunity(current.message, viewModel::refresh)
                is CommunityState.Content -> {
                    if (current.items.isEmpty()) EmptyCommunity(if (tab == CommunityTab.MESSAGES) "暂无私信" else "暂无提醒")
                    else LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                    ) {
                        items(current.items, key = { it.id }) { item ->
                            CommunityRow(item) {
                                if (item.tid.isNotBlank()) nav.navigate(Routes.postRoute(item.tid))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        Modifier.fillMaxWidth().heightIn(min = 88.dp)
            .clickable(enabled = item.tid.isNotBlank(), onClick = onClick)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.summary.isNotBlank()) Text(item.summary, style = MaterialTheme.typography.bodyMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        val meta = listOfNotNull(item.actor.takeIf { it.isNotBlank() }, item.createdAt.takeIf { it > 0 }?.let(::formatRelative))
        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyCommunity(text: String, onRetry: (() -> Unit)? = null) {
    NgaStatePanel(title = text, actionLabel = if (onRetry != null) "重试" else null, onAction = onRetry)
}
