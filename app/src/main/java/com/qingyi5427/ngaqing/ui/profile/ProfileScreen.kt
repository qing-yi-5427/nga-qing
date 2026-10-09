package com.qingyi5427.ngaqing.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.rememberScrollDockState
import com.qingyi5427.ngaqing.ui.design.NgaBackdropScope
import com.qingyi5427.ngaqing.ui.design.ngaBackdropSource
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.root.ProfileLoadState
import com.qingyi5427.ngaqing.data.local.AccountDisplay
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileScreen(
    nav: NavHostController,
    root: RootViewModel = hiltViewModel(),
    onAvatarLoadError: ((Throwable) -> Unit)? = null
) {
    val display by root.accountDisplay.collectAsStateWithLifecycle()
    val profileLoad by root.profileLoad.collectAsStateWithLifecycle()
    LaunchedEffect(display.uid, display.revision) {
        if (display.uid.isNotBlank()) root.refreshOwnProfile()
    }
    val hasDock = !LocalRootNavigationRail.current
    val dockState = rememberScrollDockState()
    val dockClearance = 96.dp + with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    NgaBackdropScope {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .nestedScroll(dockState.nestedScrollConnection)
        .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("我的")
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth()
                .testTag("profile-scroll")
                .ngaBackdropSource()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
        ) {
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxWidth().padding(bottom = if (hasDock) dockClearance else 24.dp)) {
                    ProfileIdentity(display, profileLoad, { root.refreshOwnProfile() }, onAvatarLoadError,
                        Modifier.weight(0.38f).heightIn(min = 450.dp))
                    ProfileEntries(nav, Modifier.weight(0.62f))
                }
            } else {
                Column(Modifier.padding(bottom = if (hasDock) dockClearance else 24.dp)) {
                    ProfileIdentity(display, profileLoad, { root.refreshOwnProfile() }, onAvatarLoadError,
                        Modifier.fillMaxWidth())
                    ProfileEntries(nav, Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (hasDock) Box(Modifier.align(Alignment.BottomCenter)) {
        AppBottomBar(nav, Routes.PROFILE, visible = dockState.visible)
    }
    }
    }
}

@Composable
private fun ProfileIdentity(
    display: AccountDisplay,
    load: ProfileLoadState,
    onRetry: () -> Unit,
    onAvatarLoadError: ((Throwable) -> Unit)?,
    modifier: Modifier
) {
    val name = display.username
    val uid = display.uid
    var avatarLoaded by remember(display.avatar) { mutableStateOf(false) }
    var avatarFailed by remember(display.avatar) { mutableStateOf(false) }
    Column(modifier.background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 24.dp, vertical = 28.dp)) {
        Text("个人空间", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(68.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarLoaded) Text(
                    name.codePoints().limit(1).toArray().firstOrNull()?.let { String(Character.toChars(it)) } ?: "N",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (display.avatar.isNotBlank()) {
                    AsyncImage(
                        model = display.avatar,
                        contentDescription = "用户头像",
                        contentScale = ContentScale.Crop,
                        onSuccess = { avatarLoaded = true; avatarFailed = false },
                        onError = {
                            avatarLoaded = false
                            avatarFailed = true
                            onAvatarLoadError?.invoke(it.result.throwable)
                        },
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer { alpha = if (avatarLoaded) 1f else 0f }
                            .testTag(if (avatarLoaded) "profile-avatar-loaded" else if (avatarFailed)
                                "profile-avatar-failed" else "profile-avatar-pending")
                            .semantics { stateDescription = if (avatarLoaded) "头像已加载" else if (avatarFailed)
                                "头像加载失败" else "头像加载中" }
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Text(name.ifBlank { "NGA 用户" }, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                if (uid.isNotBlank()) Text("UID $uid", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
                if (load.uid == uid && load.revision == display.revision) {
                    if (load.loading && name.isBlank()) Text("正在更新资料",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (load.error != null) TextButton(onClick = onRetry) {
                        Text("资料更新失败，重试")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ProfileEntries(nav: NavHostController, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text("我的内容", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                ProfileRow("消息与关注", "提醒、私信和关注主题", Icons.Outlined.Notifications) {
                    nav.navigate(Routes.COMMUNITY)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileRow("浏览历史", "接着阅读看过的帖子", Icons.Outlined.History) {
                    nav.navigate(Routes.HISTORY)
                }
                Spacer(Modifier.height(28.dp))
                Text("应用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                ProfileRow("设置与阅读偏好", "账号、外观与内容过滤", Icons.Filled.Settings) {
                    nav.navigate(Routes.SETTINGS)
                }
    }
}

@Composable
private fun ProfileRow(label: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
