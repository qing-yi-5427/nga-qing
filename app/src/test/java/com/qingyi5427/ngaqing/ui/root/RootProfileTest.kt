package com.qingyi5427.ngaqing.ui.root

import com.qingyi5427.ngaqing.data.local.AccountDisplay
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.model.UserProfile
import com.qingyi5427.ngaqing.data.repository.NgaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** VM contract tests use controlled flows; real DataStore transactions are tested in AccountProfileTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class RootProfileTest {
    private val display = MutableStateFlow(AccountDisplay())
    private val session = MutableStateFlow(RequestPreferences("", "", "bbs.nga.cn"))
    private val prefs = mockk<UserPreferences>()
    private val repo = mockk<NgaRepository>()

    @Before fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        every { prefs.themeMode } returns flowOf("system")
        every { prefs.isLoggedIn } returns session.map { it.uid.isNotBlank() }
        every { prefs.session } returns session
        every { prefs.uname } returns display.map { it.username }
        every { prefs.uid } returns display.map { it.uid }
        every { prefs.accountDisplay } returns display
        every { prefs.blacklistUsers } returns flowOf(emptySet())
        every { prefs.blacklistKeywords } returns flowOf(emptySet())
        every { prefs.readingTextScale } returns flowOf(1f)
        every { prefs.readingLineSpacing } returns flowOf(1f)
        every { prefs.showSignatures } returns flowOf(true)
        coEvery { prefs.requestPreferences() } coAnswers { session.value }
        coEvery { repo.isSessionCurrent(any()) } coAnswers {
            firstArg<RequestPreferences>().let { request ->
                request.uid == session.value.uid && request.cid == session.value.cid &&
                    request.revision == session.value.revision
            }
        }
        coEvery { prefs.saveProfileIfCurrent(any(), any(), any(), any()) } coAnswers {
            val request = firstArg<RequestPreferences>()
            val uid = secondArg<String>()
            if (request != session.value || uid != request.uid) false
            else {
                display.value = AccountDisplay(uid, thirdArg(), arg(3), request.revision)
                true
            }
        }
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun existingLoginFetchesProfileWithoutNavigationRevisionChange() = runBlocking {
        session.value = RequestPreferences("root-profile-201", "fixture-cid-201", "bbs.nga.cn", 6L)
        display.value = AccountDisplay(session.value.uid, revision = 6L)
        coEvery { repo.userProfile(session.value.uid, any()) } returns
            Result.success(UserProfile(session.value.uid, "已恢复昵称",
                avatar = "https://img.nga.cn/example.png"))

        val root = RootViewModel(prefs, repo)
        val job = root.refreshOwnProfile()
        withTimeout(3_000) { job.join() }
        assertFalse(job.isCancelled)
        assertEquals("已恢复昵称", display.value.username)
        assertEquals("https://img.nga.cn/example.png", display.value.avatar)
        assertEquals(6L, display.value.revision)
        coVerify(exactly = 1) {
            repo.userProfile("root-profile-201", session.value)
            prefs.saveProfileIfCurrent(session.value, session.value.uid, "已恢复昵称",
                "https://img.nga.cn/example.png")
        }
    }

    @Test fun failedRefreshKeepsStoredProfileAndOffersRetry() = runBlocking {
        session.value = RequestPreferences("root-profile-202", "fixture-cid-202", "bbs.nga.cn", 7L)
        display.value = AccountDisplay(session.value.uid, "旧昵称", "https://img.nga.cn/old.png", 7L)
        coEvery { repo.userProfile(session.value.uid, any()) } returns
            Result.failure(IllegalStateException("fictional server error"))

        val root = RootViewModel(prefs, repo)
        withTimeout(3_000) { root.refreshOwnProfile().join() }
        val load = root.profileLoad.value
        assertNotNull(load.error)
        assertFalse(load.loading)
        assertEquals("旧昵称", display.value.username)
        assertEquals("https://img.nga.cn/old.png", display.value.avatar)
        coVerify(exactly = 0) { prefs.saveProfileIfCurrent(any(), any(), any(), any()) }
    }

    @Test fun delayedProfileFromEarlierAccountRoundTripCannotOverwriteCurrentMetadata() = runBlocking {
        val stale = RequestPreferences("root-profile-203-a", "fixture-cid-a", "bbs.nga.cn", 8L)
        session.value = stale
        display.value = AccountDisplay(stale.uid, revision = stale.revision)
        val result = CompletableDeferred<Result<UserProfile>>()
        coEvery { repo.userProfile(stale.uid, stale) } coAnswers { result.await() }
        val root = RootViewModel(prefs, repo)
        val job = root.refreshOwnProfile()
        withTimeout(3_000) {
            while (!root.profileLoad.value.loading) kotlinx.coroutines.yield()
        }

        session.value = RequestPreferences("root-profile-203-b", "fixture-cid-b", "bbs.nga.cn", 9L)
        display.value = AccountDisplay(session.value.uid, "乙用户", revision = 9L)
        session.value = stale.copy(revision = 10L)
        display.value = AccountDisplay(stale.uid, revision = 10L)
        result.complete(Result.success(UserProfile(stale.uid, "过期昵称")))
        withTimeout(3_000) { job.join() }
        assertTrue(job.isCompleted)
        assertEquals("", display.value.username)
        assertEquals(10L, display.value.revision)
        coVerify(exactly = 1) {
            prefs.saveProfileIfCurrent(stale, stale.uid, "过期昵称", "")
        }
        assertFalse(root.profileLoad.value.revision == 10L && root.profileLoad.value.error != null)
    }
}
