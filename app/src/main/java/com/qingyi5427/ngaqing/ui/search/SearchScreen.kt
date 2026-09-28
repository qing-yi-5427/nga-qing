package com.qingyi5427.ngaqing.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.ui.design.NgaGlassSurface
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import com.qingyi5427.ngaqing.ui.util.formatRelative
import com.qingyi5427.ngaqing.ui.util.formatAuthorName

@Composable
fun SearchScreen(
    nav: NavHostController,
    fid: String?,
    stid: String?,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val compactHeight = LocalConfiguration.current.screenHeightDp < 480 ||
        WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val submit: () -> Unit = {
        viewModel.search(query, fid, stid)
        keyboardController?.hide()
        Unit
    }
    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .imePadding().navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
        AppTopBar(
            title = "搜索",
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )
        if (!compactHeight) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
                Text("搜索讨论", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (fid.isNullOrBlank() && stid.isNullOrBlank()) "范围 · 全站主题" else "范围 · 当前版块",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        } else {
            Text(
                if (fid.isNullOrBlank() && stid.isNullOrBlank()) "全站主题" else "当前版块",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
        }
        NgaGlassSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp,
            vertical = if (compactHeight) 4.dp else 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (fid.isNullOrBlank() && stid.isNullOrBlank()) "搜索主题" else "在当前版块搜索") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = submit, enabled = query.isNotBlank()) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.weight(1f)) {
            when (val current = state) {
                is SearchUiState.Empty -> SearchHint()
                is SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SearchUiState.Error -> SearchError(current.msg, submit)
                is SearchUiState.Success -> {
                    if (current.threads.isEmpty()) {
                        SearchMessage("没有找到相关主题", "试试更短的关键词，或检查当前搜索范围。")
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                            item(key = "result-summary", contentType = "result-summary") {
                                Text("找到 ${current.threads.size} 篇讨论",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp))
                            }
                            if (current.fromCache) {
                                item(key = "offline-banner", contentType = "offline-banner") {
                                    Text(
                                        "当前显示离线搜索结果",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }
                            }
                            items(
                                current.threads,
                                key = { it.tid },
                                contentType = { "search-result" }
                            ) { thread ->
                                SearchResultRow(thread) { nav.navigate(Routes.postRoute(thread.tid)) }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SearchHint() {
    SearchMessage("搜索主题", "输入标题关键词，按键盘搜索键开始。")
}

@Composable
private fun SearchResultRow(item: ThreadItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            item.subject.ifBlank { "（无标题）" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${item.forumName.ifBlank { formatAuthorName(item.author) }} · ${item.replies} 回复 · ${formatRelative(item.postDate)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SearchError(message: String, retry: () -> Unit) {
    SearchMessage("搜索失败", message, retry)
}

@Composable
private fun SearchMessage(title: String, detail: String, retry: (() -> Unit)? = null) {
    NgaStatePanel(title, detail, actionLabel = if (retry == null) null else "重试", onAction = retry)
}
