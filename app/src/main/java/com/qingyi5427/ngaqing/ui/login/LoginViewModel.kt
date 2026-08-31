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
        viewModelScope.launch { loginHelper.syncAuthCookies() }
    }

    /**
     * Called from WebViewClient.onPageFinished. NGA QR login writes the passport
     * cookies only after the page is refreshed, so we re-check on every load.
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
     * credentials are present (e.g. the user scanned but forgot to refresh).
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
                _state.value = LoginState.Error("未检测到登录凭证：请先在网页里扫码登录，扫码成功后点右上角「刷新」，再点「完成登录」")
            }
        }
    }
}
