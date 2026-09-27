package com.qingyi5427.ngaqing.ui

import androidx.lifecycle.SavedStateHandle
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.model.ThreadItem
import com.qingyi5427.ngaqing.data.model.ThreadPage
import com.qingyi5427.ngaqing.data.model.CommunityItem
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.ui.search.SearchUiState
import com.qingyi5427.ngaqing.ui.search.SearchViewModel
import com.qingyi5427.ngaqing.ui.community.CommunityViewModel
import com.qingyi5427.ngaqing.ui.community.CommunityTab
import com.qingyi5427.ngaqing.ui.community.CommunityState
import com.qingyi5427.ngaqing.ui.thread.ThreadListViewModel
import com.qingyi5427.ngaqing.ui.thread.ThreadUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestOrderingTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun refreshBlocksPaginationAndKeepsFreshPage() = runTest {
        val repo = threadRepo()
        val refresh = CompletableDeferred<ThreadPage>()
        var firstPageCalls = 0
        coEvery { repo.getThreads("1", null, any(), null, false, false) } coAnswers {
            when (thirdArg<Int>()) {
                1 -> if (firstPageCalls++ == 0) page("old") else refresh.await()
                else -> page("unexpected")
            }
        }
        val vm = ThreadListViewModel(repo, SavedStateHandle(mapOf("fid" to "1")))
        runCurrent()
        assertEquals("old", (vm.uiState.value as ThreadUiState.Success).threads.single().tid)

        vm.refresh()
        runCurrent()
        vm.loadMore() // A near-bottom scroll event must not cancel the pending refresh.
        runCurrent()
        coVerify(exactly = 0) { repo.getThreads("1", null, 2, null, false, false) }
        assertTrue(vm.isRefreshing.value)

        refresh.complete(page("fresh"))
        runCurrent()
        assertEquals("fresh", (vm.uiState.value as ThreadUiState.Success).threads.single().tid)
        assertFalse(vm.isRefreshing.value)
    }

    @Test fun canceledPaginationCannotOverwriteRefreshEvenIfItReturnsLate() = runTest {
        val repo = threadRepo()
        val staleMore = CompletableDeferred<ThreadPage>()
        var firstPageCalls = 0
        coEvery { repo.getThreads("1", null, any(), null, false, false) } coAnswers {
            when (thirdArg<Int>()) {
                1 -> if (firstPageCalls++ == 0) page("old") else page("fresh")
                else -> withContext(NonCancellable) { staleMore.await() }
            }
        }
        val vm = ThreadListViewModel(repo, SavedStateHandle(mapOf("fid" to "1")))
        runCurrent()
        vm.loadMore()
        runCurrent()
        vm.refresh()
        runCurrent()
        staleMore.complete(page("stale-more"))
        runCurrent()

        val state = vm.uiState.value as ThreadUiState.Success
        assertEquals(listOf("fresh"), state.threads.map { it.tid })
        assertEquals(1, state.page)
        assertFalse(state.isLoadingMore)
    }

    @Test fun olderSearchCannotReplaceNewerResults() = runTest {
        val repo = mockk<NgaRepository>()
        val stale = CompletableDeferred<ThreadPage>()
        coEvery { repo.search(any(), null, null) } coAnswers {
            if (firstArg<String>() == "old") withContext(NonCancellable) { stale.await() }
            else page("new")
        }
        val vm = SearchViewModel(repo)
        vm.search("old")
        runCurrent()
        vm.search("new")
        runCurrent()
        stale.complete(page("old"))
        runCurrent()
        assertEquals("new", (vm.uiState.value as SearchUiState.Success).threads.single().tid)

        vm.search("   ")
        assertTrue(vm.uiState.value is SearchUiState.Empty)
    }

    @Test fun oldNotificationsCannotReplaceSelectedMessages() = runTest {
        val repo = mockk<NgaRepository>()
        val stale = CompletableDeferred<Result<List<CommunityItem>>>()
        every { repo.watchedThreads() } returns flowOf(emptyList())
        coEvery { repo.captureSession() } returns RequestPreferences("u", "c", "nga.178.com", 1)
        coEvery { repo.notifications(any()) } coAnswers {
            withContext(NonCancellable) { stale.await() }
        }
        coEvery { repo.privateMessages(any()) } returns Result.success(listOf(CommunityItem("m", "message")))

        val vm = CommunityViewModel(repo)
        runCurrent()
        vm.select(CommunityTab.MESSAGES)
        runCurrent()
        stale.complete(Result.success(listOf(CommunityItem("n", "notification"))))
        runCurrent()

        assertEquals("m", (vm.state.value as CommunityState.Content).items.single().id)
        assertEquals(CommunityTab.MESSAGES, vm.tab.value)
    }

    private fun threadRepo(): NgaRepository = mockk<NgaRepository>().also { repo ->
        every { repo.history() } returns flowOf(emptyList())
        coEvery { repo.captureSession() } returns RequestPreferences("u", "c", "nga.178.com", 1)
        coEvery { repo.isFavoriteBoard("1", null, any()) } returns false
    }

    private fun page(tid: String): ThreadPage = ThreadPage(
        threads = listOf(ThreadItem(tid, tid, "author", "u", 1L, replies = 0)),
        totalRows = 60
    )
}
