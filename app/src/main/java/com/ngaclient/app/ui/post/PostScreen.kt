package com.ngaclient.app.ui.post

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ngaclient.app.data.model.Post
import com.ngaclient.app.data.local.NgaDomains
import com.ngaclient.app.data.remote.NgaInterceptor
import com.ngaclient.app.ui.theme.LocalGlassPalette
import com.ngaclient.app.ui.gesture.SwipeBackContainer
import com.ngaclient.app.ui.util.formatRelative
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    nav: NavHostController,
    viewModel: PostViewModel = hiltViewModel(),
    dark: Boolean
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val fav by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val targetFloor by viewModel.targetFloor.collectAsStateWithLifecycle()
    val totalRows by viewModel.totalRows.collectAsStateWithLifecycle()
    val onlyAuthor by viewModel.onlyAuthor.collectAsStateWithLifecycle()
    val ngaDomain by viewModel.ngaDomain.collectAsStateWithLifecycle()
    val webCookiesReady by viewModel.webCookiesReady.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val viewerUrl = remember { mutableStateOf<String?>(null) }
    val webFallback = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val ctx = LocalContext.current

    var replyOpen by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var jumpOpen by remember { mutableStateOf(false) }
    var jumpText by remember { mutableStateOf("") }
    var pagePickerOpen by remember { mutableStateOf(false) }
    var pageText by remember { mutableStateOf("") }
    var quickActionsVisible by remember { mutableStateOf(true) }
    var quickActionsExpanded by remember { mutableStateOf(false) }
    var suppressQuickActionScroll by remember { mutableStateOf(false) }
    val replying by viewModel.replying.collectAsStateWithLifecycle()
    val replyResult by viewModel.replyResult.collectAsStateWithLifecycle()

    LaunchedEffect(webFallback.value, ngaDomain) {
        if (webFallback.value) viewModel.prepareWebCookies()
    }

    // 回复结果提示
    LaunchedEffect(replyResult) {
        replyResult?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            viewModel.consumeReplyResult()
            if (it.contains("成功")) {
                replyOpen = false
                replyText = ""
            }
        }
    }

    // 滚到底部附近自动加载下一页（无限滚动）
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { info ->
                val total = info.totalItemsCount
                val visible = info.visibleItemsInfo.size
                if (total == 0 || visible >= total) return@collect
                if (info.visibleItemsInfo.last().index >= total - 4) viewModel.loadMore()
            }
    }

    // 下滑阅读时让快捷入口退出，反向上滑时再出现，避免长期遮挡正文。
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        var accumulatedDelta = 0
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { (index, offset) ->
            val delta = when {
                index > previousIndex -> 48
                index < previousIndex -> -48
                else -> offset - previousOffset
            }
            previousIndex = index
            previousOffset = offset
            if (suppressQuickActionScroll || delta == 0) {
                accumulatedDelta = 0
                return@collect
            }

            accumulatedDelta = when {
                delta > 0 && accumulatedDelta < 0 -> delta
                delta < 0 && accumulatedDelta > 0 -> delta
                else -> accumulatedDelta + delta
            }
            when {
                accumulatedDelta >= 12 -> {
                    quickActionsVisible = false
                    quickActionsExpanded = false
                    accumulatedDelta = 0
                }
                accumulatedDelta <= -12 -> {
                    quickActionsVisible = true
                    accumulatedDelta = 0
                }
            }
        }
    }

    LaunchedEffect(s, targetFloor) {
        val current = s as? PostUiState.Success ?: return@LaunchedEffect
        val target = targetFloor ?: return@LaunchedEffect
        val index = current.posts.indexOfFirst { it.lou >= target }.takeIf { it >= 0 } ?: 0
        suppressQuickActionScroll = true
        try {
            listState.scrollToItem(index)
        } finally {
            suppressQuickActionScroll = false
        }
        quickActionsVisible = true
        quickActionsExpanded = false
        viewModel.consumeTargetFloor()
    }

    LaunchedEffect(s, listState) {
        val current = s as? PostUiState.Success ?: return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                current.posts.getOrNull(index)?.let { viewModel.updateReadFloor(it.lou) }
            }
    }

    SwipeBackContainer(
        onBack = { nav.popBackStack() },
        enabled = viewerUrl.value == null && !replyOpen && !pagePickerOpen && !jumpOpen,
        // WebView 必须独占内容区触摸；否则斜向的上下滑会被全屏右滑返回识别器截断。
        // 网页模式仍保留 Android 左侧边缘的系统预测性返回。
        contentSwipeEnabled = !webFallback.value
    ) {
        Column(Modifier.fillMaxSize()) {
        com.ngaclient.app.ui.chrome.AppTopBar(
            title = subject.ifBlank { "帖子" },
            onTitleClick = {
                scope.launch {
                    suppressQuickActionScroll = true
                    try {
                        listState.animateScrollToItem(0)
                    } finally {
                        suppressQuickActionScroll = false
                        quickActionsVisible = true
                        quickActionsExpanded = false
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (fav) "取消收藏" else "收藏",
                        tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("跳转楼层") },
                            leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                            onClick = {
                                menuOpen = false
                                jumpText = ""
                                jumpOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("刷新当前页") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            onClick = {
                                menuOpen = false
                                viewModel.refresh()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("分享链接") },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = {
                                menuOpen = false
                                val url = NgaDomains.url(ngaDomain, "read.php?tid=${viewModel.tid}")
                                ctx.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                            putExtra(Intent.EXTRA_TEXT, url)
                                        },
                                        "分享帖子"
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("在浏览器打开") },
                            leadingIcon = { Icon(Icons.Filled.OpenInBrowser, null) },
                            onClick = {
                                menuOpen = false
                                ctx.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(NgaDomains.url(ngaDomain, "read.php?tid=${viewModel.tid}"))
                                    )
                                )
                            }
                        )
                    }
                }
            }
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when (val cur = s) {
                is PostUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is PostUiState.Error -> {
                    if (webFallback.value) {
                        if (webCookiesReady) {
                            WebPageView(viewModel.tid, ngaDomain)
                        } else {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        ErrorView(cur.msg, cur.raw) { webFallback.value = true }
                    }
                }
                is PostUiState.Success -> {
                    val posts = cur.posts
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(
                            posts,
                            key = { it.pid.ifBlank { it.lou } },
                            contentType = { "post-floor" }
                        ) { post ->
                            PostCard(post, cur.renderData[post.renderKey()], { viewerUrl.value = it }) { floor ->
                                val idx = posts.indexOfFirst { it.lou == floor }
                                if (idx >= 0) {
                                    scope.launch {
                                        suppressQuickActionScroll = true
                                        try {
                                            listState.animateScrollToItem(idx)
                                        } finally {
                                            suppressQuickActionScroll = false
                                            quickActionsVisible = true
                                            quickActionsExpanded = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(ctx, "该楼层尚未加载", Toast.LENGTH_SHORT).show()
                                }
                            }
                            Spacer(
                                Modifier.fillMaxWidth().height(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                            )
                        }
                        if (isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (loadError != null) {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(loadError ?: "加载失败", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    TextButton(onClick = { viewModel.retryMore() }) { Text("重试") }
                                }
                            }
                        } else if (cur.page >= cur.totalPages) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                                    Text("— 没有更多了 —", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(onClick = { viewModel.loadMore() }) {
                                        Text("加载下一页（${cur.page}/${cur.totalPages}）")
                                    }
                                }
                            }
                        }
                    }
                    if (posts.isNotEmpty()) {
                        val readingPage by remember(posts, cur.page, cur.totalPages) {
                            derivedStateOf {
                                posts.getOrNull(listState.firstVisibleItemIndex)?.lou
                                    ?.div(30)?.plus(1)
                                    ?.coerceIn(1, cur.totalPages)
                                    ?: cur.page
                            }
                        }
                        PostQuickActions(
                            visible = quickActionsVisible,
                            expanded = quickActionsExpanded,
                            currentPage = readingPage,
                            totalPages = cur.totalPages,
                            onlyAuthor = onlyAuthor,
                            onToggle = { quickActionsExpanded = !quickActionsExpanded },
                            onReply = {
                                quickActionsExpanded = false
                                replyOpen = true
                            },
                            onPage = {
                                quickActionsExpanded = false
                                pageText = readingPage.toString()
                                pagePickerOpen = true
                            },
                            onOnlyAuthor = {
                                quickActionsExpanded = false
                                quickActionsVisible = true
                                viewModel.toggleOnlyAuthor()
                            },
                            modifier = Modifier.align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = 16.dp, bottom = 18.dp)
                        )
                    }
                }
            }
        }
        }
    }

    if (pagePickerOpen) {
        val current = s as? PostUiState.Success
        val maxPage = current?.totalPages?.coerceAtLeast(1) ?: 1
        val selectedPage = pageText.toIntOrNull()
        ModalBottomSheet(
            onDismissRequest = { pagePickerOpen = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("翻页", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "共 $maxPage 页",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { value -> pageText = value.filter(Char::isDigit).take(6) },
                    label = { Text("页码") },
                    supportingText = { Text("输入 1–$maxPage") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val target = ((selectedPage ?: current?.page ?: 1) - 1).coerceAtLeast(1)
                            pagePickerOpen = false
                            viewModel.goToPage(target)
                        },
                        enabled = (selectedPage ?: current?.page ?: 1) > 1,
                        modifier = Modifier.weight(1f)
                    ) { Text("上一页") }
                    TextButton(
                        onClick = {
                            val target = ((selectedPage ?: current?.page ?: 1) + 1).coerceAtMost(maxPage)
                            pagePickerOpen = false
                            viewModel.goToPage(target)
                        },
                        enabled = (selectedPage ?: current?.page ?: 1) < maxPage,
                        modifier = Modifier.weight(1f)
                    ) { Text("下一页") }
                }
                Button(
                    onClick = {
                        pagePickerOpen = false
                        viewModel.goToPage(selectedPage ?: 1)
                    },
                    enabled = selectedPage != null && selectedPage in 1..maxPage,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("前往第 ${selectedPage ?: "-"} 页") }
            }
        }
    }

    if (jumpOpen) {
        val floor = jumpText.toIntOrNull()
        AlertDialog(
            onDismissRequest = { jumpOpen = false },
            title = { Text("跳转楼层") },
            text = {
                Column {
                    Text("可输入 0–$totalRows，0 表示主楼。")
                    OutlinedTextField(
                        value = jumpText,
                        onValueChange = { value -> jumpText = value.filter(Char::isDigit).take(8) },
                        label = { Text("楼层") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = floor != null && floor in 0..totalRows,
                    onClick = {
                        jumpOpen = false
                        viewModel.jumpToFloor(floor ?: 0)
                    }
                ) { Text("跳转") }
            },
            dismissButton = { TextButton(onClick = { jumpOpen = false }) { Text("取消") } }
        )
    }

    // 全屏图片查看器（覆盖在内容之上）
    viewerUrl.value?.let { url ->
        Dialog(
            onDismissRequest = { viewerUrl.value = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            ZoomableImageOverlay(url = url, onClose = { viewerUrl.value = null })
        }
    }

    // 可展开的回复编辑器：为键盘、长文本和后续引用/图片工具栏预留空间。
    if (replyOpen) {
        ModalBottomSheet(
            onDismissRequest = { if (!replying) replyOpen = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("回复帖子", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { replyOpen = false }, enabled = !replying) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = { viewModel.reply(replyText) },
                        enabled = !replying && replyText.isNotBlank()
                    ) {
                        if (replying) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("发送")
                        }
                    }
                }
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("写下你的回复……") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "发送前会保留当前内容；只有服务器明确确认成功后编辑器才会关闭。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun PostQuickActions(
    visible: Boolean,
    expanded: Boolean,
    currentPage: Int,
    totalPages: Int,
    onlyAuthor: Boolean,
    onToggle: () -> Unit,
    onReply: () -> Unit,
    onPage: () -> Unit,
    onOnlyAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Column(horizontalAlignment = Alignment.End) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    QuickActionChip(
                        label = if (onlyAuthor) "查看全部回复" else "只看作者",
                        onClick = onOnlyAuthor
                    ) {
                        Icon(Icons.Filled.PersonSearch, contentDescription = null, Modifier.size(18.dp))
                    }
                    QuickActionChip(
                        label = "翻页 · $currentPage / $totalPages",
                        onClick = onPage
                    ) {
                        Icon(Icons.Filled.SwapVert, contentDescription = null, Modifier.size(18.dp))
                    }
                    QuickActionChip(label = "写回复", onClick = onReply, primary = true) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, Modifier.size(18.dp))
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (expanded) 45f else 0f,
                label = "帖子快捷菜单"
            )
            FloatingActionButton(
                onClick = onToggle,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = if (expanded) "收起帖子操作" else "展开帖子操作",
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (primary) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(42.dp).padding(start = 14.dp, end = 12.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(9.dp))
            icon()
        }
    }
}

/** 连续讨论流中的单个楼层：标题、作者、正文和楼中楼保持清晰但克制的层级。 */
@Composable
private fun PostCard(
    post: Post,
    renderData: PostRenderData?,
    onImage: (String) -> Unit,
    onJumpToFloor: (Int) -> Unit
) {
    val g = LocalGlassPalette.current
    val blocks = renderData?.body.orEmpty()
    val sigBlocks = renderData?.signature.orEmpty()

    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        if (post.lou == 0) {
            Text(
                post.subject.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
        }
        // 作者行
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(post.avatar, 36.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.author.ifBlank { "匿名" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.lou == 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(start = 7.dp)
                        ) {
                            Text(
                                "楼主",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    formatRelative(post.postDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "#${post.lou}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        // 正文
        blocks.forEach { block -> RenderBlock(block, onImage, onJumpToFloor) }
        // 签名
        if (sigBlocks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(g.rowDivider))
            Spacer(Modifier.height(6.dp))
            Column {
                sigBlocks.forEach { block -> RenderBlock(block, onImage, onJumpToFloor) }
            }
        }
        if (post.comments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val commentAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(0.dp, 10.dp, 10.dp, 0.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                    .leadingAccent(commentAccent)
            ) {
                Column(
                    Modifier.weight(1f)
                        .padding(start = 14.dp, end = 11.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    post.comments.forEachIndexed { index, comment ->
                        Text(
                            comment.author.ifBlank { "匿名" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        renderData?.comments?.getOrNull(index).orEmpty().forEach {
                            RenderBlock(it, onImage, onJumpToFloor)
                        }
                        if (index != post.comments.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                            Spacer(Modifier.fillMaxWidth().height(1.dp).background(g.rowDivider))
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/** 真实头像加载期间或失败时显示中性用户图标，不使用单字圆形伪头像。 */
@Composable
private fun AuthorAvatar(avatar: String, size: androidx.compose.ui.unit.Dp) {
    var loaded by remember(avatar) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!loaded) {
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "默认作者头像",
                        modifier = Modifier.size(size * 0.56f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            }
        }
        if (avatar.isNotBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = "作者头像",
                contentScale = ContentScale.Crop,
                onLoading = { loaded = false },
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier.size(size).clip(CircleShape)
                    .graphicsLayer { alpha = if (loaded) 1f else 0f }
            )
        }
    }
}

private enum class ContentImageState { Loading, Success, Error }

/** 渲染单个正文块。 */
@Composable
private fun RenderBlock(
    block: PostBlock,
    onImage: (String) -> Unit,
    onJumpToFloor: (Int) -> Unit
) {
    val g = LocalGlassPalette.current
    when (block) {
        is PostBlock.Text -> {
            if (block.text.isBlank()) return
            Text(
                block.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (block.bold) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (block.strike) TextDecoration.LineThrough else TextDecoration.None,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        is PostBlock.Img -> {
            if (block.url.isBlank()) return
            if (block.emote) {
                AsyncImage(
                    model = block.url,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                var retryKey by remember(block.url) { mutableStateOf(0) }
                key(retryKey) {
                    var state by remember { mutableStateOf(ContentImageState.Loading) }
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                            .padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onLoading = { state = ContentImageState.Loading },
                            onSuccess = { state = ContentImageState.Success },
                            onError = { state = ContentImageState.Error },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                                .then(
                                    if (state == ContentImageState.Success) {
                                        Modifier.clickable { onImage(block.url) }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                        when (state) {
                            ContentImageState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            }
                            ContentImageState.Error -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { retryKey++ },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("图片加载失败，点按重试", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            }
                            ContentImageState.Success -> Unit
                        }
                    }
                }
            }
        }
        is PostBlock.Quote -> {
            val floor = block.floor
            val quoteAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
                    .background(g.quoteBg)
                    .leadingAccent(quoteAccent)
                    .then(
                        if (floor != null) Modifier.clickable { onJumpToFloor(floor) } else Modifier
                    )
            ) {
                Column(
                    Modifier.weight(1f)
                        .padding(start = 14.dp, end = 11.dp, top = 9.dp, bottom = 9.dp)
                ) {
                    val head = buildString {
                        append("回复")
                        if (block.refName != null) append(" @${block.refName}")
                        if (floor != null) append(" · #$floor")
                    }
                    Text(
                        head,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    block.blocks.forEach { RenderBlock(it, onImage, onJumpToFloor) }
                }
            }
        }
    }
}

/** Draws a full-height leading rule without requesting intrinsic measurements from child layouts. */
private fun Modifier.leadingAccent(color: Color): Modifier = drawBehind {
    val strokeWidth = 3.dp.toPx()
    drawLine(
        color = color,
        start = Offset(strokeWidth / 2f, 0f),
        end = Offset(strokeWidth / 2f, size.height),
        strokeWidth = strokeWidth
    )
}

/** JSON 解析失败时的错误视图：主操作按钮置顶。 */
@Composable
private fun ErrorView(msg: String, raw: String, onWebFallback: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            msg.take(120),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onWebFallback,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("用网页版打开", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "JSON 解析失败（NGA 服务器响应可能被截断）。网页版不受影响，可直接浏览、翻页。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (raw.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "原始返回（诊断用，前 800 字）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                raw.take(800),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * 网页版回退：JSON 解析失败时，直接用 WebView 打开 NGA 原始帖子网页。
 */
@Composable
private fun WebPageView(tid: String, ngaDomain: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // 创建时清掉旧的未登录页面，但之后允许脚本/图片正常缓存；
                // LOAD_NO_CACHE 会让网页滚动期间的资源复用明显变差。
                clearCache(true)
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString = NgaInterceptor.UA
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
            }
        },
        update = { w ->
            val url = NgaDomains.url(ngaDomain, "read.php?tid=$tid")
            // Use a tag instead of comparing w.url: some compatibility domains redirect
            // to the main site, and comparing URLs would otherwise create a reload loop.
            if (w.tag != url) {
                w.tag = url
                w.loadUrl(url)
            }
        },
        onRelease = { w ->
            w.clearFocus()
            w.onPause()
            w.destroy()
        }
    )
}

/**
 * 全屏图片查看器：双指缩放、单指拖拽、双击放大/还原、未缩放时单击关闭。
 */
@Composable
private fun ZoomableImageOverlay(url: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.5f) 1f else 3f
                        offset = Offset.Zero
                    },
                    onTap = { if (scale <= 1.05f) onClose() }
                )
            }
    ) {
        val density = LocalDensity.current
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }

        val transformState = rememberTransformableState { zoom, pan, _ ->
            val newScale = (scale * zoom).coerceIn(1f, 5f)
            offset = offset + pan
            scale = newScale
        }

        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(transformState)
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
        }
    }
}
