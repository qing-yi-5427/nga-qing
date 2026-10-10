package com.qingyi5427.ngaqing.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qingyi5427.ngaqing.ui.design.NgaGlassSurface

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ReaderDock(
    stage: ReaderDockStage,
    replyCount: Int,
    currentPage: Int,
    totalPages: Int,
    isFavorite: Boolean,
    onlyAuthor: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onFavorite: () -> Unit,
    onPage: () -> Unit,
    onJump: () -> Unit,
    onOnlyAuthor: () -> Unit,
    onReply: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onInteraction: () -> Unit,
    onPressChanged: (Boolean) -> Unit = {},
    compact: Boolean,
    availableWidthDp: Float,
    modifier: Modifier = Modifier
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val currentPressChanged = rememberUpdatedState(onPressChanged)
    val interacted: (() -> Unit) -> (() -> Unit) = { action ->
        { onInteraction(); action() }
    }
    NgaGlassSurface(
        modifier = modifier
            .testTag("post-quick-actions")
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusGroup()
            .onKeyEvent { onInteraction(); false }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    currentPressChanged.value(true)
                    try {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    } finally {
                        currentPressChanged.value(false)
                    }
                }
            },
        shape = RoundedCornerShape(28.dp)
    ) {
        if (stage == ReaderDockStage.PEEK) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = interacted(onExpand))
                    .semantics {
                        contentDescription = "展开阅读操作，${replyCount.coerceAtLeast(0)} 条回复"
                        role = Role.Button
                    }
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("回复 ${replyCount.coerceAtLeast(0)}", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Filled.ExpandLess, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (stage == ReaderDockStage.EXPANDED) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                // Six compact actions need 301dp including the outer insets and spacer.
                maxItemsInEachRow = if (availableWidthDp in 1f..<302f) 3 else Int.MAX_VALUE,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = interacted(onFavorite), modifier = Modifier.size(48.dp)) {
                    Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier.size(48.dp).clickable(onClick = interacted(onPage))
                        .semantics {
                            contentDescription = "翻页，当前第 $currentPage 页，共 $totalPages 页"
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(compactPageLabel(currentPage, totalPages, largeText),
                        style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = interacted(onJump), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "跳转楼层", Modifier.size(20.dp))
                }
                IconButton(onClick = interacted(onOnlyAuthor), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.PersonSearch,
                        contentDescription = if (onlyAuthor) "查看全部回复" else "只看楼主",
                        tint = if (onlyAuthor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
                if (compact) {
                    IconButton(onClick = interacted(onReply), modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "回复", Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Spacer(Modifier.width(3.dp))
                    Surface(
                        onClick = interacted(onReply),
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(Modifier.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp))
                            Text("回复", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                IconButton(onClick = interacted(onCollapse), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.ExpandLess, contentDescription = "收起阅读操作", Modifier.size(18.dp))
                }
            }
        }
    }
}
