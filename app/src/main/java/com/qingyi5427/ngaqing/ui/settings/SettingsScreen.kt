package com.qingyi5427.ngaqing.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(nav: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val ngaDomain by viewModel.ngaDomain.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blacklistUsers.collectAsStateWithLifecycle()
    val blockedKeywords by viewModel.blacklistKeywords.collectAsStateWithLifecycle()
    val readingTextScale by viewModel.readingTextScale.collectAsStateWithLifecycle()
    val readingLineSpacing by viewModel.readingLineSpacing.collectAsStateWithLifecycle()
    val showSignatures by viewModel.showSignatures.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeUid by viewModel.activeUid.collectAsStateWithLifecycle()
    var userInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var confirmLogout by remember { mutableStateOf(false) }

    SwipeBackContainer(onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .navigationBarsPadding().imePadding()) {
        AppTopBar(
            title = "设置",
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingSection("外观", "主题 · ${when (theme) { "light" -> "浅色"; "dark" -> "深色"; else -> "跟随系统" }}") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                            FilterChip(
                                selected = theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                SettingSection("阅读", "字号、行距和签名") {
                    Text("正文字号", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.9f to "小", 1f to "标准", 1.15f to "大", 1.3f to "特大").forEach { (value, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(readingTextScale - value) < 0.01f,
                                onClick = { viewModel.setReadingTextScale(value) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("行距", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.9f to "紧凑", 1f to "标准", 1.18f to "宽松").forEach { (value, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(readingLineSpacing - value) < 0.01f,
                                onClick = { viewModel.setReadingLineSpacing(value) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(label) }
                            )
                        }
                    }
                    FilterChip(
                        selected = showSignatures,
                        onClick = { viewModel.setShowSignatures(!showSignatures) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(if (showSignatures) "显示用户签名" else "隐藏用户签名") }
                    )
                }
            }
            item {
                SettingSection("账号", "${accounts.size} 个已保存账号") {
                    if (accounts.size <= 1) {
                        Text(
                            "再次登录其他账号后，可在这里快速切换。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    accounts.forEach { account ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .clickable { viewModel.switchAccount(account.uid) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = account.uid == activeUid,
                                onClick = null
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(account.username.ifBlank { "NGA 用户" }, style = MaterialTheme.typography.bodyLarge)
                                Text("UID ${account.uid}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.removeAccount(account.uid) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "移除账号 ${account.username}")
                            }
                        }
                    }
                    TextButton(
                        onClick = { nav.navigate(Routes.ADD_ACCOUNT) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("添加账号") }
                }
            }
            item {
                SettingSection("访问域名", NgaDomains.options.firstOrNull { it.host == ngaDomain }?.label ?: ngaDomain) {
                    NgaDomains.options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .clickable { viewModel.setNgaDomain(option.host) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ngaDomain == option.host,
                                onClick = null
                            )
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        "切换后，新请求、登录页和网页版帖子会统一使用该域名。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SettingSection("屏蔽用户", "${blockedUsers.size} 位用户") {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("输入用户名") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (userInput.isNotBlank()) {
                                viewModel.addBlacklistUser(userInput.trim())
                                userInput = ""
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                viewModel.addBlacklistUser(userInput.trim())
                                userInput = ""
                            }
                        },
                        enabled = userInput.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("添加屏蔽用户") }
                    if (blockedUsers.isEmpty()) Text(
                        "尚未屏蔽用户", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    blockedUsers.forEach { value ->
                        RemovableSetting(value) { viewModel.removeBlacklistUser(value) }
                    }
                }
            }
            item {
                SettingSection("屏蔽标题关键词", "${blockedKeywords.size} 个关键词") {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = { Text("输入关键词") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (keywordInput.isNotBlank()) {
                                viewModel.addBlacklistKeyword(keywordInput.trim())
                                keywordInput = ""
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            if (keywordInput.isNotBlank()) {
                                viewModel.addBlacklistKeyword(keywordInput.trim())
                                keywordInput = ""
                            }
                        },
                        enabled = keywordInput.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("添加关键词") }
                    if (blockedKeywords.isEmpty()) Text(
                        "尚未屏蔽标题关键词", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    blockedKeywords.forEach { value ->
                        RemovableSetting(value) { viewModel.removeBlacklistKeyword(value) }
                    }
                }
            }
            item {
                SettingSection("存储", "离线缓存") {
                    Text(
                        "主题列表、帖子和搜索结果会保留 14 天，在网络不可用时自动显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::clearOfflineCache, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("清理离线缓存")
                    }
                }
            }
            item {
                TextButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)
                ) {
                    Text("退出登录", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("退出登录？") },
            text = { Text("本机保存的 NGA 登录凭证会被清除，收藏仍会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingSection(title: String, summary: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun RemovableSetting(value: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "移除 $value")
        }
    }
}
