package com.qingyi5427.ngaqing.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.HistoryDao
import com.qingyi5427.ngaqing.data.local.HistoryEntity
import com.qingyi5427.ngaqing.data.local.RequestPreferences
import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.remote.NgaApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Forces the old metadata read to overlap a newer reading-position write. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryWriteOrderTest {
    @Test fun invalidatedAccountRevisionCannotInsertHistoryRow() = runBlocking {
        val stale = RequestPreferences("owner", "cid", "bbs.nga.cn", 17)
        val prefs = mockk<UserPreferences>()
        coEvery { prefs.requestPreferences() } returns stale.copy(revision = 18)
        val dao = mockk<HistoryDao>(relaxed = true)
        val db = mockk<AppDatabase>()
        every { db.historyDao() } returns dao
        val repo = NgaRepository(mockk<NgaApi>(), prefs, db,
            ApplicationProvider.getApplicationContext<Context>())

        assertThrows(CancellationException::class.java) {
            runBlocking { repo.recordHistory("tid", "title", "author", "fid", stale) }
        }
        coVerify(exactly = 0) { dao.insertIfAbsent(any()) }
        coVerify(exactly = 0) { dao.updateMetadata(any(), any(), any(), any(), any(), any()) }
    }

    @Test fun realRoomMetadataRefreshPreservesFloorAndNonblankFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val session = RequestPreferences("owner", "cid", "bbs.nga.cn", 17)
        val prefs = mockk<UserPreferences>()
        coEvery { prefs.requestPreferences() } returns session
        try {
            db.historyDao().insert(HistoryEntity("tid", "old title", "old author", "old fid",
                lastFloor = 5, ownerUid = "owner"))
            val repo = NgaRepository(mockk<NgaApi>(), prefs, db, context)
            repo.recordHistory("tid", "new title", "   ", "", session)
            val saved = db.historyDao().get("owner", "tid")!!
            assertEquals(5, saved.lastFloor)
            assertEquals("new title", saved.title)
            assertEquals("old author", saved.author)
            assertEquals("old fid", saved.fid)
        } finally {
            db.close()
        }
    }

    @Test fun invalidatedAccountRevisionCannotWriteReadingFloor() = runBlocking {
        val stale = RequestPreferences("owner", "cid", "bbs.nga.cn", 17)
        val prefs = mockk<UserPreferences>()
        coEvery { prefs.requestPreferences() } returns stale.copy(revision = 18)
        val dao = mockk<HistoryDao>(relaxed = true)
        val db = mockk<AppDatabase>()
        every { db.historyDao() } returns dao
        val repo = NgaRepository(mockk<NgaApi>(), prefs, db,
            ApplicationProvider.getApplicationContext<Context>())

        assertThrows(CancellationException::class.java) {
            runBlocking { repo.updateHistoryFloor("tid", 5, stale) }
        }
        coVerify(exactly = 0) { dao.updateFloor(any(), any(), any()) }
    }

    @Test fun metadataRefreshCannotReplaceNewerReadingFloor() = runBlocking {
        val session = RequestPreferences("owner", "cid", "bbs.nga.cn", 17)
        val prefs = mockk<UserPreferences>()
        coEvery { prefs.requestPreferences() } returns session
        val dao = mockk<HistoryDao>()
        val db = mockk<AppDatabase>()
        every { db.historyDao() } returns dao
        var stored = HistoryEntity("tid", "old title", "author", "fid", lastFloor = 0,
            ownerUid = "owner")
        val readOld = CompletableDeferred<Unit>()
        val allowMetadataWrite = CompletableDeferred<Unit>()
        coEvery { dao.get("owner", "tid") } coAnswers {
            val snapshot = stored
            readOld.complete(Unit)
            allowMetadataWrite.await()
            snapshot
        }
        coEvery { dao.insert(any()) } coAnswers {
            stored = firstArg()
        }
        coEvery { dao.insertIfAbsent(any()) } returns Unit
        coEvery { dao.updateMetadata("owner", "tid", any(), any(), any(), any()) } coAnswers {
            readOld.complete(Unit)
            allowMetadataWrite.await()
            stored = stored.copy(title = thirdArg())
        }
        coEvery { dao.updateFloor("owner", "tid", any()) } coAnswers {
            stored = stored.copy(lastFloor = thirdArg())
        }
        val repo = NgaRepository(mockk<NgaApi>(), prefs, db,
            ApplicationProvider.getApplicationContext<Context>())

        val metadata = async {
            repo.recordHistory("tid", "new title", "author", "fid", session)
        }
        readOld.await()
        repo.updateHistoryFloor("tid", 5, session)
        allowMetadataWrite.complete(Unit)
        metadata.await()

        assertEquals("new title", stored.title)
        assertEquals("metadata refresh must preserve the already stored floor", 5, stored.lastFloor)
    }
}
