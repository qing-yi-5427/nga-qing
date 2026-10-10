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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.chrome.AppBottomBar
import com.qingyi5427.ngaqing.ui.chrome.EditorialRootTopBar
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
    val headerMargin = if (LocalV8LayoutPolicy.current.availableWidthDp >= 600f) 34.dp else 20.dp
    val dockState = rememberScrollDockState()
    val dockClearance = 96.dp + with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    NgaBackdropScope {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .nestedScroll(dockState.nestedScrollConnection)
        .then(if (hasDock) Modifier else Modifier.navigationBarsPadding())) {
    Column(Modifier.fillMaxSize()) {
        EditorialRootTopBar(nav, Routes.PROFILE, "我的空间",
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = headerMargin))
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            val fontScale = LocalDensity.current.fontScale
            val margin = if (maxWidth >= 600.dp) 34.dp else 20.dp
            val suppliedPolicy = LocalV8LayoutPolicy.current
            val policy = if (suppliedPolicy.availableWidthDp > 0f) suppliedPolicy
                else decideV8LayoutPolicy(maxWidth.value, maxHeight.value, fontScale)
            val sideBySide = policy.favoriteColumns == 2 && !policy.compactHeight && maxHeight >= 480.dp
            Box(Modifier.fillMaxSize().testTag("profile-scroll")
                .ngaBackdropSource().verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 1120.dp).fillMaxWidth()
                    .padding(horizontal = margin)
                    .padding(bottom = if (hasDock) dockClearance else 24.dp)) {
                    ProfileMasthead()
                    if (sideBySide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            ProfileIdentity(display, profileLoad, { root.refreshOwnProfile() },
                                onAvatarLoadError, Modifier.weight(0.39f).heightIn(min = 260.dp))
                            ProfileEntries(nav, Modifier.weight(0.61f))
                        }
                    } else {
                        ProfileIdentity(display, profileLoad, { root.refreshOwnProfile() },
                            onAvatarLoadError, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(18.dp))
                        ProfileEntries(nav, Modifier.fillMaxWidth())
                    }
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
private fun ProfileMasthead() {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp)) {
        Text("ACCOUNT  ·  NGA QING", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("我的", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text(".", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(7.dp))
        Text("账号、阅读记录与社区消息", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val cardShape = RoundedCornerShape(16.dp)
    Column(modifier.testTag("profile-identity")
        .background(MaterialTheme.colorScheme.surface, cardShape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
        .padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text("个人空间", style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(CircleShape)
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
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
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
        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))
        Text("账号切换与退出可在设置中管理。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileEntries(nav: NavHostController, modifier: Modifier) {
    Column(modifier.testTag("profile-entries")) {
        Text("我的内容", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 11.dp))
        val cardShape = RoundedCornerShape(16.dp)
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
            .padding(horizontal = 15.dp)) {
            ProfileRow("消息与关注", "提醒、私信和关注主题", Icons.Outlined.Notifications) {
                nav.navigate(Routes.COMMUNITY)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ProfileRow("浏览历史", "接着阅读看过的帖子", Icons.Outlined.History) {
                nav.navigate(Routes.HISTORY)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("应用", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 11.dp))
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
            .padding(horizontal = 15.dp)) {
            ProfileRow("设置与阅读偏好", "账号、外观与内容过滤", Icons.Filled.Settings) {
                nav.navigate(Routes.SETTINGS)
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer,
            RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
