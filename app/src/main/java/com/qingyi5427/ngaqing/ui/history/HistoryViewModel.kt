package com.qingyi5427.ngaqing.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: NgaRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(HistorySort.RECENT)
    val history = combine(repo.history(), query, sort) { items, q, order ->
        items.asSequence()
            .filter { q.isBlank() || it.title.contains(q, true) || it.author.contains(q, true) }
            .let { sequence ->
                when (order) {
                    HistorySort.RECENT -> sequence.sortedByDescending { it.lastVisited }
                    HistorySort.TITLE -> sequence.sortedBy { it.title }
                    HistorySort.AUTHOR -> sequence.sortedBy { it.author }
                }
            }.toList()
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { query.value = value }
    fun setSort(value: HistorySort) { sort.value = value }

    fun remove(tid: String) = viewModelScope.launch { repo.removeHistory(tid) }

    fun clear() = viewModelScope.launch { repo.clearHistory() }
}

enum class HistorySort { RECENT, TITLE, AUTHOR }
