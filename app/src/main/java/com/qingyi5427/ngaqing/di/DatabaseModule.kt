package com.qingyi5427.ngaqing.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.FavoriteDao
import com.qingyi5427.ngaqing.data.local.FavoriteBoardDao
import com.qingyi5427.ngaqing.data.local.HistoryDao
import com.qingyi5427.ngaqing.data.local.ResponseCacheDao
import com.qingyi5427.ngaqing.data.local.DraftDao
import com.qingyi5427.ngaqing.data.local.WatchedThreadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `history` (`tid` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `fid` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, `lastFloor` INTEGER NOT NULL, PRIMARY KEY(`tid`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `favorite_boards` (`key` TEXT NOT NULL, `fid` TEXT NOT NULL, `stid` TEXT NOT NULL, `name` TEXT NOT NULL, `info` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))"""
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `favorites` ADD COLUMN `folder` TEXT NOT NULL DEFAULT '默认'")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `response_cache` (`key` TEXT NOT NULL, `payload` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `drafts` (`key` TEXT NOT NULL, `kind` TEXT NOT NULL, `tid` TEXT NOT NULL, `fid` TEXT NOT NULL, `stid` TEXT NOT NULL, `targetPid` TEXT NOT NULL, `targetAuthor` TEXT NOT NULL, `targetFloor` INTEGER NOT NULL, `subject` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `watched_threads` (`tid` TEXT NOT NULL, `title` TEXT NOT NULL, `fid` TEXT NOT NULL, `lastKnownReplies` INTEGER NOT NULL, `lastSeenReplies` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`tid`))"""
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideFavoriteBoardDao(db: AppDatabase): FavoriteBoardDao = db.favoriteBoardDao()

    @Provides
    fun provideResponseCacheDao(db: AppDatabase): ResponseCacheDao = db.responseCacheDao()

    @Provides
    fun provideDraftDao(db: AppDatabase): DraftDao = db.draftDao()

    @Provides
    fun provideWatchedThreadDao(db: AppDatabase): WatchedThreadDao = db.watchedThreadDao()
}
