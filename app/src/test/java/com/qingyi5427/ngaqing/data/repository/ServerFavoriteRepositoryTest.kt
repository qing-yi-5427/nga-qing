package com.qingyi5427.ngaqing.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.FavoriteEntity
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.NgaApi
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
    private val repo = NgaRepository(api, prefs, db, context)

    @Test fun legacyLocalCopySurvivesCloudSelectionAndFolderSpecificRemoval() = runBlocking {
        prefs.saveAuth("fav-legacy-a", "cid-a", "A")
        val owner = repo.captureSession()
        db.favoriteDao().insert(FavoriteEntity(
            tid = "42", title = "saved locally", fid = "9", author = "A",
            folder = "待读", ts = 1234L, ownerUid = owner.uid
        ))
        mockTwoFolders()
        val initial = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        assertEquals(setOf("7"), initial.selectedIds)
        assertTrue(initial.hasLocalCopy)
        assertEquals("待读", db.favoriteDao().get(owner.uid, "42")?.folder)

        coEvery { api.addServerFavorite(folder = "1", tid = "42", session = owner) } returns success()
        assertTrue(repo.addFavorite("42", "remote title", folderId = "1", selection = initial, session = owner).isSuccess)
        coVerify(exactly = 1) { api.addServerFavorite(folder = "1", tid = "42", session = owner) }
        val selectedBoth = initial.copy(selectedIds = setOf("1", "7"))
        assertEquals(setOf("1", "7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())

        coEvery { api.removeServerFavorite(folder = any(), tidArray = "42", session = owner) } returns success()
        assertTrue(repo.removeFavorite("42", "7", selectedBoth, owner).isSuccess)
        coVerify(exactly = 1) { api.removeServerFavorite(folder = "7", tidArray = "42", session = owner) }
        assertEquals(setOf("1"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        assertTrue(repo.removeFavorite("42", "1", initial.copy(selectedIds = setOf("1")), owner).isSuccess)
        coVerify(exactly = 1) { api.removeServerFavorite(folder = "1", tidArray = "42", session = owner) }
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
        val imported = repo.syncServerFavorites(owner).getOrThrow()
        assertEquals(2, imported)
        val remote = db.favoriteDao().get(owner.uid, "42")
        assertEquals("server", remote?.source)
        assertEquals(setOf("7"), db.serverFavoriteDao().memberships(owner.uid, "42").map { it.folderId }.toSet())
        val initial = repo.loadServerFavoriteSelection("42", owner).getOrThrow()
        coEvery { api.addServerFavorite(folder = "1", tid = "42", session = owner) } returns success()
        assertTrue(repo.addFavorite("42", "title", folderId = "1", selection = initial, session = owner).isSuccess)
        coEvery { api.removeServerFavorite(folder = any(), tidArray = "42", session = owner) } returns success()
        assertTrue(repo.removeFavorite("42", "7", initial.copy(selectedIds = setOf("7", "1")), owner).isSuccess)
        assertNotNull(db.favoriteDao().get(owner.uid, "42"))
        assertTrue(repo.removeFavorite("42", "1", initial.copy(selectedIds = setOf("1")), owner).isSuccess)
        assertNull(db.favoriteDao().get(owner.uid, "42"))
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
        coEvery { api.addServerFavorite(folder = "1", tid = "99", session = owner) } returns
            """{"error":{"0":"not allowed"}}"""
        assertTrue(repo.addFavorite("99", "title", folderId = "1", selection = selection, session = owner).isFailure)
        assertNull(db.favoriteDao().get(owner.uid, "99"))
        assertTrue(db.serverFavoriteDao().memberships(owner.uid, "99").isEmpty())
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
        val response = CompletableDeferred<String>()
        coEvery { api.addServerFavorite(folder = "1", tid = "99", session = owner) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        val pending = async {
            runCatching { repo.addFavorite("99", "title", folderId = "1", selection = selection, session = owner) }
        }
        started.await()
        prefs.saveAuth("fav-revision-b", "cid-b", "B")
        assertTrue(prefs.switchAccount("fav-revision-a"))
        response.complete(success())
        assertTrue(pending.await().exceptionOrNull() is CancellationException)
        assertNull(db.favoriteDao().get("fav-revision-a", "99"))
        assertNull(db.favoriteDao().get("fav-revision-b", "99"))
        assertTrue(db.serverFavoriteDao().memberships("fav-revision-a", "99").isEmpty())
        db.close()
    }

    private fun mockTwoFolders() {
        coEvery { api.listServerFavoriteFolders(session = any()) } returns
            Response.success("""{"data":{"0":{"1":{"id":"1","name":"默认","length":1,"default":1},"7":{"id":"7","name":"待读","length":1}}}}""")
        coEvery { api.threadList(favor = 1, page = 1, session = any()) } returns threadPage("10")
        coEvery { api.threadList(favor = 7, page = 1, session = any()) } returns threadPage("42")
    }

    private fun threadPage(tid: String): String =
        """{"data":{"__ROWS":1,"__T":{"0":{"tid":"$tid","subject":"topic $tid","author":"author","authorid":"42","postdate":1,"replies":0}}}}"""

    private fun success(): String = """{"data":{"0":0,"1":"操作成功"}}"""
}
