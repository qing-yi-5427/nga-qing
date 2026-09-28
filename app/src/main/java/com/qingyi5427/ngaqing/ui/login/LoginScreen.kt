package com.qingyi5427.ngaqing.ui.login

import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
import com.qingyi5427.ngaqing.ui.design.NgaStatePanel
import com.qingyi5427.ngaqing.data.local.NgaDomains

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    nav: NavHostController,
    addingAccount: Boolean = false,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ngaDomain by viewModel.ngaDomain.collectAsStateWithLifecycle()
    val webReady by viewModel.webReady.collectAsStateWithLifecycle()
    var loading by remember { mutableStateOf(true) }
    var webError by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(addingAccount) { viewModel.prepareLogin(addingAccount) }
    DisposableEffect(addingAccount) {
        onDispose {
            if (addingAccount) viewModel.restoreSavedCookies()
        }
    }

    // enable cookie persistence for the WebView
    CookieManager.getInstance().setAcceptCookie(true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = if (addingAccount) "添加 NGA 账号" else "登录 NGA",
                navigationIcon = if (addingAccount) {
                    {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                } else null,
                actions = {
                    IconButton(onClick = { helpOpen = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "登录帮助")
                    }
                    // NGA QR login only writes the passport cookies after a manual
                    // page refresh, so we expose a refresh button here.
                    IconButton(onClick = {
                        webError = false
                        loading = true
                        webView?.reload()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新页面")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).imePadding()) {
            // Give the website one bounded, flexible viewport. Its own scrolling and
            // zoom handle the QR dialog; the native action remains outside the WebView.
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics { contentDescription = "NGA 登录网页区域" }
            ) {
                    if (webReady) androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                // NGA's fixed-width login dialog needs the page's wide
                                // viewport fitted to this WebView, not a cropped 100% scale.
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?, url: String?, favicon: android.graphics.Bitmap?
                                    ) {
                                        webError = false
                                        loading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        loading = false
                                        // Re-check cookies after every load — this is how
                                        // we pick up the passport cookies NGA sets post-refresh.
                                        if (!webError) viewModel.onPageFinished(url)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            webError = true
                                            loading = false
                                        }
                                    }
                                }
                            }.also { webView = it }
                        },
                        update = { view ->
                            if (view.tag != ngaDomain) {
                                view.tag = ngaDomain
                                loading = true
                                webError = false
                                view.loadUrl(NgaDomains.url(ngaDomain))
                            }
                        },
                        onRelease = { view ->
                            webView = null
                            view.stopLoading()
                            view.destroy()
                        }
                    )
                    if (webError) {
                        NgaStatePanel(
                            title = "登录页面加载失败",
                            actionLabel = "重试",
                            onAction = {
                                webError = false
                                loading = true
                                webView?.reload()
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        )
                    } else if (loading || !webReady) {
                        Column(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                if (webReady) "正在载入登录页面" else "正在准备登录环境",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
            }
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.tryCapture() },
                        enabled = state !is LoginState.Checking && webReady,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (state is LoginState.Checking) "正在检测" else "完成登录")
                    }
                    if (state is LoginState.Error) {
                        Text(
                            (state as LoginState.Error).msg,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
        }
    }
    if (helpOpen) {
        AlertDialog(
            onDismissRequest = { helpOpen = false },
            title = { Text("如何登录") },
            text = { Text("请在 NGA 网页完成扫码或账号登录。扫码后点右上角刷新，再点“完成登录”检测状态。网页内容可以双指缩放。") },
            confirmButton = { TextButton(onClick = { helpOpen = false }) { Text("知道了") } }
        )
    }
}
