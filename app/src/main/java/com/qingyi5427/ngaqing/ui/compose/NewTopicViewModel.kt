package com.qingyi5427.ngaqing.ui.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.local.DraftEntity
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
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
    private val _publishSucceeded = MutableStateFlow(false)
    val publishSucceeded: StateFlow<Boolean> = _publishSucceeded.asStateFlow()
    @Volatile private var session: RequestPreferences? = null
    private val draftMutex = Mutex()
    private var draftJob: Job? = null
    @Volatile private var draftVersion = 0L

    init {
        viewModelScope.launch {
            val owner = repo.captureSession()
            session = owner
            _draft.value = repo.draft(draftKey, owner)
            _draftLoaded.value = true
        }
    }

    fun saveDraft(subject: String, content: String, immediate: Boolean = false) {
        if (_publishSucceeded.value || !_draftLoaded.value) return
        val item = DraftEntity(
            key = draftKey,
            kind = "new",
            fid = fid,
            stid = stid.orEmpty(),
            subject = subject,
            content = content
        )
        _draft.value = item.takeUnless { subject.isBlank() && content.isBlank() }
        draftJob?.cancel()
        val version = ++draftVersion
        draftJob = draftScope.launch {
            if (!immediate) delay(300)
            val owner = session ?: return@launch
            try {
                draftMutex.withLock {
                    if (version != draftVersion) return@withLock
                    if (subject.isBlank() && content.isBlank()) repo.deleteDraft(draftKey, owner)
                    else repo.saveDraft(item, owner)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NgaDraft", "主题草稿保存失败", e)
            }
        }
    }

    fun publish(subject: String, content: String) {
        if (_publishing.value || subject.isBlank() || content.isBlank()) return
        _publishing.value = true
        _result.value = null
        _publishSucceeded.value = false
        viewModelScope.launch {
            val owner = session
            if (owner == null || !repo.isSessionCurrent(owner)) {
                _publishing.value = false
                _result.value = "账号已切换，请重新打开编辑器后再发布"
                return@launch
            }
            val response = repo.publish(
                action = "new",
                fid = fid,
                stid = stid,
                subject = subject.trim(),
                content = content.trim(),
                session = owner
            )
            response.onSuccess {
                draftJob?.cancel()
                draftVersion++
                draftJob = draftScope.launch {
                    try {
                        draftMutex.withLock { repo.deleteDraft(draftKey, owner) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("NgaDraft", "主题发布后清理草稿失败", e)
                    }
                }
                _draft.value = null
                _publishSucceeded.value = true
                _result.value = it
            }.onFailure {
                if (it is CancellationException) throw it
                _result.value = it.message ?: "发布失败"
            }
            _publishing.value = false
        }
    }

    fun consumeResult() { _result.value = null }

    private companion object {
        // Complete the final bounded draft write even when navigation clears this ViewModel.
        val draftScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
