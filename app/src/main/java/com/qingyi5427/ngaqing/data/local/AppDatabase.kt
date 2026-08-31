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

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val tid: String,
    val title: String,
    val fid: String,
    val author: String,
    val folder: String = "默认",
    val ts: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY ts DESC")
    fun all(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE tid = :tid LIMIT 1")
    suspend fun get(tid: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(f: FavoriteEntity)

    @Delete
    suspend fun delete(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE tid = :tid")
    suspend fun deleteByTid(tid: String)

    @Query("UPDATE favorites SET folder = :folder WHERE tid = :tid")
    suspend fun updateFolder(tid: String, folder: String)
}

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val tid: String,
    val title: String,
    val author: String,
    val fid: String,
    val lastVisited: Long = System.currentTimeMillis(),
    val lastFloor: Int = 0
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastVisited DESC")
    fun all(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE tid = :tid LIMIT 1")
    suspend fun get(tid: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity)

    @Query("UPDATE history SET lastFloor = :floor WHERE tid = :tid")
    suspend fun updateFloor(tid: String, floor: Int)

    @Query("DELETE FROM history WHERE tid = :tid")
    suspend fun deleteByTid(tid: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Entity(tableName = "favorite_boards")
data class FavoriteBoardEntity(
    @PrimaryKey val key: String,
    val fid: String,
    val stid: String,
    val name: String,
    val info: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "response_cache")
data class ResponseCacheEntity(
    @PrimaryKey val key: String,
    val payload: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ResponseCacheDao {
    @Query("SELECT * FROM response_cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): ResponseCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: ResponseCacheEntity)

    @Query("DELETE FROM response_cache")
    suspend fun clear()

    @Query("DELETE FROM response_cache WHERE updatedAt < :before")
    suspend fun prune(before: Long)
}

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val key: String,
    val kind: String,
    val tid: String = "",
    val fid: String = "",
    val stid: String = "",
    val targetPid: String = "",
    val targetAuthor: String = "",
    val targetFloor: Int = 0,
    val subject: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): DraftEntity?

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun all(): Flow<List<DraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: DraftEntity)

    @Query("DELETE FROM drafts WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Entity(tableName = "watched_threads")
data class WatchedThreadEntity(
    @PrimaryKey val tid: String,
    val title: String,
    val fid: String,
    val lastKnownReplies: Int = 0,
    val lastSeenReplies: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface WatchedThreadDao {
    @Query("SELECT * FROM watched_threads ORDER BY updatedAt DESC")
    fun all(): Flow<List<WatchedThreadEntity>>

    @Query("SELECT * FROM watched_threads WHERE tid = :tid LIMIT 1")
    suspend fun get(tid: String): WatchedThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: WatchedThreadEntity)

    @Query("DELETE FROM watched_threads WHERE tid = :tid")
    suspend fun delete(tid: String)
}

@Dao
interface FavoriteBoardDao {
    @Query("SELECT * FROM favorite_boards ORDER BY addedAt DESC")
    fun all(): Flow<List<FavoriteBoardEntity>>

    @Query("SELECT * FROM favorite_boards WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): FavoriteBoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteBoardEntity)

    @Query("UPDATE favorite_boards SET addedAt = :orderValue WHERE `key` = :key")
    suspend fun updateOrder(key: String, orderValue: Long)

    @Query("DELETE FROM favorite_boards WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
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
    version = 3,
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
