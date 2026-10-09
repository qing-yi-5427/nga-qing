package com.qingyi5427.ngaqing.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.AccountDisplay
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val repo: NgaRepository
) : ViewModel() {
    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val isLoggedIn = prefs.isLoggedIn.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val authState = prefs.isLoggedIn
        .map { loggedIn -> loggedIn as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val authSession = prefs.session
        .map { it as RequestPreferences? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userName = prefs.uname.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userId = prefs.uid.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val accountDisplay = prefs.accountDisplay
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountDisplay())
    private val _profileLoad = MutableStateFlow(ProfileLoadState())
    val profileLoad = _profileLoad.asStateFlow()
    private var profileJob: Job? = null
    private var profileRequest: Pair<String, Long>? = null

    /** Called when My Profile is shown, including for accounts saved before profile support. */
    fun refreshOwnProfile(): Job {
        val visibleRequest = accountDisplay.value.let { it.uid to it.revision }
        if (profileRequest == visibleRequest && profileJob?.isActive == true) return profileJob!!
        val previousJob = profileJob
        profileJob = viewModelScope.launch {
            val session = prefs.requestPreferences()
            if (session.uid.isBlank() || session.cid.isBlank()) return@launch
            val request = session.uid to session.revision
            if (profileRequest == request && previousJob?.isActive == true) {
                profileJob = previousJob
                return@launch
            }
            previousJob?.cancel()
            profileRequest = request
            _profileLoad.value = ProfileLoadState(session.uid, session.revision, loading = true)
            try {
                val result = repo.userProfile(session.uid, session)
                val profile = result.getOrElse { error ->
                    if (error is CancellationException) throw error
                    if (profileRequest == request && repo.isSessionCurrent(session)) {
                        _profileLoad.value = ProfileLoadState(session.uid, session.revision,
                            error = "暂时无法更新用户资料")
                    }
                    return@launch
                }
                if (profile.uid != session.uid) {
                    if (profileRequest == request && repo.isSessionCurrent(session)) {
                        _profileLoad.value = ProfileLoadState(session.uid, session.revision,
                            error = "用户资料与当前账号不匹配")
                    }
                    return@launch
                }
                val saved = prefs.saveProfileIfCurrent(session, profile.uid, profile.username, profile.avatar)
                if (profileRequest == request && saved && repo.isSessionCurrent(session)) {
                    _profileLoad.value = ProfileLoadState(session.uid, session.revision,
                        error = if (profile.username.isBlank() || profile.username == session.uid)
                            "暂时未获取到用户名" else null)
                } else if (profileRequest == request &&
                    _profileLoad.value.uid == session.uid &&
                    _profileLoad.value.revision == session.revision) {
                    _profileLoad.value = ProfileLoadState(session.uid, session.revision)
                }
            } catch (error: CancellationException) {
                throw error
            }
        }
        return profileJob!!
    }
    val blacklistUsers = prefs.blacklistUsers.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val blacklistKeywords = prefs.blacklistKeywords.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val readingTextScale = prefs.readingTextScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val readingLineSpacing = prefs.readingLineSpacing.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val showSignatures = prefs.showSignatures.stateIn(viewModelScope, SharingStarted.Eagerly, true)
}

data class ProfileLoadState(
    val uid: String = "",
    val revision: Long = 0L,
    val loading: Boolean = false,
    val error: String? = null
)
