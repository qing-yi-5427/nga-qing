package com.ngaclient.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ngaclient.app.ui.gesture.SwipeBackContainer
import com.ngaclient.app.ui.theme.NgaQingTheme

/** 仅供真机测试 WebView 与 Compose 手势事件分发，不进入 Release 包。 */
class WebSwipeInteropTestActivity : ComponentActivity() {
    companion object {
        const val ACTION_SWIPE = "com.ngaclient.app.debug.TEST_WEB_SWIPE"
        const val EXTRA_MODE = "mode"
        private const val TAG = "WebSwipeInteropTest"
    }

    lateinit var webView: WebView
    var backCount = 0
    private val swipeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mode = intent?.getStringExtra(EXTRA_MODE).orEmpty()
            val before = webView.scrollY
            when (mode) {
                "vertical" -> drag(600f, 2100f, 600f, 450f)
                "diagonal" -> drag(450f, 2100f, 700f, 500f)
                "horizontal" -> drag(350f, 1500f, 980f, 1450f)
                else -> return
            }
            webView.postDelayed({
                Log.i(
                    TAG,
                    "mode=$mode before=$before after=${webView.scrollY} backCount=$backCount"
                )
            }, 350)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            swipeReceiver,
            IntentFilter(ACTION_SWIPE),
            ContextCompat.RECEIVER_EXPORTED
        )
        setContent {
            NgaQingTheme(themeMode = "light") {
                SwipeBackContainer(
                    onBack = { backCount++ },
                    contentSwipeEnabled = false
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).also { view ->
                                webView = view
                                view.settings.javaScriptEnabled = false
                                val rows = buildString {
                                    append("<html><body style='font-size:32px'>")
                                    repeat(240) { append("<p>WebView scroll row $it</p>") }
                                    append("</body></html>")
                                }
                                view.loadDataWithBaseURL(null, rows, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(swipeReceiver)
        super.onDestroy()
    }

    private fun drag(startX: Float, startY: Float, endX: Float, endY: Float) {
        val target = window.decorView
        val downTime = SystemClock.uptimeMillis()
        target.dispatchTouchEvent(
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        )
        repeat(16) { index ->
            val fraction = (index + 1) / 16f
            target.dispatchTouchEvent(
                MotionEvent.obtain(
                    downTime,
                    downTime + (index + 1) * 10L,
                    MotionEvent.ACTION_MOVE,
                    startX + (endX - startX) * fraction,
                    startY + (endY - startY) * fraction,
                    0
                )
            )
        }
        target.dispatchTouchEvent(
            MotionEvent.obtain(downTime, downTime + 180L, MotionEvent.ACTION_UP, endX, endY, 0)
        )
    }
}
