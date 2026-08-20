package com.ngaclient.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngaclient.app.data.local.UserPreferences
import com.ngaclient.app.data.local.NgaDomains
import com.ngaclient.app.data.remote.LoginHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val loginHelper: LoginHelper
) : ViewModel() {

    val themeMode: StateFlow<String> =
        prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val ngaDomain: StateFlow<String> =
        prefs.ngaDomain.stateIn(viewModelScope, SharingStarted.Eagerly, NgaDomains.DEFAULT_HOST)
    val blacklistUsers: StateFlow<Set<String>> =
        prefs.blacklistUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val blacklistKeywords: StateFlow<Set<String>> =
        prefs.blacklistKeywords.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setTheme(mode: String) {
        viewModelScope.launch { prefs.setTheme(mode) }
    }

    fun setNgaDomain(host: String) {
        viewModelScope.launch {
            prefs.setNgaDomain(host)
            loginHelper.syncAuthCookies()
        }
    }

    fun setBlacklistUsers(set: Set<String>) {
        viewModelScope.launch { prefs.setBlacklistUsers(set) }
    }

    fun setBlacklistKeywords(set: Set<String>) {
        viewModelScope.launch { prefs.setBlacklistKeywords(set) }
    }

    fun addBlacklistUser(name: String) {
        viewModelScope.launch {
            val cur = prefs.blacklistUsers.first()
            prefs.setBlacklistUsers(cur + name)
        }
    }

    fun removeBlacklistUser(name: String) {
        viewModelScope.launch {
            prefs.setBlacklistUsers(prefs.blacklistUsers.first() - name)
        }
    }

    fun addBlacklistKeyword(kw: String) {
        viewModelScope.launch {
            prefs.setBlacklistKeywords(prefs.blacklistKeywords.first() + kw)
        }
    }

    fun removeBlacklistKeyword(kw: String) {
        viewModelScope.launch {
            prefs.setBlacklistKeywords(prefs.blacklistKeywords.first() - kw)
        }
    }

    fun logout() {
        viewModelScope.launch { loginHelper.clearAuth() }
    }
}
