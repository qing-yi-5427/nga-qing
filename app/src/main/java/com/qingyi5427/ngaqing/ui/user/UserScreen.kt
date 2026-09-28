package com.qingyi5427.ngaqing.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative

@Composable
fun UserScreen(nav: NavHostController, viewModel: UserViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .navigationBarsPadding()) {
            AppTopBar(
                state.profile?.username ?: viewModel.fallbackName,
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
                }
            )
            if (state.loading) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text("正在加载用户资料", modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                item {
                    val profile = state.profile
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 24.dp, vertical = 28.dp)) {
                        Text("用户资料", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!profile?.avatar.isNullOrBlank()) {
                            AsyncImage(profile?.avatar, "用户头像", contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(CircleShape))
                        } else {
                            Box(Modifier.size(64.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, "用户头像",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 18.dp)) {
                            Text(profile?.username ?: viewModel.fallbackName,
                                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text("UID ${profile?.uid ?: viewModel.uid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOfNotNull(profile?.group?.takeIf { it.isNotBlank() }, profile?.title?.takeIf { it.isNotBlank() }).takeIf { it.isNotEmpty() }?.let {
                                Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        }
                        profile?.let { Text("${it.posts} 帖  ·  ${it.followedBy} 关注者",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 20.dp)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("最近主题", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp))
                }
                items(state.topics, key = { it.tid }) { topic ->
                    Column(Modifier.fillMaxWidth().heightIn(min = 88.dp)
                        .clickable { nav.navigate(Routes.postRoute(topic.tid)) }
                        .padding(horizontal = 24.dp, vertical = 14.dp)) {
                        Text(topic.subject, style = MaterialTheme.typography.titleMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${topic.replies} 回复 · ${formatRelative(topic.lastPostDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (state.topics.isEmpty()) item {
                    UserStatus(state.error ?: "暂无公开主题",
                        onRetry = if (state.error != null) viewModel::refresh else null)
                }
            }
        }
    }
}

@Composable
private fun UserStatus(message: String, onRetry: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp).heightIn(min = 48.dp)) {
                Text("重试")
            }
        }
    }
}
