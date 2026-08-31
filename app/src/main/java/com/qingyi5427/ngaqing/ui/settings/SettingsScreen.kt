package com.qingyi5427.ngaqing.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer

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
        Column(Modifier.fillMaxSize()) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingSection("外观") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                            FilterChip(
                                selected = theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                SettingSection("阅读") {
                    Text("正文字号", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.9f to "小", 1f to "标准", 1.15f to "大", 1.3f to "特大").forEach { (value, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(readingTextScale - value) < 0.01f,
                                onClick = { viewModel.setReadingTextScale(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("行距", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.9f to "紧凑", 1f to "标准", 1.18f to "宽松").forEach { (value, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(readingLineSpacing - value) < 0.01f,
                                onClick = { viewModel.setReadingLineSpacing(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                    FilterChip(
                        selected = showSignatures,
                        onClick = { viewModel.setShowSignatures(!showSignatures) },
                        label = { Text(if (showSignatures) "显示用户签名" else "隐藏用户签名") }
                    )
                }
            }
            item {
                SettingSection("账号") {
                    if (accounts.size <= 1) {
                        Text(
                            "再次登录其他账号后，可在这里快速切换。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    accounts.forEach { account ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { viewModel.switchAccount(account.uid) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = account.uid == activeUid,
                                onClick = { viewModel.switchAccount(account.uid) }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(account.username.ifBlank { "NGA 用户" })
                                Text("UID ${account.uid}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.removeAccount(account.uid) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "移除账号 ${account.username}")
                            }
                        }
                    }
                    TextButton(onClick = { nav.navigate(Routes.ADD_ACCOUNT) }) { Text("添加账号") }
                }
            }
            item {
                SettingSection("访问域名") {
                    NgaDomains.options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.setNgaDomain(option.host) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ngaDomain == option.host,
                                onClick = { viewModel.setNgaDomain(option.host) }
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
                SettingSection("屏蔽用户") {
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
                    blockedUsers.forEach { value ->
                        RemovableSetting(value) { viewModel.removeBlacklistUser(value) }
                    }
                }
            }
            item {
                SettingSection("屏蔽标题关键词") {
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
                    blockedKeywords.forEach { value ->
                        RemovableSetting(value) { viewModel.removeBlacklistKeyword(value) }
                    }
                }
            }
            item {
                SettingSection("存储") {
                    Text(
                        "主题列表、帖子和搜索结果会保留 14 天，在网络不可用时自动显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::clearOfflineCache) {
                        Text("清理离线缓存")
                    }
                }
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
            title = { Text("退出登录？") },
            text = { Text("本机保存的 NGA 登录凭证会被清除，收藏仍会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                    nav.navigate(Routes.LOGIN) { popUpTo(0) }
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun RemovableSetting(value: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 12.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "移除 $value")
        }
    }
}
