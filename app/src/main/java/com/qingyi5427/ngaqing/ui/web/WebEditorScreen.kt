package com.qingyi5427.ngaqing.ui.web

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel

/**
 * NGA 网页高级编辑器。原生编辑器负责快速纯文本发布，这里补齐站点本身支持的
 * 图片选择/上传、表情、附件及复杂排版，同时复用应用登录 Cookie，不跳出浏览器。
 */
@Composable
fun WebEditorScreen(
    nav: NavHostController,
    viewModel: WebEditorViewModel = hiltViewModel()
) {
    val cookiesReady by viewModel.cookiesReady.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pageLoading by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val selected = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else null
        fileCallback?.onReceiveValue(selected)
        fileCallback = null
    }

    fun goBack() {
        val web = webView
        if (web?.canGoBack() == true) web.goBack() else nav.popBackStack()
    }
    BackHandler(onBack = ::goBack)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .navigationBarsPadding().imePadding()) {
        AppTopBar(
            title = viewModel.title,
            navigationIcon = {
                IconButton(onClick = ::goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    pageError = false
                    pageLoading = true
                    webView?.reload()
                }, enabled = cookiesReady && webView != null) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新网页编辑器")
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NGA 网页编辑器", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text(
                when {
                    !cookiesReady -> "同步账号中"
                    pageError -> "连接失败"
                    pageLoading -> "加载中"
                    else -> "已连接"
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                color = if (pageError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (!cookiesReady) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("正在同步登录状态", modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(Modifier.fillMaxSize()) {
              AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                pageLoading = true
                                pageError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    pageLoading = false
                                    pageError = true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                view: WebView?,
                                callback: ValueCallback<Array<Uri>>?,
                                params: FileChooserParams?
                            ): Boolean {
                                fileCallback?.onReceiveValue(null)
                                fileCallback = callback
                                val intent = runCatching { params?.createIntent() }
                                    .getOrNull() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }
                                return runCatching {
                                    filePicker.launch(intent)
                                    true
                                }.getOrElse {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = null
                                    false
                                }
                            }
                        }
                        loadUrl(viewModel.url)
                        webView = this
                    }
                },
                update = { webView = it }
              )
              if (pageLoading && !pageError) {
                  Column(
                      Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                  ) {
                      CircularProgressIndicator()
                      Text("正在打开网页编辑器", modifier = Modifier.padding(top = 16.dp),
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
              }
              if (pageError) {
                  WebEditorErrorPanel(onRetry = {
                      pageError = false
                      pageLoading = true
                      webView?.reload()
                  })
              }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fileCallback?.onReceiveValue(null)
            webView?.stopLoading()
            webView?.destroy()
        }
    }
}

/** Shared by the live WebView error path and its offline render test. */
@Composable
internal fun WebEditorErrorPanel(onRetry: () -> Unit) {
    NgaStatePanel(
        title = "网页编辑器加载失败",
        actionLabel = "重试",
        onAction = onRetry,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    )
}
