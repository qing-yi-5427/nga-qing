package com.qingyi5427.ngaqing.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        Column(Modifier.fillMaxSize()) {
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
            if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(Modifier.fillMaxSize()) {
                item {
                    val profile = state.profile
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!profile?.avatar.isNullOrBlank()) {
                            AsyncImage(profile?.avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                        } else {
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null) }
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(profile?.username ?: viewModel.fallbackName, style = MaterialTheme.typography.titleLarge)
                            Text("UID ${profile?.uid ?: viewModel.uid}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOfNotNull(profile?.group?.takeIf { it.isNotBlank() }, profile?.title?.takeIf { it.isNotBlank() }).takeIf { it.isNotEmpty() }?.let {
                                Text(it.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
                            }
                            profile?.let { Text("${it.posts} 帖 · ${it.followedBy} 关注者", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    HorizontalDivider()
                    Text("最近主题", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(18.dp))
                }
                items(state.topics, key = { it.tid }) { topic ->
                    Column(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.postRoute(topic.tid)) }.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text(topic.subject, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${topic.replies} 回复 · ${formatRelative(topic.lastPostDate)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
                if (state.topics.isEmpty()) item { Text(state.error ?: "暂无公开主题", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
