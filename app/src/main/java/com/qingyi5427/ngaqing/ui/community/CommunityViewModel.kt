package com.qingyi5427.ngaqing.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.WatchedThreadEntity
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

sealed interface CommunityState {
    data object Loading : CommunityState
    data class Content(val items: List<CommunityItem>) : CommunityState
    data class Error(val message: String) : CommunityState
}

enum class CommunityTab { NOTIFICATIONS, MESSAGES, WATCHING }

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repo: NgaRepository
) : ViewModel() {
    private val _tab = MutableStateFlow(CommunityTab.NOTIFICATIONS)
    val tab: StateFlow<CommunityTab> = _tab.asStateFlow()
    private val _state = MutableStateFlow<CommunityState>(CommunityState.Loading)
    val state: StateFlow<CommunityState> = _state.asStateFlow()
    val watched = repo.watchedThreads().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    private var requestJob: Job? = null
    private var generation = 0L
    private var session: RequestPreferences? = null

    init {
        viewModelScope.launch { session = repo.captureSession() }
        refresh()
    }

    fun select(tab: CommunityTab) {
        requestJob?.cancel()
        generation++
        _tab.value = tab
        if (tab != CommunityTab.WATCHING) refresh()
    }

    fun refresh() {
        if (_tab.value == CommunityTab.WATCHING) return
        requestJob?.cancel()
        val request = ++generation
        val selectedTab = _tab.value
        _state.value = CommunityState.Loading
        requestJob = viewModelScope.launch {
            val result = if (selectedTab == CommunityTab.NOTIFICATIONS) {
                repo.notifications()
            } else {
                repo.privateMessages()
            }
            if (request != generation) return@launch
            _state.value = result.fold(
                onSuccess = { CommunityState.Content(it) },
                onFailure = { CommunityState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun unwatch(item: WatchedThreadEntity) {
        viewModelScope.launch { session?.let { repo.unwatchThread(item.tid, it) } }
    }
}
