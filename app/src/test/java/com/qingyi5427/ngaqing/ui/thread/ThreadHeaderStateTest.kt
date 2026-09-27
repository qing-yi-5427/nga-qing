package com.qingyi5427.ngaqing.ui.thread

import androidx.lifecycle.SavedStateHandle
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.model.ThreadPage
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadHeaderStateTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun sourceAndPostSidebarShareHeaderStateButAnotherBoardDoesNot() = runTest {
        val repo = repo()
        val saved = SavedStateHandle(mapOf("fid" to "1"))
        val board = ThreadListViewModel(repo, saved)
        runCurrent()
        val sourceHeader = board.headerVisible
        val sidebarHeader = board.headerVisible // Both destinations use the source entry's ViewModel.
        board.setHeaderVisible(false)

        assertSame(sourceHeader, sidebarHeader)
        assertFalse(sourceHeader.value)
        assertFalse(saved.get<Boolean>("thread_header_visible") ?: true)

        val restored = ThreadListViewModel(
            repo, SavedStateHandle(mapOf("fid" to "1", "thread_header_visible" to false))
        )
        val otherBoard = ThreadListViewModel(repo, SavedStateHandle(mapOf("fid" to "2")))
        runCurrent()
        assertFalse(restored.headerVisible.value)
        assertTrue(otherBoard.headerVisible.value)
    }

    @Test fun emptyFirstLayoutCannotRevealHeaderButMeasuredTopCan() {
        assertFalse(shouldRevealHeaderAtTop(0, 0, 0, 0))
        assertFalse(shouldRevealHeaderAtTop(20, 0, 0, 0))
        assertTrue(shouldRevealHeaderAtTop(20, 8, 0, 0))
        assertFalse(shouldRevealHeaderAtTop(20, 8, 1, 0))
        assertFalse(shouldRevealHeaderAtTop(20, 8, 0, 5))
    }

    private fun repo(): NgaRepository = mockk<NgaRepository>().also { repo ->
        every { repo.history() } returns flowOf(emptyList())
        coEvery { repo.captureSession() } returns RequestPreferences("u", "c", "nga.178.com", 1)
        coEvery { repo.isFavoriteBoard(any(), null, any()) } returns false
        coEvery { repo.getThreads(any(), null, any(), null, false, false) } returns ThreadPage()
    }
}
