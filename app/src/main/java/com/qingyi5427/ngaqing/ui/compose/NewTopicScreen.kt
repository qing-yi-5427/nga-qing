package com.qingyi5427.ngaqing.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaOutlinedCard
import com.qingyi5427.ngaqing.ui.design.NgaGlassSurface
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.gesture.SwipeBackContainer

@Composable
fun NewTopicScreen(
    nav: NavHostController,
    viewModel: NewTopicViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val draftLoaded by viewModel.draftLoaded.collectAsStateWithLifecycle()
    val publishing by viewModel.publishing.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val publishSucceeded by viewModel.publishSucceeded.collectAsStateWithLifecycle()
    val ngaDomain by viewModel.ngaDomain.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var subject by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var restored by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(draft, draftLoaded) {
        if (!restored && draftLoaded) {
            subject = draft?.subject.orEmpty()
            content = draft?.content.orEmpty()
            restored = true
        }
    }
    val latestSubject by rememberUpdatedState(subject)
    val latestContent by rememberUpdatedState(content)
    val latestRestored by rememberUpdatedState(restored)
    val latestSucceeded by rememberUpdatedState(publishSucceeded)
    DisposableEffect(Unit) {
        onDispose {
            if (latestRestored && !latestSucceeded) {
                viewModel.saveDraft(latestSubject, latestContent, immediate = true)
            }
        }
    }
    LaunchedEffect(result, publishSucceeded) {
        result?.let {
            if (publishSucceeded) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                nav.popBackStack()
            } else submitError = it
            viewModel.consumeResult()
        }
    }

    SwipeBackContainer(onBack = { if (!publishing) nav.popBackStack() }) {
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .imePadding().navigationBarsPadding()
        ) {
            AppTopBar(
                title = "新主题",
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }, enabled = !publishing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "发布到 ${viewModel.boardName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!draftLoaded) Text(
                    "正在载入草稿…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                submitError?.let { message ->
                    NgaOutlinedCard(Modifier.fillMaxWidth()) {
                        Text("发布未完成", style = MaterialTheme.typography.titleMedium)
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                BasicTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        viewModel.saveDraft(it, content)
                    },
                    singleLine = true,
                    enabled = draftLoaded && !publishing,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .semantics { contentDescription = "标题" }
                        .padding(vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (subject.isEmpty()) Text("标题", style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BasicTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        viewModel.saveDraft(subject, it)
                    },
                    enabled = draftLoaded && !publishing,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp)
                        .semantics { contentDescription = "正文" }
                        .padding(vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (content.isEmpty()) Text("正文", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        if (publishing) "正在提交，请稍候" else "标题和正文会自动保存为草稿",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            NgaGlassSurface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        nav.navigate(Routes.webNewTopicRoute(ngaDomain, viewModel.fid, viewModel.stid))
                    },
                    enabled = !publishing,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("图片/高级") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        submitError = null
                        viewModel.publish(subject, content)
                    },
                    enabled = draftLoaded && !publishing && subject.isNotBlank() && content.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (publishing) CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp
                    ) else Text("发布")
                }
            }
            }
        }
    }
}
