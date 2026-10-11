package com.qingyi5427.ngaqing.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.LoginHelper
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import com.qingyi5427.ngaqing.data.repository.ServerFavoriteFolder
import com.qingyi5427.ngaqing.data.repository.ServerFavoriteSelection
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FavoriteFolderDialogStateTest {
    @Test fun cancelWhileFolderSnapshotLoadsCannotReopenDialogOrMutateFavorite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = RequestPreferences("owner", "cid", "bbs.nga.cn", 7)
        val repo = mockk<NgaRepository>()
        val prefs = mockk<UserPreferences>()
        val requestStarted = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ServerFavoriteSelection>()
        coEvery { repo.captureSession() } returns session
        coEvery { repo.isSessionCurrent(session) } returns true
        coEvery { repo.isFavorite("tid", session) } returns false
        coEvery { repo.isWatching("tid", session) } returns false
        coEvery { repo.draft("reply:tid", session) } returns null
        coEvery { repo.history("tid", session) } returns null
        coEvery { repo.getPosts("tid", 1, null) } coAnswers { awaitCancellation() }
        coEvery { repo.loadServerFavoriteSelection("tid", session) } coAnswers {
            requestStarted.complete(Unit)
            Result.success(response.await())
        }
        every { prefs.ngaDomain } returns flowOf("bbs.nga.cn")
        val store = ViewModelStore()
        try {
            val vm = PostViewModel(repo, mockk<LoginHelper>(), prefs,
                SavedStateHandle(mapOf("tid" to "tid")))
            store.put("post", vm)
            runCurrent()
            vm.openFavoriteFolders()
            runCurrent()
            requestStarted.await()
            assertEquals(FavoriteFolderUiState.Loading, vm.favoriteFolderUiState.value)

            vm.dismissFavoriteFolders()
            response.complete(ServerFavoriteSelection(
                listOf(ServerFavoriteFolder("10", "阅读", 1, true)), emptySet(), false
            ))
            runCurrent()
            assertEquals(FavoriteFolderUiState.Closed, vm.favoriteFolderUiState.value)
            assertEquals(false, vm.isFavorite.value)
            coVerify(exactly = 0) { repo.addFavorite(any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { repo.removeFavorite(any(), any(), any(), any()) }
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
