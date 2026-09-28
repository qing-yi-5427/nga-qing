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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadHeaderStateTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun sourceAndPostSidebarSharePartialHeaderProgressButAnotherBoardDoesNot() = runTest {
        val repo = repo()
        val saved = SavedStateHandle(mapOf("fid" to "1"))
        val board = ThreadListViewModel(repo, saved)
        runCurrent()
        val sourceHeader = board.headerHiddenFraction
        val sidebarHeader = board.headerHiddenFraction // Both destinations use the source entry's ViewModel.
        board.setHeaderHiddenFraction(0.4f)

        assertSame(sourceHeader, sidebarHeader)
        assertEquals(0.4f, sourceHeader.value)
        assertEquals(0.4f, saved.get<Float>("thread_header_hidden_fraction"))

        val restored = ThreadListViewModel(
            repo, SavedStateHandle(mapOf("fid" to "1", "thread_header_hidden_fraction" to 0.4f))
        )
        val otherBoard = ThreadListViewModel(repo, SavedStateHandle(mapOf("fid" to "2")))
        runCurrent()
        assertEquals(0.4f, restored.headerHiddenFraction.value)
        assertEquals(0f, otherBoard.headerHiddenFraction.value)
        board.resetScrollPosition()
        assertEquals(0f, board.headerHiddenFraction.value)
    }

    @Test fun headerConsumesOnlyItsOwnTravelInBothDirections() {
        val middle = consumeHeaderScroll(-40f, 200, 0f)
        assertEquals(0.2f, middle.hiddenFraction)
        assertEquals(-40f, middle.consumedY)
        val hidden = consumeHeaderScroll(-200f, 200, middle.hiddenFraction)
        assertEquals(1f, hidden.hiddenFraction)
        assertEquals(-160f, hidden.consumedY) // Remaining -40 px belongs to LazyColumn.
        val revealed = consumeHeaderScroll(40f, 200, hidden.hiddenFraction)
        assertEquals(0.8f, revealed.hiddenFraction)
        assertEquals(40f, revealed.consumedY)
        assertEquals(0f, consumeHeaderScroll(40f, 0, 0.5f).consumedY)
    }

    @Test fun legacyHiddenFlagMigratesToFullTravel() = runTest {
        val restored = ThreadListViewModel(repo(), SavedStateHandle(mapOf("fid" to "1", "thread_header_visible" to false)))
        runCurrent()
        assertEquals(1f, restored.headerHiddenFraction.value)
    }

    private fun repo(): NgaRepository = mockk<NgaRepository>().also { repo ->
        every { repo.history() } returns flowOf(emptyList())
        coEvery { repo.captureSession() } returns RequestPreferences("u", "c", "nga.178.com", 1)
        coEvery { repo.isFavoriteBoard(any(), null, any()) } returns false
        coEvery { repo.getThreads(any(), null, any(), null, false, false) } returns ThreadPage()
    }
}
