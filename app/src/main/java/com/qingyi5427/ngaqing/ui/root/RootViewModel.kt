package com.qingyi5427.ngaqing.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    prefs: UserPreferences
) : ViewModel() {
    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val isLoggedIn = prefs.isLoggedIn.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val authState = prefs.isLoggedIn
        .map { loggedIn -> loggedIn as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userName = prefs.uname.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userId = prefs.uid.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val blacklistUsers = prefs.blacklistUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val blacklistKeywords = prefs.blacklistKeywords.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val readingTextScale = prefs.readingTextScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val readingLineSpacing = prefs.readingLineSpacing.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val showSignatures = prefs.showSignatures.stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
