package com.qingyi5427.ngaqing.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.LoginHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import android.util.Log
import javax.inject.Inject

sealed interface LoginState {
    data object Idle : LoginState
    data object Checking : LoginState
    data object Success : LoginState
    data class Error(val msg: String) : LoginState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginHelper: LoginHelper,
    prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()
    val ngaDomain: StateFlow<String> =
        prefs.ngaDomain.stateIn(viewModelScope, SharingStarted.Eagerly, NgaDomains.DEFAULT_HOST)

    // Guards against re-capturing / re-navigating after a successful login.
    private var captured = false
    private val _webReady = MutableStateFlow(false)
    val webReady: StateFlow<Boolean> = _webReady.asStateFlow()
    private var loginPrepared = false

    fun prepareLogin(addingAccount: Boolean) {
        if (loginPrepared) return
        loginPrepared = true
        if (!addingAccount) {
            _webReady.value = true
        } else {
            viewModelScope.launch {
                loginHelper.prepareAdditionalLogin()
                _webReady.value = true
            }
        }
    }

    fun restoreSavedCookies() {
        restoreScope.launch {
            try {
                loginHelper.syncAuthCookies()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NgaLogin", "恢复账号 Cookie 失败", e)
            }
        }
    }

    /**
     * Called from WebViewClient.onPageFinished. The NGA page may write passport
     * cookies after a refresh, so we re-check on every load.
     * Auto-navigates to boards once credentials appear. Does NOT surface an error
     * here (the user may simply not be logged in yet on this particular load).
     */
    fun onPageFinished(url: String?) {
        if (captured) return
        viewModelScope.launch {
            val ok = loginHelper.captureAndSave(listOfNotNull(url))
            if (ok) {
                captured = true
                _state.value = LoginState.Success
            }
        }
    }

    /**
     * Manual fallback triggered by the "完成登录" button. Surfaces an error if no
     * credentials are present (e.g. the user completed the site's login flow but
     * the WebView has not received the resulting cookies yet).
     */
    fun tryCapture() {
        if (captured) return
        _state.value = LoginState.Checking
        viewModelScope.launch {
            val ok = loginHelper.captureAndSave()
            if (ok) {
                captured = true
                _state.value = LoginState.Success
            } else {
                _state.value = LoginState.Error("未检测到登录凭证。请完成网页的账号或验证流程；也可让另一设备扫描本页二维码。扫码后若未生效，请刷新页面再检测。")
            }
        }
    }

    private companion object {
        // A disposed add-account screen still has to restore the saved account's cookies.
        val restoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
