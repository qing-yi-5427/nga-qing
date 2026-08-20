package com.ngaclient.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngaclient.app.data.model.ThreadItem
import com.ngaclient.app.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Empty : SearchUiState
    data object Loading : SearchUiState
    data class Success(val threads: List<ThreadItem>) : SearchUiState
    data class Error(val raw: String, val msg: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: NgaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Empty)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(key: String, fid: String? = null, stid: String? = null) {
        val k = key.trim()
        if (k.isBlank()) return
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            val result = repo.search(k, fid, stid)
            if (result.error != null) {
                _uiState.value = SearchUiState.Error(result.raw, result.error)
            } else {
                _uiState.value = SearchUiState.Success(result.threads)
            }
        }
    }
}
