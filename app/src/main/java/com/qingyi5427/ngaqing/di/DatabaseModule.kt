package com.qingyi5427.ngaqing.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qingyi5427.ngaqing.data.local.AppDatabase
import com.qingyi5427.ngaqing.data.local.FavoriteDao
import com.qingyi5427.ngaqing.data.local.FavoriteBoardDao
import com.qingyi5427.ngaqing.data.local.HistoryDao
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        ).addMigrations(MIGRATION_1_2).build()
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
}
