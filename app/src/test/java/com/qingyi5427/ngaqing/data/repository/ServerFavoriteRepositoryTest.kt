package com.qingyi5427.ngaqing.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.NgaApi
import com.qingyi5427.ngaqing.data.remote.SignedFavoriteApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ServerFavoriteRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val prefs = UserPreferences(context)
    private val api = mockk<NgaApi>()
    private val signed = mockk<SignedFavoriteApi>()
    private val signedMemberships = mutableSetOf<Pair<String, String>>()
    private val repo = NgaRepository(api, prefs, db, context, signed)

    init {
        coEvery { signed.add(any(), any(), any()) } coAnswers {
            signedMemberships += secondArg<String>() to thirdArg<String>()
            JSONObject("""{"code":"0","result":"操作成功"}""")
        }
        coEvery { signed.remove(any(), any(), any()) } coAnswers {
            signedMemberships -= secondArg<String>() to thirdArg<String>()
            JSONObject("""{"code":"0","result":["操作成功"]}""")
        }
        coEvery { signed.listTopics(any(), any(), any()) } coAnswers {
            signedPage(*signedMemberships.filter { it.first == secondArg<String>() }
                .map { it.second }.toTypedArray())
        }
    }

    @Test fun legacyLocalCopySurvivesCloudSelectionAndFolderSpecificRemoval() = runBlocking {
        prefs.saveAuth("fav-legacy-a", "cid-a", "A")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity(
            tid = "42", title = "saved locally", fid = "9", author = "A",
            folder = "待读", ts = 1234L, ownerUid = owner.uid
        ))
        mockTwoFolders()
        coEvery { api.threadList(favor = 1, page = 1, session = any()) } returnsMany listOf(
            threadPage("10"), threadPage("42"), threadPage("42"), threadPage("42"), threadPage("42")
        )
        coEvery { api.threadList(favor = 7, page = 1, session = any()) } returnsMany listOf(
            threadPage("42"), threadPage("42"), threadPage("42", "43"), threadPage("43"), threadPage("43")
        )
        val initial = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        assertEquals(setOf("7"), initial.selectedIds)
        assertTrue(initial.hasLocalCopy)
        assertEquals("待读", db.favoriteDao().get(owner.uid, "42")?.folder)

        assertTrue(repo.addFavorite("42", "remote title", folderId = "1", selection = initial, session = owner).isSuccess)
        coVerify(exactly = 1) { signed.add(owner, "1", "42") }
        val selectedBoth = initial.copy(selectedIds = setOf("1", "7"))
        assertEquals(setOf("1", "7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())

        assertTrue(repo.removeFavorite("42", "7", selectedBoth, owner).isSuccess)
        coVerify(exactly = 1) { signed.remove(owner, "7", "42") }
        assertEquals(setOf("1"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        assertTrue(repo.removeFavorite("42", "1", initial.copy(selectedIds = setOf("1")), owner).isFailure)
        coVerify(exactly = 0) { signed.remove(owner, "1", "42") }
        assertEquals(setOf("1"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        val local = db.favoriteDao().get(owner.uid, "42")
        assertNotNull(local)
        assertEquals("local", local?.source)
        assertEquals("待读", local?.folder)
        assertEquals(1234L, local?.ts)
        db.close()
    }

    @Test fun syncedServerCopyIsRemovedOnlyAfterLastCloudFolder() = runBlocking {
        prefs.saveAuth("fav-server-b", "cid-b", "B")
        val owner = repo.captureSession()
        mockTwoFolders()
        coEvery { api.threadList(favor = 1, page = 1, session = any()) } returnsMany listOf(
            threadPage("10"), threadPage("10"), threadPage("42"),
            threadPage("42"), threadPage("42"), threadPage("42")
        )
        coEvery { api.threadList(favor = 7, page = 1, session = any()) } returnsMany listOf(
            threadPage("42"), threadPage("42"), threadPage("42"),
            threadPage("42", "43"), threadPage("43"), threadPage("43")
        )
        val imported = repo.syncServerFavorites(owner).getOrThrow()
        assertEquals(2, imported)
        val remote = db.favoriteDao().get(owner.uid, "42")
        assertEquals("server", remote?.source)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        val initial = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        assertTrue(repo.addFavorite("42", "title", folderId = "1", selection = initial, session = owner).isSuccess)
        assertTrue(repo.removeFavorite("42", "7", initial.copy(selectedIds = setOf("7", "1")), owner).isSuccess)
        assertNotNull(db.favoriteDao().get(owner.uid, "42"))
        assertTrue(repo.removeFavorite("42", "1", initial.copy(selectedIds = setOf("1")), owner).isFailure)
        assertNotNull(db.favoriteDao().get(owner.uid, "42"))
        coVerify(exactly = 0) { signed.remove(owner, "1", "42") }
        db.close()
    }

    @Test fun removingLegacyLocalCopyKeepsLiveCloudMembershipVisible() = runBlocking {
        prefs.saveAuth("fav-local-cloud-e", "cid-e", "E")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity(
            tid = "42", title = "local", fid = "", author = "A",
            folder = "待读", ownerUid = owner.uid
        ))
        mockTwoFolders()
        val selection = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        repo.removeLocalFavorite("42", owner, selection)
        assertEquals("server", db.favoriteDao().get(owner.uid, "42")?.source)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        db.close()
    }

    @Test fun failedOrUnknownMutationNeverCreatesLocalSuccess() = runBlocking {
        prefs.saveAuth("fav-error-c", "cid-c", "C")
        val owner = repo.captureSession()
        mockTwoFolders()
        val selection = repo.loadServerFavoriteSelection("99", owner).getOrThrow()
        coEvery { signed.add(owner, "1", "99") } throws IllegalStateException("not allowed")
        assertTrue(repo.addFavorite("99", "title", folderId = "1", selection = selection, session = owner).isFailure)
        assertNull(db.favoriteDao().get(owner.uid, "99"))
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "99").isEmpty())
        coVerify(exactly = 1) { api.listServerFavoriteFolders(session = owner) }
        coVerify(exactly = 1) { signed.add(owner, "1", "99") }
        db.close()
    }

    @Test fun unknownAddMessageSucceedsOnlyAfterOneWriteAndConfirmedMembership() = runBlocking {
        prefs.saveAuth("fav-readback-add", "cid-a", "A")
        val owner = repo.captureSession()
        mockTwoFolders()
        coEvery { api.threadList(favor = 1, page = 1, session = owner) } returnsMany listOf(
            threadPage("10"), threadPage("99")
        )
        val before = repo.loadServerFavoriteSelection("99", owner).getOrThrow()

        val after = repo.addFavorite("99", "title", folderId = "1", selection = before, session = owner)
            .getOrThrow()
        assertEquals(setOf("1"), after.selectedIds)
        assertEquals(setOf("1"), db.serverFavoriteDao().memberships(owner.uid, "99").map { it.folderId }.toSet())
        assertEquals("server", db.favoriteDao().get(owner.uid, "99")?.source)
        coVerify(exactly = 1) { signed.add(owner, "1", "99") }
        db.close()
    }

    @Test fun unknownRemoveMessageNeedsConfirmedAbsenceAndKeepsLocalCopy() = runBlocking {
        prefs.saveAuth("fav-readback-remove", "cid-r", "R")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity(
            tid = "42", title = "local", fid = "", author = "R",
            folder = "待读", ownerUid = owner.uid
        ))
        mockTwoFolders()
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returnsMany listOf(
            threadPage("42"), threadPage("42", "10"), threadPage("10")
        )
        val before = repo.loadServerFavoriteSelection("42", owner).getOrThrow()

        val after = repo.removeFavorite("42", "7", before, owner).getOrThrow()
        assertTrue(after.selectedIds.isEmpty())
        assertTrue(after.hasLocalCopy)
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "42").isEmpty())
        assertEquals("local", db.favoriteDao().get(owner.uid, "42")?.source)
        coVerify(exactly = 1) { signed.remove(owner, "7", "42") }
        db.close()
    }

    @Test fun mismatchedOrFailedReadbackNeverMarksAddAsSuccessful() = runBlocking {
        prefs.saveAuth("fav-readback-fail", "cid-f", "F")
        val owner = repo.captureSession()
        mockTwoFolders()
        val before = repo.loadServerFavoriteSelection("99", owner).getOrThrow()

        assertTrue(repo.addFavorite("99", "title", folderId = "1", selection = before, session = owner).isFailure)
        assertNull(db.favoriteDao().get(owner.uid, "99"))
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "99").isEmpty())
        coVerify(exactly = 1) { signed.add(owner, "1", "99") }
        db.close()
    }

    @Test fun failedReadbackKeepsExistingCloudStateAndDoesNotRepeatDelete() = runBlocking {
        prefs.saveAuth("fav-readback-error", "cid-e", "E")
        val owner = repo.captureSession()
        mockTwoFolders()
        val before = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        db.serverFavoriteDao().putFolder(com.qingyi5427.ngaqing.data.local.ServerFavoriteFolderEntity(
            owner.uid, "7", "待读", 1, false
        ))
        db.serverFavoriteDao().putMembership(com.qingyi5427.ngaqing.data.local.ServerFavoriteMembershipEntity(
            owner.uid, "42", "7"
        ))
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns
            threadPage("42", "10")
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"1":{"id":"1","name":"默认","length":1,"default":1},"7":{"id":"7","name":"待读","length":2}}}}""")

        assertTrue(repo.removeFavorite("42", "7", before, owner).isFailure)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        coVerify(exactly = 1) { signed.remove(owner, "7", "42") }
        db.close()
    }

    @Test fun newFolderRequiresServerReadBackAndPreservesLegacyLocalCopy() = runBlocking {
        prefs.saveAuth("fav-create-f", "cid-f", "F")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity(
            tid = "42", title = "local", fid = "", author = "A",
            folder = "待读", ownerUid = owner.uid
        ))
        coEvery { api.listServerFavoriteFolders(session = owner) } returnsMany listOf(
            Response.success("""{"data":{"0":{},"1":0}}"""),
            Response.success("""{"data":{"0":{"9":{"id":"9","name":"新目录","length":1}},"1":0}}""")
        )
        coEvery { api.threadList(favor = 9, page = 1, session = owner) } returns threadPage("42")
        coEvery { api.addServerFavorite(folder = "-1", tid = "42", session = owner) } returns
            """{"data":{"0":0,"1":"已创建"}}"""

        val empty = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        assertTrue(empty.folders.isEmpty())
        val created = repo.createFavoriteFolderAndAdd("42", "remote", selection = empty, session = owner)
            .getOrThrow()
        assertEquals(setOf("9"), created.selectedIds)
        assertEquals(setOf("9"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        assertEquals("local", db.favoriteDao().get(owner.uid, "42")?.source)
        assertEquals("待读", db.favoriteDao().get(owner.uid, "42")?.folder)
        coVerify(exactly = 1) { api.addServerFavorite(folder = "-1", tid = "42", session = owner) }
        db.close()
    }

    @Test fun unconfirmedNewFolderDoesNotCacheOrRepeatMutation() = runBlocking {
        prefs.saveAuth("fav-create-g", "cid-g", "G")
        val owner = repo.captureSession()
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{},"1":0}}""")
        coEvery { api.addServerFavorite(folder = "-1", tid = "99", session = owner) } returns
            """{"data":{"0":0,"1":"已创建"}}"""

        val empty = repo.loadServerFavoriteSelection("99", owner).getOrThrow()
        assertTrue(repo.createFavoriteFolderAndAdd("99", "title", selection = empty, session = owner).isFailure)
        assertNull(db.favoriteDao().get(owner.uid, "99"))
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "99").isEmpty())
        coVerify(exactly = 1) { api.addServerFavorite(folder = "-1", tid = "99", session = owner) }
        db.close()
    }

    @Test fun incompleteRefreshCannotErasePreviouslyConfirmedMemberships() = runBlocking {
        prefs.saveAuth("fav-partial-d", "cid-d", "D")
        val owner = repo.captureSession()
        mockTwoFolders()
        assertTrue(repo.syncServerFavorites(owner).isSuccess)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())

        coEvery { api.listServerFavoriteFolders(session = any()) } returns
            Response.success("""{"data":{"0":{"1":{"id":"1","name":"默认","length":1,"default":1},"7":{"id":"7","name":"待读","length":2}}}}""")
        assertTrue(repo.syncServerFavorites(owner).isFailure)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        assertEquals("server", db.favoriteDao().get(owner.uid, "42")?.source)

        coEvery { api.listServerFavoriteFolders(session = any()) } returns Response.success("""{"data":{"0":{}}}""")
        assertTrue(repo.syncServerFavorites(owner).isFailure)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        assertNotNull(db.favoriteDao().get(owner.uid, "42"))
        db.close()
    }

    @Test fun accountRevisionChangedDuringWriteCannotConfirmOrUpdateEitherAccount() = runBlocking {
        prefs.saveAuth("fav-revision-a", "cid-a", "A")
        val owner = repo.captureSession()
        mockTwoFolders()
        val selection = repo.loadServerFavoriteSelection("99", owner).getOrThrow()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        coEvery { signed.add(owner, "1", "99") } coAnswers {
            started.complete(Unit)
            response.await()
        }
        val pending = async {
            runCatching { repo.addFavorite("99", "title", folderId = "1", selection = selection, session = owner) }
        }
        started.await()
        prefs.saveAuth("fav-revision-b", "cid-b", "B")
        assertTrue(prefs.switchAccount("fav-revision-a"))
        response.complete(JSONObject("""{"code":"0","result":"操作成功"}"""))
        assertTrue(pending.await().exceptionOrNull() is CancellationException)
        assertNull(db.favoriteDao().get("fav-revision-a", "99"))
        assertNull(db.favoriteDao().get("fav-revision-b", "99"))
        assertTrue(db.serverFavoriteDao().memberships("fav-revision-a", "99").isEmpty())
        db.close()
    }

    @Test fun deletingOnlyFolderAfterConfirmationPreservesLocalCopies() = runBlocking {
        prefs.saveAuth("fav-last-ok", "cid", "A")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity("42", "local", "9", "A",
            folder = "私有分组", ownerUid = owner.uid, source = "local"))
        db.favoriteDao().insert(FavoriteEntity("99", "other local", "9", "A",
            ownerUid = owner.uid, source = "local"))
        db.serverFavoriteDao().putFolder(com.qingyi5427.ngaqing.data.local.ServerFavoriteFolderEntity(
            owner.uid, "7", "待读", 1, false
        ))
        db.serverFavoriteDao().putMembership(com.qingyi5427.ngaqing.data.local.ServerFavoriteMembershipEntity(
            owner.uid, "42", "7"
        ))
        coEvery { api.listServerFavoriteFolders(session = owner) } returnsMany listOf(
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"待读","length":1}},"1":0}}"""),
            Response.success("""{"data":{"0":{},"1":0}}""")
        )
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns threadPage("42")
        coEvery { signed.listTopics(owner, "7", any()) } returns signedPage("42")
        coEvery { signed.deleteFolder(owner, "7") } returns JSONObject("""{"code":"0","result":["操作成功"]}""")
        val selection = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 1, false)),
            setOf("7"), true)

        val confirmed = repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner).getOrThrow()
        assertTrue(confirmed.folders.isEmpty())
        assertTrue(confirmed.selectedIds.isEmpty())
        assertTrue(confirmed.hasLocalCopy)
        assertTrue(db.serverFavoriteDao().folders(owner.uid).isEmpty())
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "42").isEmpty())
        assertEquals("local", db.favoriteDao().get(owner.uid, "42")?.source)
        assertNotNull(db.favoriteDao().get(owner.uid, "99"))
        coVerify(exactly = 1) { signed.deleteFolder(owner, "7") }
        coVerify(exactly = 0) { signed.remove(any(), any(), any()) }
        db.close()
    }

    @Test fun changedLastFolderPreflightNeverDeletesFolder() = runBlocking {
        prefs.saveAuth("fav-last-changed", "cid", "A")
        val owner = repo.captureSession()
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"待读","length":1}},"1":0}}""")
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns
            threadPage("42", "43")
        val selection = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 1, false)),
            setOf("7"), false)
        assertTrue(repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner).isFailure)
        coVerify(exactly = 0) { signed.deleteFolder(any(), any()) }
        db.close()
    }

    @Test fun renamedOrMissingLastFolderNeverDeletesFolder() = runBlocking {
        prefs.saveAuth("fav-last-renamed", "cid", "A")
        val owner = repo.captureSession()
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"新名称","length":1}},"1":0}}""")
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns threadPage("42")
        val selection = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 1, false)),
            setOf("7"), false)
        assertTrue(repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner).isFailure)
        coVerify(exactly = 0) { signed.deleteFolder(any(), any()) }
        db.close()
    }

    @Test fun displayedFolderNameMustStillMatchCurrentSelectionBeforeDelete() = runBlocking {
        prefs.saveAuth("fav-last-display-name", "cid", "A")
        val owner = repo.captureSession()
        val selection = ServerFavoriteSelection(
            listOf(ServerFavoriteFolder("7", "新名称", 1, false)), setOf("7"), false
        )
        assertTrue(repo.removeLastFavoriteAndFolder("42", "7", "弹窗显示的旧名称", selection, owner).isFailure)
        coVerify(exactly = 0) { api.listServerFavoriteFolders(session = any()) }
        coVerify(exactly = 0) { signed.deleteFolder(any(), any()) }
        db.close()
    }

    @Test fun successMessageWithoutDeletedFolderKeepsCloudCache() = runBlocking {
        prefs.saveAuth("fav-last-noop", "cid", "A")
        val owner = repo.captureSession()
        db.serverFavoriteDao().putFolder(com.qingyi5427.ngaqing.data.local.ServerFavoriteFolderEntity(
            owner.uid, "7", "待读", 1, false
        ))
        db.serverFavoriteDao().putMembership(com.qingyi5427.ngaqing.data.local.ServerFavoriteMembershipEntity(
            owner.uid, "42", "7"
        ))
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"待读","length":1}},"1":0}}""")
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns threadPage("42")
        coEvery { signed.listTopics(owner, "7", any()) } returns signedPage("42")
        coEvery { signed.deleteFolder(owner, "7") } returns JSONObject("""{"code":"0","result":["操作成功"]}""")
        val selection = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 1, false)),
            setOf("7"), false)
        assertTrue(repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner).isFailure)
        assertEquals(setOf("7"), db.serverFavoriteDao().folders(owner.uid).map { it.folderId }.toSet())
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        coVerify(exactly = 1) { signed.deleteFolder(owner, "7") }
        db.close()
    }

    @Test fun accountChangeDuringLastFolderPreflightNeverDeletesFolder() = runBlocking {
        prefs.saveAuth("fav-last-account-a", "cid-a", "A")
        val owner = repo.captureSession()
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"待读","length":1}},"1":0}}""")
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns threadPage("42")
        coEvery { signed.listTopics(owner, "7", any()) } coAnswers {
            prefs.saveAuth("fav-last-account-b", "cid-b", "B")
            signedPage("42")
        }
        val selection = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 1, false)),
            setOf("7"), false)
        val outcome = runCatching {
            repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner)
        }
        assertTrue(outcome.isFailure)
        assertTrue(outcome.exceptionOrNull() is CancellationException)
        coVerify(exactly = 0) { signed.deleteFolder(any(), any()) }
        db.close()
    }

    @Test fun staleOrdinaryRemoveThatBecameLastItemDoesNotWrite() = runBlocking {
        prefs.saveAuth("fav-last-stale", "cid", "A")
        val owner = repo.captureSession()
        coEvery { api.listServerFavoriteFolders(session = owner) } returns
            Response.success("""{"data":{"0":{"7":{"id":"7","name":"待读","length":1}},"1":0}}""")
        coEvery { api.threadList(favor = 7, page = 1, session = owner) } returns threadPage("42")
        val stale = ServerFavoriteSelection(listOf(ServerFavoriteFolder("7", "待读", 2, false)),
            setOf("7"), false)
        val result = repo.removeFavorite("42", "7", stale, owner)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("重新读取并确认") == true)
        coVerify(exactly = 0) { signed.remove(any(), any(), any()) }
        coVerify(exactly = 0) { signed.deleteFolder(any(), any()) }
        db.close()
    }

    @Test fun incompletePostDeleteFolderListKeepsOtherCloudCache() = runBlocking {
        prefs.saveAuth("fav-last-partial", "cid", "A")
        val owner = repo.captureSession()
        mockTwoFolders()
        db.serverFavoriteDao().putFolder(com.qingyi5427.ngaqing.data.local.ServerFavoriteFolderEntity(
            owner.uid, "1", "默认", 1, true
        ))
        db.serverFavoriteDao().putFolder(com.qingyi5427.ngaqing.data.local.ServerFavoriteFolderEntity(
            owner.uid, "7", "待读", 1, false
        ))
        db.serverFavoriteDao().putMembership(com.qingyi5427.ngaqing.data.local.ServerFavoriteMembershipEntity(
            owner.uid, "42", "7"
        ))
        coEvery { api.listServerFavoriteFolders(session = owner) } returnsMany listOf(
            Response.success("""{"data":{"0":{"1":{"id":"1","name":"默认","length":1,"default":1},"7":{"id":"7","name":"待读","length":1}},"1":0}}"""),
            Response.success("""{"data":{"0":{},"1":0}}""")
        )
        coEvery { signed.listTopics(owner, "7", any()) } returns signedPage("42")
        coEvery { signed.deleteFolder(owner, "7") } returns JSONObject("""{"code":"0","result":["操作成功"]}""")
        val selection = ServerFavoriteSelection(listOf(
            ServerFavoriteFolder("1", "默认", 1, true), ServerFavoriteFolder("7", "待读", 1, false)
        ), setOf("7"), false)

        assertTrue(repo.removeLastFavoriteAndFolder("42", "7", "待读", selection, owner).isFailure)
        assertEquals(setOf("1", "7"), db.serverFavoriteDao().folders(owner.uid).map { it.folderId }.toSet())
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        coVerify(exactly = 1) { signed.deleteFolder(owner, "7") }
        db.close()
    }

    private fun mockTwoFolders() {
        coEvery { api.listServerFavoriteFolders(session = any()) } returns
            Response.success("""{"data":{"0":{"1":{"id":"1","name":"默认","length":1,"default":1},"7":{"id":"7","name":"待读","length":1}}}}""")
        coEvery { api.threadList(favor = 1, page = 1, session = any()) } returns threadPage("10")
        coEvery { api.threadList(favor = 7, page = 1, session = any()) } returns threadPage("42")
    }

    private fun threadPage(vararg tids: String): String {
        val items = tids.mapIndexed { index, tid ->
            """"$index":{"tid":"$tid","subject":"topic $tid","author":"author","authorid":"42","postdate":1,"replies":0}"""
        }.joinToString(",")
        return """{"data":{"__ROWS":${tids.size},"__T":{$items}}}"""
    }

    private fun signedPage(vararg tids: String): JSONObject {
        val data = tids.joinToString(",") { """{"tid":"$it"}""" }
        return JSONObject("""{"code":"0","result":{"data":[$data]},"totalPage":"1","total":"${tids.size}"}""")
    }

    private fun success(): String = """{"data":{"0":0,"1":"操作成功"}}"""
}
