package com.qingyi5427.ngaqing.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites", primaryKeys = ["ownerUid", "tid"])
data class FavoriteEntity(
    val tid: String,
    val title: String,
    val fid: String,
    val author: String,
    val folder: String = "默认",
    val ts: Long = System.currentTimeMillis(),
    val ownerUid: String = ""
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE ownerUid = :ownerUid ORDER BY ts DESC")
    fun all(ownerUid: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE ownerUid = :ownerUid AND tid = :tid LIMIT 1")
    suspend fun get(ownerUid: String, tid: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(f: FavoriteEntity)

    @Delete
    suspend fun delete(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE ownerUid = :ownerUid AND tid = :tid")
    suspend fun deleteByTid(ownerUid: String, tid: String)

    @Query("UPDATE favorites SET folder = :folder WHERE ownerUid = :ownerUid AND tid = :tid")
    suspend fun updateFolder(ownerUid: String, tid: String, folder: String)
}

@Entity(tableName = "history", primaryKeys = ["ownerUid", "tid"])
data class HistoryEntity(
    val tid: String,
    val title: String,
    val author: String,
    val fid: String,
    val lastVisited: Long = System.currentTimeMillis(),
    val lastFloor: Int = 0,
    val ownerUid: String = ""
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE ownerUid = :ownerUid ORDER BY lastVisited DESC")
    fun all(ownerUid: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE ownerUid = :ownerUid AND tid = :tid LIMIT 1")
    suspend fun get(ownerUid: String, tid: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity)

    @Query("UPDATE history SET lastFloor = :floor WHERE ownerUid = :ownerUid AND tid = :tid")
    suspend fun updateFloor(ownerUid: String, tid: String, floor: Int)

    @Query("DELETE FROM history WHERE ownerUid = :ownerUid AND tid = :tid")
    suspend fun deleteByTid(ownerUid: String, tid: String)

    @Query("DELETE FROM history WHERE ownerUid = :ownerUid")
    suspend fun clear(ownerUid: String)
}

@Entity(tableName = "favorite_boards", primaryKeys = ["ownerUid", "key"])
data class FavoriteBoardEntity(
    val key: String,
    val fid: String,
    val stid: String,
    val name: String,
    val info: String,
    val addedAt: Long = System.currentTimeMillis(),
    val ownerUid: String = ""
)

@Entity(tableName = "response_cache", primaryKeys = ["ownerUid", "key"])
data class ResponseCacheEntity(
    val key: String,
    val payload: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerUid: String = ""
)

@Dao
interface ResponseCacheDao {
    @Query("SELECT * FROM response_cache WHERE ownerUid = :ownerUid AND `key` = :key LIMIT 1")
    suspend fun get(ownerUid: String, key: String): ResponseCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: ResponseCacheEntity)

    @Query("DELETE FROM response_cache WHERE ownerUid = :ownerUid")
    suspend fun clear(ownerUid: String)

    @Query("DELETE FROM response_cache WHERE ownerUid = :ownerUid AND updatedAt < :before")
    suspend fun prune(ownerUid: String, before: Long)
}

@Entity(tableName = "drafts", primaryKeys = ["ownerUid", "key"])
data class DraftEntity(
    val key: String,
    val kind: String,
    val tid: String = "",
    val fid: String = "",
    val stid: String = "",
    val targetPid: String = "",
    val targetAuthor: String = "",
    val targetFloor: Int = 0,
    val subject: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerUid: String = ""
)

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE ownerUid = :ownerUid AND `key` = :key LIMIT 1")
    suspend fun get(ownerUid: String, key: String): DraftEntity?

    @Query("SELECT * FROM drafts WHERE ownerUid = :ownerUid ORDER BY updatedAt DESC")
    fun all(ownerUid: String): Flow<List<DraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: DraftEntity)

    @Query("DELETE FROM drafts WHERE ownerUid = :ownerUid AND `key` = :key")
    suspend fun delete(ownerUid: String, key: String)
}

@Entity(tableName = "watched_threads", primaryKeys = ["ownerUid", "tid"])
data class WatchedThreadEntity(
    val tid: String,
    val title: String,
    val fid: String,
    val lastKnownReplies: Int = 0,
    val lastSeenReplies: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerUid: String = ""
)

@Dao
interface WatchedThreadDao {
    @Query("SELECT * FROM watched_threads WHERE ownerUid = :ownerUid ORDER BY updatedAt DESC")
    fun all(ownerUid: String): Flow<List<WatchedThreadEntity>>

    @Query("SELECT * FROM watched_threads WHERE ownerUid = :ownerUid AND tid = :tid LIMIT 1")
    suspend fun get(ownerUid: String, tid: String): WatchedThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: WatchedThreadEntity)

    @Query("DELETE FROM watched_threads WHERE ownerUid = :ownerUid AND tid = :tid")
    suspend fun delete(ownerUid: String, tid: String)
}

@Dao
interface FavoriteBoardDao {
    @Query("SELECT * FROM favorite_boards WHERE ownerUid = :ownerUid ORDER BY addedAt DESC")
    fun all(ownerUid: String): Flow<List<FavoriteBoardEntity>>

    @Query("SELECT * FROM favorite_boards WHERE ownerUid = :ownerUid AND `key` = :key LIMIT 1")
    suspend fun get(ownerUid: String, key: String): FavoriteBoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteBoardEntity)

    @Query("UPDATE favorite_boards SET addedAt = :orderValue WHERE ownerUid = :ownerUid AND `key` = :key")
    suspend fun updateOrder(ownerUid: String, key: String, orderValue: Long)

    @Query("DELETE FROM favorite_boards WHERE ownerUid = :ownerUid AND `key` = :key")
    suspend fun deleteByKey(ownerUid: String, key: String)
}

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        FavoriteBoardEntity::class,
        ResponseCacheEntity::class,
        DraftEntity::class,
        WatchedThreadEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteBoardDao(): FavoriteBoardDao
    abstract fun responseCacheDao(): ResponseCacheDao
    abstract fun draftDao(): DraftDao
    abstract fun watchedThreadDao(): WatchedThreadDao

    companion object {
        const val NAME = "nga_client.db"
    }
}
