package com.ngaclient.app.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngaclient.app.data.model.Post
import com.ngaclient.app.data.local.NgaDomains
import com.ngaclient.app.data.local.UserPreferences
import com.ngaclient.app.data.remote.LoginHelper
import com.ngaclient.app.data.repository.NgaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface PostUiState {
    data object Loading : PostUiState
    data class Success(
        val posts: List<Post>,
        val page: Int,
        val totalPages: Int,
        val users: Map<String, String> = emptyMap(),
        val renderData: Map<String, PostRenderData> = emptyMap()
    ) : PostUiState
    data class Error(val raw: String, val msg: String) : PostUiState
}

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repo: NgaRepository,
    private val loginHelper: LoginHelper,
    prefs: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val tid: String = savedStateHandle.get<String>("tid") ?: ""

    private val _subject = MutableStateFlow("")
    val subject: StateFlow<String> = _subject.asStateFlow()

    private val _page = MutableStateFlow(1)
    val page: StateFlow<Int> = _page.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _favorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _favorite.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _replying = MutableStateFlow(false)
    val replying: StateFlow<Boolean> = _replying.asStateFlow()

    private val _replyResult = MutableStateFlow<String?>(null)
    val replyResult: StateFlow<String?> = _replyResult.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _targetFloor = MutableStateFlow<Int?>(null)
    val targetFloor: StateFlow<Int?> = _targetFloor.asStateFlow()
    private val _totalRows = MutableStateFlow(0)
    val totalRows: StateFlow<Int> = _totalRows.asStateFlow()
    private val _onlyAuthor = MutableStateFlow(false)
    val onlyAuthor: StateFlow<Boolean> = _onlyAuthor.asStateFlow()
    val ngaDomain: StateFlow<String> =
        prefs.ngaDomain.stateIn(viewModelScope, SharingStarted.Eagerly, NgaDomains.DEFAULT_HOST)
    private val _webCookiesReady = MutableStateFlow(false)
    val webCookiesReady: StateFlow<Boolean> = _webCookiesReady.asStateFlow()
    private val _opAuthorId = MutableStateFlow("")
    private var threadAuthor: String = ""
    private var threadFid: String = ""

    init {
        viewModelScope.launch {
            _favorite.value = repo.isFavorite(tid)
            val history = repo.history(tid)
            _subject.value = history?.title.orEmpty()
            val savedFloor = history?.lastFloor?.takeIf { it > 0 }
            _targetFloor.value = savedFloor
            load(savedFloor?.let { it / 30 + 1 } ?: 1)
        }
    }

    fun load(page: Int = _page.value, showLoading: Boolean = true) {
        val p = page.coerceAtLeast(1)
        _page.value = p
        if (showLoading) _uiState.value = PostUiState.Loading else _isRefreshing.value = true
        viewModelScope.launch {
            val result = repo.getPosts(
                tid,
                p,
                authorId = _opAuthorId.value.takeIf { _onlyAuthor.value && it.isNotBlank() }
            )
            if (result.error != null) {
                if (showLoading || _uiState.value !is PostUiState.Success) {
                    _uiState.value = PostUiState.Error(result.raw, result.error)
                }
                _isRefreshing.value = false
                return@launch
            }
            _subject.value = result.subject.ifBlank {
                result.posts.firstOrNull()?.subject?.takeIf { it.isNotBlank() } ?: _subject.value
            }
            if (result.authorId.isNotBlank()) _opAuthorId.value = result.authorId
            if (result.author.isNotBlank()) threadAuthor = result.author
            if (result.fid.isNotBlank()) threadFid = result.fid
            _totalRows.value = result.totalRows
            // NGA default 30 posts per page; total pages from __ROWS
            val tp = ((result.totalRows) + 29) / 30
            _totalPages.value = tp.coerceAtLeast(1)
            val renderData = prepareRenderData(result.posts, result.users)
            _uiState.value = PostUiState.Success(
                result.posts,
                p,
                _totalPages.value,
                result.users,
                renderData
            )
            repo.recordHistory(tid, _subject.value, result.author, result.fid)
            _isRefreshing.value = false
        }
    }

    fun refresh() = load(_page.value, showLoading = false)

    fun prepareWebCookies() {
        _webCookiesReady.value = false
        viewModelScope.launch {
            runCatching { loginHelper.syncAuthCookies() }
            _webCookiesReady.value = true
        }
    }

    /** 追加式加载下一页（无限滚动 / 手动翻下一页都走这里）。 */
    fun loadMore() {
        val cur = _uiState.value as? PostUiState.Success ?: return
        if (_isLoadingMore.value) return
        val next = cur.page + 1
        if (next > cur.totalPages) return
        _isLoadingMore.value = true
        _loadError.value = null
        viewModelScope.launch {
            val result = repo.getPosts(
                tid,
                next,
                authorId = _opAuthorId.value.takeIf { _onlyAuthor.value && it.isNotBlank() }
            )
            if (result.error != null) {
                _loadError.value = result.error
                _isLoadingMore.value = false
                return@launch
            }
            val tp = ((result.totalRows) + 29) / 30
            _totalPages.value = tp.coerceAtLeast(1)
            _totalRows.value = result.totalRows
            _page.value = next
            val merged = (cur.posts + result.posts).distinctBy { it.pid.ifBlank { "floor-${it.lou}" } }
            val users = cur.users + result.users
            val newRenderData = prepareRenderData(result.posts, users)
            _uiState.value = PostUiState.Success(
                merged,
                next,
                _totalPages.value,
                users,
                cur.renderData + newRenderData
            )
            _isLoadingMore.value = false
        }
    }

    fun retryMore() = loadMore()

    /** 发表回复。fid 从帖子第一楼数据里取，成功后提示并刷新当前页。 */
    fun reply(content: String) {
        if (_replying.value) return
        val cur = _uiState.value as? PostUiState.Success ?: return
        val fid = cur.posts.firstOrNull()?.fid ?: run {
            _replyResult.value = "无法获取版块 ID，暂时不能回复"
            return
        }
        val text = content.trim()
        if (text.isBlank()) {
            _replyResult.value = "回复内容不能为空"
            return
        }
        _replying.value = true
        _replyResult.value = null
        viewModelScope.launch {
            val r = repo.reply(fid, tid, text)
            _replying.value = false
            val success = r.getOrNull()
            if (success != null) {
                refreshTailAfterReply()
                _replyResult.value = success
            } else {
                _replyResult.value = r.exceptionOrNull()?.message ?: "回复失败"
            }
        }
    }

    /** 回复成功后读取最后一页，避免 loadMore 在“已是最后一页”时直接无操作。 */
    private suspend fun refreshTailAfterReply() {
        var target = _totalPages.value.coerceAtLeast(1)
        var result = repo.getPosts(tid, target)
        if (result.error != null) return
        val newestTotal = ((result.totalRows + 29) / 30).coerceAtLeast(1)
        if (newestTotal > target) {
            target = newestTotal
            result = repo.getPosts(tid, target)
            if (result.error != null) return
        }
        _page.value = target
        _totalPages.value = newestTotal
        _subject.value = result.posts.firstOrNull()?.subject ?: _subject.value
        val renderData = prepareRenderData(result.posts, result.users)
        _uiState.value = PostUiState.Success(
            result.posts,
            target,
            newestTotal,
            result.users,
            renderData
        )
    }

    fun consumeReplyResult() {
        _replyResult.value = null
    }

    fun jumpToFloor(floor: Int) {
        val normalized = floor.coerceIn(0, _totalRows.value.coerceAtLeast(0))
        _targetFloor.value = normalized
        load(normalized / 30 + 1)
    }

    fun goToPage(page: Int) {
        val target = page.coerceIn(1, _totalPages.value.coerceAtLeast(1))
        _targetFloor.value = (target - 1) * 30
        load(target)
    }

    fun consumeTargetFloor() {
        _targetFloor.value = null
    }

    fun toggleOnlyAuthor() {
        _onlyAuthor.value = !_onlyAuthor.value
        _targetFloor.value = 0
        load(1)
    }

    fun updateReadFloor(floor: Int) {
        viewModelScope.launch { repo.updateHistoryFloor(tid, floor) }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (_favorite.value) {
                repo.removeFavorite(tid)
                _favorite.value = false
            } else {
                repo.addFavorite(tid, _subject.value, threadFid, threadAuthor)
                _favorite.value = true
            }
        }
    }

    fun nextPage() = goToPage(_page.value + 1)
    fun prevPage() = goToPage(_page.value - 1)

    private suspend fun prepareRenderData(
        posts: List<Post>,
        users: Map<String, String>
    ): Map<String, PostRenderData> = withContext(Dispatchers.Default) {
        posts.associate { post ->
            post.renderKey() to PostRenderData(
                body = PostContentParser.parse(post.content, users),
                signature = if (post.signature.isBlank()) {
                    emptyList()
                } else {
                    PostContentParser.parse(post.signature, users)
                },
                comments = post.comments.map { PostContentParser.parse(it.content, users) }
            )
        }
    }
}

internal fun Post.renderKey(): String = pid.ifBlank { "floor-$lou" }
