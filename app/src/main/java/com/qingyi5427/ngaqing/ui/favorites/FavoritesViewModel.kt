package com.qingyi5427.ngaqing.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: NgaRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntity>> =
        repo.favorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun remove(tid: String) {
        viewModelScope.launch { repo.removeFavorite(tid) }
    }
}
