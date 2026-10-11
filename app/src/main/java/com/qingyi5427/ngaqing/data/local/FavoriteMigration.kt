package com.qingyi5427.ngaqing.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve every existing local label and favorite while introducing account-scoped NGA folders. */
internal fun favoriteMigration4To5(): Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'local'")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `server_favorite_folders` (`ownerUid` TEXT NOT NULL, `folderId` TEXT NOT NULL, `name` TEXT NOT NULL, `count` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`ownerUid`, `folderId`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `server_favorite_memberships` (`ownerUid` TEXT NOT NULL, `tid` TEXT NOT NULL, `folderId` TEXT NOT NULL, PRIMARY KEY(`ownerUid`, `tid`, `folderId`))""")
    }
}
