package com.qingyi5427.ngaqing.ui.web

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.remote.LoginHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loginHelper: LoginHelper
) : ViewModel() {
    val url: String = savedStateHandle["url"] ?: ""
    val title: String = savedStateHandle["title"] ?: "高级编辑"

    private val _cookiesReady = MutableStateFlow(false)
    val cookiesReady = _cookiesReady.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { loginHelper.syncAuthCookies() }
            _cookiesReady.value = true
        }
    }
}
