package com.qingyi5427.ngaqing.ui.login

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.chrome.AppTopBar
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
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(addingAccount) {
        viewModel.prepareLogin(addingAccount)
    }
    DisposableEffect(addingAccount) {
        onDispose {
            if (addingAccount) viewModel.restoreSavedCookies()
        }
    }
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            if (addingAccount) nav.popBackStack()
            else nav.navigate(Routes.BOARDS) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    // enable cookie persistence for the WebView
    CookieManager.getInstance().setAcceptCookie(true)

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (addingAccount) "添加 NGA 账号" else "登录 NGA",
                navigationIcon = {
                    if (addingAccount) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    // NGA QR login only writes the passport cookies after a manual
                    // page refresh, so we expose a refresh button here.
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新页面")
                    }
                }
            )
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (webReady) androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        loading = false
                                        // Re-check cookies after every load — this is how
                                        // we pick up the passport cookies NGA sets post-refresh.
                                        viewModel.onPageFinished(url)
                                    }
                                }
                            }.also { webView = it }
                        },
                        update = { view ->
                            if (view.tag != ngaDomain) {
                                view.tag = ngaDomain
                                loading = true
                                view.loadUrl(NgaDomains.url(ngaDomain))
                            }
                        },
                        onRelease = { view ->
                            webView = null
                            view.stopLoading()
                            view.destroy()
                        }
                    )
                    if (loading || !webReady) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "使用 NGA App 扫码。成功后点右上角刷新，检测到登录状态会自动进入。",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.tryCapture() }) {
                        Text("完成登录")
                    }
                    if (state is LoginState.Error) {
                        Text(
                            (state as LoginState.Error).msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
