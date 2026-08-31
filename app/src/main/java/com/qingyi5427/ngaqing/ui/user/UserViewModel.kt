package com.qingyi5427.ngaqing.ui.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.UserProfile
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val topics: List<ThreadItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repo: NgaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val uid: String = savedStateHandle["uid"] ?: ""
    val fallbackName: String = savedStateHandle["name"] ?: uid
    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = UserUiState(loading = true)
        viewModelScope.launch {
            val profile = async { repo.userProfile(uid) }
            val topics = async { repo.userTopics(uid) }
            val p = profile.await()
            val t = topics.await()
            _state.value = UserUiState(
                loading = false,
                profile = p.getOrNull(),
                topics = t.getOrDefault(emptyList()),
                error = p.exceptionOrNull()?.message ?: t.exceptionOrNull()?.message
            )
        }
    }
}
