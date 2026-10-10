package com.qingyi5427.ngaqing.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.LoginHelper
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReadingFloorPersistenceTest {
    @Test fun orderedInFlightFloorWritesFinishAfterReaderDisposal() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = RequestPreferences("owner", "cid", "bbs.nga.cn", 3)
        val repo = mockk<NgaRepository>()
        val prefs = mockk<UserPreferences>()
        val requestStarted = CompletableDeferred<Unit>()
        val writeStarted = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        val writeFinished = CompletableDeferred<Unit>()
        val storedFloor = AtomicInteger(0)
        coEvery { repo.captureSession() } returns session
        coEvery { repo.isFavorite("tid", session) } returns false
        coEvery { repo.isWatching("tid", session) } returns false
        coEvery { repo.draft("reply:tid", session) } returns null
        coEvery { repo.history("tid", session) } returns null
        coEvery { repo.getPosts("tid", 1, null) } coAnswers {
            requestStarted.complete(Unit)
            awaitCancellation()
        }
        coEvery { repo.updateHistoryFloor("tid", any(), session) } coAnswers {
            val floor = secondArg<Int>()
            if (floor == 9) {
                writeStarted.complete(Unit)
                allowWrite.await() // Represents a Room write suspended at an IO boundary.
            }
            storedFloor.set(floor)
            if (floor == 5) writeFinished.complete(Unit)
        }
        every { prefs.ngaDomain } returns flowOf("bbs.nga.cn")
        val store = ViewModelStore()
        try {
            store.put("post", PostViewModel(repo, mockk<LoginHelper>(), prefs,
                SavedStateHandle(mapOf("tid" to "tid"))))
            runCurrent()
            requestStarted.await() // Session is captured; the page request is deliberately suspended.
            val vm = store["post"] as PostViewModel
            vm.updateReadFloor(9)
            runCurrent()
            writeStarted.await()
            vm.updateReadFloor(5)
            store.clear() // Same immediate return/disposal seen by the real navigation path.
            allowWrite.complete(Unit)
            kotlinx.coroutines.withTimeout(1_000) { writeFinished.await() }
            assertEquals(5, storedFloor.get())
            coVerifyOrder {
                repo.updateHistoryFloor("tid", 9, session)
                repo.updateHistoryFloor("tid", 5, session)
            }
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun immediatelyReopenedReaderWaitsForConfirmedPendingFloor() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = RequestPreferences("owner", "cid", "bbs.nga.cn", 4)
        val repo = mockk<NgaRepository>()
        val prefs = mockk<UserPreferences>()
        val storedFloor = AtomicInteger(9)
        val historyReads = AtomicInteger(0)
        val writeStarted = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        val secondHistoryRead = CompletableDeferred<Int>()
        coEvery { repo.captureSession() } returns session
        coEvery { repo.isFavorite("tid", session) } returns false
        coEvery { repo.isWatching("tid", session) } returns false
        coEvery { repo.draft("reply:tid", session) } returns null
        coEvery { repo.history("tid", session) } coAnswers {
            val floor = storedFloor.get()
            if (historyReads.incrementAndGet() == 2) secondHistoryRead.complete(floor)
            HistoryEntity("tid", "title", "author", "fid", lastFloor = floor,
                ownerUid = "owner")
        }
        coEvery { repo.getPosts("tid", 1, null) } coAnswers { awaitCancellation() }
        coEvery { repo.updateHistoryFloor("tid", 5, session) } coAnswers {
            writeStarted.complete(Unit)
            allowWrite.await()
            storedFloor.set(5)
        }
        every { prefs.ngaDomain } returns flowOf("bbs.nga.cn")
        val first = ViewModelStore()
        val reopened = ViewModelStore()
        try {
            val oldVm = PostViewModel(repo, mockk<LoginHelper>(), prefs,
                SavedStateHandle(mapOf("tid" to "tid")))
            first.put("post", oldVm)
            runCurrent()
            assertEquals(1, historyReads.get())
            oldVm.updateReadFloor(5)
            runCurrent()
            writeStarted.await()
            first.clear()

            val newVm = PostViewModel(repo, mockk<LoginHelper>(), prefs,
                SavedStateHandle(mapOf("tid" to "tid")))
            reopened.put("post", newVm)
            runCurrent()
            assertEquals("the new reader must not read the stale DB floor", 1,
                historyReads.get())
            allowWrite.complete(Unit)
            assertEquals(5, kotlinx.coroutines.withTimeout(1_000) { secondHistoryRead.await() })
            runCurrent()
            assertEquals(5, newVm.targetFloor.value)
        } finally {
            first.clear()
            reopened.clear()
            Dispatchers.resetMain()
        }
    }
}
