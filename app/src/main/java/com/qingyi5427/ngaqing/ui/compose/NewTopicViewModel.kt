package com.qingyi5427.ngaqing.ui.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.DraftEntity
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NewTopicViewModel @Inject constructor(
    private val repo: NgaRepository,
    prefs: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val fid: String = savedStateHandle["fid"] ?: ""
    val stid: String? = savedStateHandle["stid"]
    val boardName: String = savedStateHandle["name"] ?: "版块"
    private val draftKey = "new:$fid:${stid.orEmpty()}"
    val ngaDomain: StateFlow<String> = prefs.ngaDomain.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        NgaDomains.DEFAULT_HOST
    )

    private val _draft = MutableStateFlow<DraftEntity?>(null)
    val draft: StateFlow<DraftEntity?> = _draft.asStateFlow()
    private val _draftLoaded = MutableStateFlow(false)
    val draftLoaded: StateFlow<Boolean> = _draftLoaded.asStateFlow()
    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()
    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    init {
        viewModelScope.launch {
            _draft.value = repo.draft(draftKey)
            _draftLoaded.value = true
        }
    }

    fun saveDraft(subject: String, content: String) {
        viewModelScope.launch {
            if (subject.isBlank() && content.isBlank()) {
                repo.deleteDraft(draftKey)
            } else {
                val item = DraftEntity(
                    key = draftKey,
                    kind = "new",
                    fid = fid,
                    stid = stid.orEmpty(),
                    subject = subject,
                    content = content
                )
                repo.saveDraft(item)
                _draft.value = item
            }
        }
    }

    fun publish(subject: String, content: String) {
        if (_publishing.value || subject.isBlank() || content.isBlank()) return
        _publishing.value = true
        _result.value = null
        viewModelScope.launch {
            val response = repo.publish(
                action = "new",
                fid = fid,
                stid = stid,
                subject = subject.trim(),
                content = content.trim()
            )
            _publishing.value = false
            response.onSuccess {
                repo.deleteDraft(draftKey)
                _draft.value = null
                _result.value = it
            }.onFailure {
                _result.value = it.message ?: "发布失败"
            }
        }
    }

    fun consumeResult() { _result.value = null }
}
