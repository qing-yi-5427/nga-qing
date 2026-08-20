package com.ngaclient.app.data.local

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

@Dao
interface FavoriteBoardDao {
    @Query("SELECT * FROM favorite_boards ORDER BY addedAt DESC")
    fun all(): Flow<List<FavoriteBoardEntity>>

    @Query("SELECT * FROM favorite_boards WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): FavoriteBoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteBoardEntity)

    @Query("DELETE FROM favorite_boards WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, FavoriteBoardEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteBoardDao(): FavoriteBoardDao

    companion object {
        const val NAME = "nga_client.db"
    }
}
