package com.qingyi5427.ngaqing.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.DraftEntity
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.NgaApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val prefs = UserPreferences(context)
    private val api = mockk<NgaApi>()
    private val repo = NgaRepository(api, prefs, db, context)

    @Test fun sameLocalKeysRemainIndependentAcrossAccounts() = runBlocking {
        prefs.saveAuth("account-a", "cid-a", "A")
        coEvery { api.addServerFavorite(tid = any(), session = any()) } returns
            """{"data":{"0":0,"1":"收藏成功"}}"""
        coEvery { api.threadList(fid = "10", page = 1, session = any()) } returns threadPage("1", "A")
        repo.saveDraft(DraftEntity(key = "reply:1", kind = "reply", content = "A draft"))
        assertTrue(repo.addFavorite("1", "A favorite").isSuccess)
        repo.getThreads(fid = "10")
        val cacheKey = "threads:10::1::false:false"
        withTimeout(3_000) {
            while (db.responseCacheDao().get("account-a", cacheKey) == null) delay(10)
        }

        prefs.saveAuth("account-b", "cid-b", "B")
        assertNull(repo.draft("reply:1"))
        assertFalse(repo.isFavorite("1"))
        assertNull(db.responseCacheDao().get("account-b", cacheKey))
        repo.saveDraft(DraftEntity(key = "reply:1", kind = "reply", content = "B draft"))
        assertTrue(repo.addFavorite("1", "B favorite").isSuccess)
        coEvery { api.threadList(fid = "10", page = 1, session = any()) } returns threadPage("1", "B")
        repo.getThreads(fid = "10")
        withTimeout(3_000) {
            while (db.responseCacheDao().get("account-b", cacheKey) == null) delay(10)
        }
        assertEquals("B draft", repo.draft("reply:1")?.content)
        assertEquals("B favorite", repo.favorites().first().single().title)

        assertTrue(prefs.switchAccount("account-a"))
        assertEquals("A draft", repo.draft("reply:1")?.content)
        assertEquals("A favorite", repo.favorites().first().single().title)
        assertTrue(db.responseCacheDao().get("account-a", cacheKey)!!.payload.contains("A"))
        assertTrue(db.responseCacheDao().get("account-b", cacheKey)!!.payload.contains("B"))
        db.close()
    }

    @Test fun stalePublishAfterRoundTripCannotConfirmOrDeleteDraft() = runBlocking {
        prefs.saveAuth("account-a", "cid-a", "A")
        val session = repo.captureSession()
        repo.saveDraft(DraftEntity(key = "reply:1", kind = "reply", content = "keep me"), session)
        val requestStarted = CompletableDeferred<Unit>()
        val response = CompletableDeferred<String>()
        coEvery { api.publish(action = "reply", fid = "10", tid = "1", content = "text", session = any()) } coAnswers {
            requestStarted.complete(Unit)
            response.await()
        }
        val pending = async {
            runCatching { repo.publish(action = "reply", fid = "10", tid = "1", content = "text", session = session) }
        }
        requestStarted.await()
        prefs.saveAuth("account-b", "cid-b", "B")
        assertTrue(prefs.switchAccount("account-a"))
        response.complete("""{"data":{"0":0,"1":"回复成功"}}""")
        val result = pending.await()
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals("keep me", repo.draft("reply:1")?.content)
        db.close()
    }

    @Test fun failedSecondFavoritePageDoesNotPartiallyImport() = runBlocking {
        prefs.saveAuth("account-a", "cid-a", "A")
        coEvery { api.threadList(page = 1, favor = 1, session = any()) } returns threadPage("1", "one", total = 2)
        coEvery { api.threadList(page = 2, favor = 1, session = any()) } returns
            """{"error":{"0":"2:服务端失败"}}"""
        val result = repo.syncServerFavorites()
        assertTrue(result.isFailure)
        assertTrue(repo.favorites().first().isEmpty())
        db.close()
    }

    private fun threadPage(tid: String, title: String, total: Int = 1): String =
        """{"data":{"__ROWS":$total,"__T":{"0":{"tid":"$tid","subject":"$title","author":"author","authorid":"42","postdate":1,"replies":0}}}}"""
}
