package com.ngaclient.app

import android.os.Build
import android.os.Bundle
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.ngaclient.app.data.remote.LoginHelper
import com.ngaclient.app.ui.NgaNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var loginHelper: LoginHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 杀进程重进后，把 DataStore 里的登录凭证同步回 WebView CookieManager，
        // 否则帖子里的图片请求会因为带不上 passport cookie 而裂图。
        lifecycleScope.launch {
            runCatching { loginHelper.syncAuthCookies() }
        }
        // 预测性返回 + 全面屏（edge-to-edge）：系统驱动滑动返回手势，NavHost 自动联动
        enableEdgeToEdge()
        requestMaxRefreshRate()
        setContent {
            NgaNavHost()
        }
    }

    /** 显式选用屏幕支持的最高刷新率（如 120Hz），否则部分机型会把第三方应用限制在 60Hz。 */
    private fun requestMaxRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val displayManager = getSystemService(DisplayManager::class.java) ?: return
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val currentMode = display.mode
        // 只在当前物理分辨率下选最高刷新率，避免某些设备为追求 Hz 意外切换分辨率。
        val modes = display.supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth &&
                it.physicalHeight == currentMode.physicalHeight
        }
        if (modes.isEmpty()) return
        val best = modes.maxByOrNull { it.refreshRate } ?: return
        val params = window.attributes
        params.preferredDisplayModeId = best.modeId
        window.attributes = params
    }
}
