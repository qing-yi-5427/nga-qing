package com.qingyi5427.ngaqing.ui.thread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

sealed interface ThreadUiState {
    data object Loading : ThreadUiState
    @Immutable
    data class Success(
        val threads: List<ThreadItem>,
        val subBoards: List<Board>,
        val page: Int,           // 当前已加载到的最后一页
        val totalPages: Int,
        val recommendedOnly: Boolean,
        val fromCache: Boolean = false,
        val isLoadingMore: Boolean = false,  // 是否正在加载下一页
        val loadError: String? = null        // 加载下一页时的瞬时错误
    ) : ThreadUiState
    data class Error(val raw: String, val msg: String) : ThreadUiState
}

enum class ThreadSort { LAST_REPLY, POST_DATE }

@HiltViewModel
class ThreadListViewModel @Inject constructor(
    private val repo: NgaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val fid: String = savedStateHandle.get<String>("fid") ?: ""
    val stid: String? = savedStateHandle.get<String>("stid")
    val name: String = savedStateHandle.get<String>("name") ?: ""

    private val _page = MutableStateFlow(1)
    val page: StateFlow<Int> = _page.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _onlyAuthor = MutableStateFlow<String?>(null)
    val onlyAuthor: StateFlow<String?> = _onlyAuthor.asStateFlow()

    private val _threads = MutableStateFlow<List<ThreadItem>>(emptyList())
    private val _recommendedOnly = MutableStateFlow(false)
    val recommendedOnly: StateFlow<Boolean> = _recommendedOnly.asStateFlow()
    private val _subBoards = MutableStateFlow<List<Board>>(emptyList())
    val subBoards: StateFlow<List<Board>> = _subBoards.asStateFlow()
    private val _sort = MutableStateFlow(ThreadSort.LAST_REPLY)
    val sort: StateFlow<ThreadSort> = _sort.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _isFavoriteBoard = MutableStateFlow(false)
    val isFavoriteBoard: StateFlow<Boolean> = _isFavoriteBoard.asStateFlow()
    private val _headerVisible = MutableStateFlow(savedStateHandle.get<Boolean>(HEADER_VISIBLE) ?: true)
    val headerVisible: StateFlow<Boolean> = _headerVisible.asStateFlow()
    val initialScrollIndex: Int get() = savedStateHandle[SCROLL_INDEX] ?: 0
    val initialScrollOffset: Int get() = savedStateHandle[SCROLL_OFFSET] ?: 0
    val visitedTids = repo.history().map { items -> items.mapTo(mutableSetOf()) { it.tid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _uiState = MutableStateFlow<ThreadUiState>(ThreadUiState.Loading)
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()
    private var fetchJob: Job? = null
    private var fetchGeneration = 0L
    private var session: RequestPreferences? = null
    private var favoriteBusy = false

    init {
        viewModelScope.launch {
            val owner = repo.captureSession()
            session = owner
            _isFavoriteBoard.value = repo.isFavoriteBoard(fid, stid, owner)
        }
        loadInitial()
    }

    /** 加载第一页并清空已累积的列表（进入版块、切换只看楼主时调用）。 */
    fun loadInitial(showLoading: Boolean = true) {
        fetchJob?.cancel()
        fetchGeneration++
        _page.value = 1
        _isRefreshing.value = !showLoading
        if (showLoading) _threads.value = emptyList()
        if (showLoading) _uiState.value = ThreadUiState.Loading else _isRefreshing.value = true
        fetch(1, null)
    }

    fun refresh() = loadInitial(showLoading = false)

    /** 追加加载下一页（列表滚到底部时调用）。 */
    fun loadMore() {
        val cur = _uiState.value
        if (cur !is ThreadUiState.Success) return
        if (cur.isLoadingMore || _isRefreshing.value) return
        if (cur.page >= cur.totalPages) return
        _uiState.value = cur.copy(isLoadingMore = true, loadError = null)
        fetch(cur.page + 1, cur)
    }

    private fun fetch(page: Int, base: ThreadUiState.Success?) {
        fetchJob?.cancel()
        val generation = ++fetchGeneration
        val authorId = _onlyAuthor.value
        val recommendedOnly = _recommendedOnly.value
        val sortByPostDate = _sort.value == ThreadSort.POST_DATE
        fetchJob = viewModelScope.launch {
            val result = try {
                repo.getThreads(
                fid = fid,
                stid = stid,
                page = page,
                authorId = authorId,
                recommendedOnly = recommendedOnly,
                sortByPostDate = sortByPostDate
                )
            } catch (e: CancellationException) {
                throw e
            }
            if (generation != fetchGeneration) return@launch
            if (result.error != null) {
                _isRefreshing.value = false
                if (page == 1) {
                    _uiState.value = ThreadUiState.Error(result.raw, result.error)
                } else {
                    // 保留已加载内容，仅提示本次加载失败，允许重试
                    val cur = _uiState.value
                    if (cur is ThreadUiState.Success) {
                        _uiState.value = cur.copy(isLoadingMore = false, loadError = result.error)
                    }
                }
                return@launch
            }
            val tp = if (result.totalRows > 0) ((result.totalRows + 29) / 30).coerceAtLeast(1) else 1
            val combined = (if (page == 1) result.threads else (base?.threads.orEmpty() + result.threads))
                .distinctBy { it.tid }
            _threads.value = combined
            if (page == 1) _subBoards.value = result.subBoards
            _page.value = page
            _totalPages.value = tp
            _uiState.value = ThreadUiState.Success(
                threads = combined,
                subBoards = if (page == 1) result.subBoards else base?.subBoards.orEmpty(),
                page = page,
                totalPages = tp,
                recommendedOnly = recommendedOnly,
                fromCache = result.fromCache,
                isLoadingMore = false,
                loadError = null
            )
            _isRefreshing.value = false
        }
    }

    fun setOnlyAuthor(authorId: String?) {
        _onlyAuthor.value = authorId
        loadInitial()
    }

    fun setRecommendedOnly(enabled: Boolean) {
        if (_recommendedOnly.value == enabled) return
        _recommendedOnly.value = enabled
        loadInitial()
    }

    fun setSort(value: ThreadSort) {
        if (_sort.value == value) return
        _sort.value = value
        loadInitial()
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        savedStateHandle[SCROLL_INDEX] = index.coerceAtLeast(0)
        savedStateHandle[SCROLL_OFFSET] = offset.coerceAtLeast(0)
    }

    fun resetScrollPosition() = saveScrollPosition(0, 0)

    fun setHeaderVisible(visible: Boolean) {
        if (_headerVisible.value == visible) return
        _headerVisible.value = visible
        savedStateHandle[HEADER_VISIBLE] = visible
    }

    fun toggleFavoriteBoard() {
        if (favoriteBusy) return
        favoriteBusy = true
        viewModelScope.launch {
            try {
                val owner = session ?: return@launch
                if (!repo.isSessionCurrent(owner)) return@launch
                if (_isFavoriteBoard.value) {
                    repo.removeFavoriteBoard(fid, stid, owner)
                    _isFavoriteBoard.value = false
                } else {
                    repo.addFavoriteBoard(fid, stid, name, session = owner)
                    _isFavoriteBoard.value = true
                }
            } finally {
                favoriteBusy = false
            }
        }
    }

    fun retryMore() = loadMore()

    private companion object {
        const val SCROLL_INDEX = "thread_scroll_index"
        const val SCROLL_OFFSET = "thread_scroll_offset"
        const val HEADER_VISIBLE = "thread_header_visible"
    }
}
