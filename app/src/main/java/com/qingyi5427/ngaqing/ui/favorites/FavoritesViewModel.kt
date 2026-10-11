package com.qingyi5427.ngaqing.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.data.repository.ServerFavoriteSelection
import com.qingyi5427.ngaqing.ui.post.FavoriteFolderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val repo: NgaRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(FavoriteSort.NEWEST)
    private val folder = MutableStateFlow<String?>(null)
    val syncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)
    val syncError = MutableStateFlow<String?>(null)
    val actionError = MutableStateFlow<String?>(null)
    val manageFavorite = MutableStateFlow<FavoriteEntity?>(null)
    val manageState = MutableStateFlow<FavoriteFolderUiState>(FavoriteFolderUiState.Closed)
    val actionBusy = MutableStateFlow(false)
    private var manageJob: Job? = null
    private var manageGeneration = 0L
    private var manageSession: RequestPreferences? = null

    /** These are local labels, not the NGA account's cloud folders. */
    val folders: StateFlow<List<String>> = repo.favorites()
        .map { items -> items.map { it.folder }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val visitedTids: StateFlow<Set<String>> = repo.history()
        .map { items -> items.mapTo(mutableSetOf()) { it.tid } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
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

    fun openManage(favorite: FavoriteEntity) {
        if (actionBusy.value || manageState.value == FavoriteFolderUiState.Loading) return
        manageSession = null
        manageFavorite.value = favorite
        manageState.value = FavoriteFolderUiState.Loading
        val generation = ++manageGeneration
        manageJob = viewModelScope.launch {
            val owner = repo.captureSession()
            if (!repo.isSessionCurrent(owner)) {
                if (generation == manageGeneration) manageState.value = FavoriteFolderUiState.Closed
                return@launch
            }
            if (favorite.ownerUid != owner.uid.ifBlank { "__guest__" }) {
                if (generation == manageGeneration) manageState.value = FavoriteFolderUiState.Error("账号已切换，请重新打开收藏")
                return@launch
            }
            val result = if (owner.uid.isBlank()) {
                Result.success(ServerFavoriteSelection(emptyList(), emptySet(), favorite.source == "local"))
            } else repo.loadServerFavoriteSelection(favorite.tid, owner)
            if (generation != manageGeneration) return@launch
            if (!repo.isSessionCurrent(owner)) {
                manageState.value = FavoriteFolderUiState.Closed
                return@launch
            }
            manageSession = owner
            manageState.value = result.fold(
                onSuccess = { FavoriteFolderUiState.Ready(it) },
                onFailure = { FavoriteFolderUiState.Error(it.message ?: "NGA 收藏文件夹读取失败") }
            )
        }
    }

    fun dismissManage() {
        if (actionBusy.value) return
        manageGeneration++
        manageJob?.cancel()
        manageSession = null
        manageFavorite.value = null
        manageState.value = FavoriteFolderUiState.Closed
    }

    fun addServerFolder(folderId: String) = mutateServerFolder(folderId, add = true)

    fun removeServerFolder(folderId: String) = mutateServerFolder(folderId, add = false)

    private fun mutateServerFolder(folderId: String, add: Boolean) {
        if (actionBusy.value) return
        val favorite = manageFavorite.value ?: return
        val selection = (manageState.value as? FavoriteFolderUiState.Ready)?.selection ?: return
        val owner = manageSession ?: return
        if (add == (folderId in selection.selectedIds)) return
        actionBusy.value = true
        viewModelScope.launch {
            try {
                if (!repo.isSessionCurrent(owner)) return@launch
                val result = if (add) repo.addFavorite(
                    favorite.tid, favorite.title, favorite.fid, favorite.author,
                    folderId, selection, owner
                ) else repo.removeFavorite(favorite.tid, folderId, selection, owner)
                if (!repo.isSessionCurrent(owner)) return@launch
                result.onSuccess { confirmed ->
                    manageState.value = FavoriteFolderUiState.Ready(confirmed)
                }.onFailure {
                    actionError.value = it.message ?: "NGA 收藏操作未确认"
                    manageState.value = FavoriteFolderUiState.Error("操作未确认，请重新读取 NGA 文件夹")
                }
            } finally {
                actionBusy.value = false
                if (!repo.isSessionCurrent(owner)) dismissManage()
            }
        }
    }

    fun removeLocalCopy() {
        if (actionBusy.value) return
        val favorite = manageFavorite.value ?: return
        val selection = (manageState.value as? FavoriteFolderUiState.Ready)?.selection ?: return
        val owner = manageSession ?: return
        if (!selection.hasLocalCopy) return
        actionBusy.value = true
        viewModelScope.launch {
            try {
                if (!repo.isSessionCurrent(owner)) return@launch
                repo.removeLocalFavorite(favorite.tid, owner, selection)
                if (repo.isSessionCurrent(owner)) {
                    manageState.value = FavoriteFolderUiState.Ready(selection.copy(hasLocalCopy = false))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                actionError.value = error.message ?: "本地收藏删除失败"
            } finally {
                actionBusy.value = false
                if (!repo.isSessionCurrent(owner)) dismissManage()
            }
        }
    }

    fun move(favorite: FavoriteEntity, folder: String) {
        viewModelScope.launch {
            val owner = repo.captureSession()
            if (favorite.ownerUid == owner.uid.ifBlank { "__guest__" } && repo.isSessionCurrent(owner)) {
                repo.moveFavorite(favorite.tid, folder, owner)
            }
        }
    }

    fun dismissActionError() { actionError.value = null }

    fun syncFromServer() {
        if (syncing.value) return
        syncing.value = true
        viewModelScope.launch {
            try {
                val owner = repo.captureSession()
                if (!repo.isSessionCurrent(owner)) return@launch
                val result = repo.syncServerFavorites(owner)
                if (!repo.isSessionCurrent(owner)) return@launch
                result.onSuccess {
                    syncMessage.value = if (it == 0) "已与 NGA 收藏同步" else "已从 NGA 导入 $it 个收藏"
                    syncError.value = null
                }.onFailure {
                    if (it is CancellationException) throw it
                    syncError.value = it.message ?: "请稍后重试"
                }
            } finally { syncing.value = false }
        }
    }

    fun consumeSyncMessage() { syncMessage.value = null }
    fun dismissSyncError() { syncError.value = null }
}

enum class FavoriteSort { NEWEST, TITLE, AUTHOR }
