package com.qingyi5427.ngaqing.ui.board

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.model.BoardGroup
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BoardUiState {
    data object Loading : BoardUiState
    data class Success(val groups: List<BoardGroup>) : BoardUiState
    data class Error(val raw: String?, val msg: String) : BoardUiState
}

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val repo: NgaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<BoardUiState>(BoardUiState.Loading)
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()
    val favoriteBoards = repo.favoriteBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val initialScrollIndex: Int get() = savedStateHandle[SCROLL_INDEX] ?: 0
    val initialScrollOffset: Int get() = savedStateHandle[SCROLL_OFFSET] ?: 0

    init { load() }

    fun load(showLoading: Boolean = true) {
        if (showLoading) _uiState.value = BoardUiState.Loading else _isRefreshing.value = true
        viewModelScope.launch {
            repo.getCategory()
                .onSuccess { groups ->
                    if (groups.isEmpty()) {
                        if (showLoading || _uiState.value !is BoardUiState.Success) {
                            _uiState.value = BoardUiState.Error(null, "未解析到版块，请查看原始数据")
                        }
                    } else {
                        _uiState.value = BoardUiState.Success(groups)
                    }
                    _isRefreshing.value = false
                }
                .onFailure { e ->
                    if (showLoading || _uiState.value !is BoardUiState.Success) {
                        _uiState.value = BoardUiState.Error(null, e.message ?: "加载失败")
                    }
                    _isRefreshing.value = false
                }
        }
    }

    fun refresh() = load(showLoading = false)

    fun saveScrollPosition(index: Int, offset: Int) {
        savedStateHandle[SCROLL_INDEX] = index.coerceAtLeast(0)
        savedStateHandle[SCROLL_OFFSET] = offset.coerceAtLeast(0)
    }

    private companion object {
        const val SCROLL_INDEX = "board_scroll_index"
        const val SCROLL_OFFSET = "board_scroll_offset"
    }
}
