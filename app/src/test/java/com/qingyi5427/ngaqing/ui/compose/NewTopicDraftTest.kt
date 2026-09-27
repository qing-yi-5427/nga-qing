package com.qingyi5427.ngaqing.ui.compose

import androidx.lifecycle.SavedStateHandle
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewTopicDraftTest {
    private val owner = RequestPreferences("u", "c", "nga.178.com", 1)

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun immediateCloseWriteKeepsLastEdit() = runTest {
        val repo = repo()
        val vm = viewModel(repo)
        runCurrent()
        assertTrue(vm.draftLoaded.value)

        vm.saveDraft("subject", "first")
        vm.saveDraft("subject", "last character", immediate = true)

        coVerify(timeout = 2_000) {
            repo.saveDraft(match { it.content == "last character" }, owner)
        }
        assertEquals("last character", vm.draft.value?.content)
    }

    @Test fun ambiguousPublishFailureRetainsDraftAndEditorState() = runTest {
        val repo = repo()
        coEvery { repo.isSessionCurrent(owner) } returns true
        coEvery { repo.publish("new", "1", null, null, null, any(), any(), owner) } returns
            Result.failure(IllegalStateException("无法确认发布是否成功，请刷新确认"))
        val vm = viewModel(repo)
        runCurrent()
        vm.saveDraft("subject", "content", immediate = true)
        vm.publish("subject", "content")
        runCurrent()

        assertFalse(vm.publishSucceeded.value)
        assertEquals("content", vm.draft.value?.content)
        assertEquals("无法确认发布是否成功，请刷新确认", vm.result.value)
        coVerify(exactly = 0) { repo.deleteDraft(any(), any()) }
    }

    private fun viewModel(repo: NgaRepository): NewTopicViewModel {
        val prefs = mockk<UserPreferences>()
        every { prefs.ngaDomain } returns flowOf("nga.178.com")
        return NewTopicViewModel(repo, prefs, SavedStateHandle(mapOf("fid" to "1")))
    }

    private fun repo(): NgaRepository = mockk<NgaRepository>().also { repo ->
        coEvery { repo.captureSession() } returns owner
        coEvery { repo.draft(any(), owner) } returns null
        coEvery { repo.saveDraft(any(), owner) } returns Unit
        coEvery { repo.deleteDraft(any(), owner) } returns Unit
    }
}
