package com.qingyi5427.ngaqing.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: NgaRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(FavoriteSort.NEWEST)
    private val folder = MutableStateFlow<String?>(null)
    val syncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)
    val syncError = MutableStateFlow<String?>(null)
    val actionError = MutableStateFlow<String?>(null)
    private var pendingRetryTid: String? = null
    private var session: RequestPreferences? = null
    private var actionBusy = false
    init { viewModelScope.launch { session = repo.captureSession() } }
    val folders: StateFlow<List<String>> = repo.favorites()
        .map { items -> items.map { it.folder }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites: StateFlow<List<FavoriteEntity>> =
        combine(repo.favorites(), query, sort, folder) { items, q, order, selectedFolder ->
            items.asSequence()
                .filter { selectedFolder == null || it.folder == selectedFolder }
                .filter { q.isBlank() || it.title.contains(q, true) || it.author.contains(q, true) }
                .let { sequence ->
                    when (order) {
                        FavoriteSort.NEWEST -> sequence.sortedByDescending { it.ts }
                        FavoriteSort.TITLE -> sequence.sortedBy { it.title }
                        FavoriteSort.AUTHOR -> sequence.sortedBy { it.author }
                    }
                }.toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(value: String) { query.value = value }
    fun setSort(value: FavoriteSort) { sort.value = value }
    fun setFolder(value: String?) { folder.value = value }

    fun remove(tid: String) {
        if (actionBusy) return
        actionBusy = true
        viewModelScope.launch {
            try {
                val owner = session ?: return@launch
                if (!repo.isSessionCurrent(owner)) return@launch
                val result = repo.removeFavorite(tid, owner)
                result.onFailure {
                    if (it is CancellationException) throw it
                    pendingRetryTid = tid
                    actionError.value = it.message ?: "NGA 收藏删除失败"
                }
            } finally {
                actionBusy = false
            }
        }
    }

    fun move(tid: String, folder: String) {
        viewModelScope.launch { session?.let { repo.moveFavorite(tid, folder, it) } }
    }

    fun retryFavorite() {
        if (actionBusy) return
        val tid = pendingRetryTid ?: return
        actionBusy = true
        viewModelScope.launch {
            try {
                val owner = session ?: return@launch
                if (!repo.isSessionCurrent(owner)) return@launch
                val result = repo.retryServerFavorite(tid, add = false, session = owner)
                result.onSuccess { dismissActionError() }.onFailure {
                    if (it is CancellationException) throw it
                    actionError.value = it.message ?: "NGA 收藏删除失败"
                }
            } finally {
                actionBusy = false
            }
        }
    }

    fun dismissActionError() {
        actionError.value = null
        pendingRetryTid = null
    }

    fun syncFromServer() {
        if (syncing.value) return
        syncing.value = true
        viewModelScope.launch {
            try {
                val owner = session ?: return@launch
                if (!repo.isSessionCurrent(owner)) return@launch
                val result = repo.syncServerFavorites(owner)
                result.onSuccess {
                    syncMessage.value = if (it == 0) "已与 NGA 收藏同步" else "已从 NGA 导入 $it 个收藏"
                    syncError.value = null
                }.onFailure {
                    if (it is CancellationException) throw it
                    syncError.value = it.message ?: "请稍后重试"
                }
            } finally {
                syncing.value = false
            }
        }
    }

    fun consumeSyncMessage() { syncMessage.value = null }
    fun dismissSyncError() { syncError.value = null }
}

enum class FavoriteSort { NEWEST, TITLE, AUTHOR }
