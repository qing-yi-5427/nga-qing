package com.qingyi5427.ngaqing.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer
import kotlinx.coroutines.delay

@Composable
fun NewTopicScreen(
    nav: NavHostController,
    viewModel: NewTopicViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val draftLoaded by viewModel.draftLoaded.collectAsStateWithLifecycle()
    val publishing by viewModel.publishing.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val ngaDomain by viewModel.ngaDomain.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var restored by remember { mutableStateOf(false) }

    LaunchedEffect(draft, draftLoaded) {
        if (!restored && draftLoaded) {
            subject = draft?.subject.orEmpty()
            content = draft?.content.orEmpty()
            restored = true
        }
    }
    LaunchedEffect(subject, content, restored) {
        if (!restored && draft == null) return@LaunchedEffect
        delay(600)
        viewModel.saveDraft(subject, content)
    }
    LaunchedEffect(result) {
        result?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeResult()
            if (it.contains("成功")) nav.popBackStack()
        }
    }

    SwipeBackContainer(onBack = { if (!publishing) nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
            AppTopBar(
                title = "在 ${viewModel.boardName} 发主题",
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }, enabled = !publishing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            nav.navigate(Routes.webNewTopicRoute(ngaDomain, viewModel.fid, viewModel.stid))
                        },
                        enabled = !publishing
                    ) { Text("图片/高级") }
                    TextButton(
                        onClick = { viewModel.publish(subject, content) },
                        enabled = !publishing && subject.isNotBlank() && content.isNotBlank()
                    ) {
                        if (publishing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("发布")
                    }
                }
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("正文") },
                    minLines = 12,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        "标题和正文会自动保存为草稿",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
