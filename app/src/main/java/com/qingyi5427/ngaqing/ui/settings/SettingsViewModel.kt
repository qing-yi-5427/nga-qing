package com.qingyi5427.ngaqing.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.remote.LoginHelper
import com.qingyi5427.ngaqing.data.repository.NgaRepository
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
    private val loginHelper: LoginHelper,
    private val repo: NgaRepository
) : ViewModel() {

    val themeMode: StateFlow<String> =
        prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val ngaDomain: StateFlow<String> =
        prefs.ngaDomain.stateIn(viewModelScope, SharingStarted.Eagerly, NgaDomains.DEFAULT_HOST)
    val blacklistUsers: StateFlow<Set<String>> =
        prefs.blacklistUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val blacklistKeywords: StateFlow<Set<String>> =
        prefs.blacklistKeywords.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val readingTextScale: StateFlow<Float> =
        prefs.readingTextScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val readingLineSpacing: StateFlow<Float> =
        prefs.readingLineSpacing.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val showSignatures: StateFlow<Boolean> =
        prefs.showSignatures.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val accounts = prefs.accounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeUid = prefs.uid.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setTheme(mode: String) {
        viewModelScope.launch { prefs.setTheme(mode) }
    }

    fun setReadingTextScale(value: Float) {
        viewModelScope.launch { prefs.setReadingTextScale(value) }
    }

    fun setReadingLineSpacing(value: Float) {
        viewModelScope.launch { prefs.setReadingLineSpacing(value) }
    }

    fun setShowSignatures(value: Boolean) {
        viewModelScope.launch { prefs.setShowSignatures(value) }
    }

    fun clearOfflineCache() {
        viewModelScope.launch { repo.clearResponseCache() }
    }

    fun switchAccount(uid: String) {
        viewModelScope.launch {
            if (prefs.switchAccount(uid)) loginHelper.syncAuthCookies()
        }
    }

    fun removeAccount(uid: String) {
        viewModelScope.launch {
            val wasActive = prefs.uid.first() == uid
            prefs.removeAccount(uid)
            if (wasActive) {
                val next = prefs.accounts.first().firstOrNull()
                if (next != null) {
                    prefs.switchAccount(next.uid)
                    loginHelper.syncAuthCookies()
                } else {
                    loginHelper.clearAuth()
                }
            }
        }
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
