package com.qingyi5427.ngaqing.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Rows whose previous owner cannot be established stay recoverable but invisible to every account. */
internal const val UNASSIGNED_LEGACY_OWNER = "__legacy_owner_unknown__"

internal fun accountMigration3To4(currentUid: () -> String): Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val owner = currentUid().ifBlank { UNASSIGNED_LEGACY_OWNER }
        val definitions = listOf(
            TableDefinition("favorites", "tid", "`tid` TEXT NOT NULL, `title` TEXT NOT NULL, `fid` TEXT NOT NULL, `author` TEXT NOT NULL, `folder` TEXT NOT NULL, `ts` INTEGER NOT NULL", "tid,title,fid,author,folder,ts"),
            TableDefinition("history", "tid", "`tid` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `fid` TEXT NOT NULL, `lastVisited` INTEGER NOT NULL, `lastFloor` INTEGER NOT NULL", "tid,title,author,fid,lastVisited,lastFloor"),
            TableDefinition("favorite_boards", "key", "`key` TEXT NOT NULL, `fid` TEXT NOT NULL, `stid` TEXT NOT NULL, `name` TEXT NOT NULL, `info` TEXT NOT NULL, `addedAt` INTEGER NOT NULL", "`key`,fid,stid,name,info,addedAt"),
            TableDefinition("response_cache", "key", "`key` TEXT NOT NULL, `payload` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL", "`key`,payload,updatedAt"),
            TableDefinition("drafts", "key", "`key` TEXT NOT NULL, `kind` TEXT NOT NULL, `tid` TEXT NOT NULL, `fid` TEXT NOT NULL, `stid` TEXT NOT NULL, `targetPid` TEXT NOT NULL, `targetAuthor` TEXT NOT NULL, `targetFloor` INTEGER NOT NULL, `subject` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL", "`key`,kind,tid,fid,stid,targetPid,targetAuthor,targetFloor,subject,content,updatedAt"),
            TableDefinition("watched_threads", "tid", "`tid` TEXT NOT NULL, `title` TEXT NOT NULL, `fid` TEXT NOT NULL, `lastKnownReplies` INTEGER NOT NULL, `lastSeenReplies` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL", "tid,title,fid,lastKnownReplies,lastSeenReplies,updatedAt")
        )
        definitions.forEach { table ->
            db.execSQL("ALTER TABLE `${table.name}` RENAME TO `${table.name}_legacy_v3`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `${table.name}` (`ownerUid` TEXT NOT NULL, ${table.fields}, PRIMARY KEY(`ownerUid`, `${table.key}`))")
            db.execSQL("INSERT INTO `${table.name}` (`ownerUid`, ${table.columns}) SELECT ?, ${table.columns} FROM `${table.name}_legacy_v3`", arrayOf(owner))
            db.execSQL("DROP TABLE `${table.name}_legacy_v3`")
        }
    }
}

private data class TableDefinition(val name: String, val key: String, val fields: String, val columns: String)
