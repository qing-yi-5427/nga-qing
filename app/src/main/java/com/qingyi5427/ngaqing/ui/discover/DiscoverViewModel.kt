package com.qingyi5427.ngaqing.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiscoverState {
    data object Loading : DiscoverState
    data class Ready(
        val boards: List<Board>,
        val discussionBoard: Board?,
        val discussions: List<ThreadItem>,
        val discussionError: String? = null,
        val boardError: String? = null,
        val fromCache: Boolean = false,
        val cachedAt: Long = 0L
    ) : DiscoverState
    data class Error(val message: String) : DiscoverState
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(private val repo: NgaRepository) : ViewModel() {
    val history: StateFlow<List<HistoryEntity>> = repo.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteBoards: StateFlow<List<Board>> = repo.favoriteBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow<DiscoverState>(DiscoverState.Loading)
    val state = _state.asStateFlow()
    private var generation = 0L

    init { refresh() }

    fun refresh() {
        val request = ++generation
        _state.value = DiscoverState.Loading
        viewModelScope.launch {
            val groups = try { repo.getCategory().getOrThrow() } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == generation) _state.value = DiscoverState.Ready(
                    emptyList(), null, emptyList(), boardError = error.message ?: "版块加载失败"
                )
                return@launch
            }
            val boards = groups.flatMap { group -> listOfNotNull(group.parent) + group.children }
                .distinctBy { "${it.fid}:${it.stid.orEmpty()}" }
            if (boards.isEmpty()) {
                if (request == generation) _state.value = DiscoverState.Ready(
                    emptyList(), null, emptyList(), boardError = "目前没有可显示的版块"
                )
                return@launch
            }
            val savedBoards = repo.favoriteBoards().first()
            val active = savedBoards.firstOrNull { favorite ->
                boards.any { it.fid == favorite.fid && it.stid == favorite.stid }
            } ?: boards.first()
            // This is the same repository path used by the normal topic list.
            val page = try { repo.getThreads(active.fid, active.stid, 1) } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == generation) _state.value = DiscoverState.Ready(
                    boards, active, emptyList(), error.message ?: "讨论加载失败"
                )
                return@launch
            }
            if (request == generation) _state.value = DiscoverState.Ready(
                boards = boards,
                discussionBoard = active,
                discussions = page.threads,
                discussionError = page.error,
                fromCache = page.fromCache,
                cachedAt = page.cachedAt
            )
        }
    }
}
