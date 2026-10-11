package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opens an actual v4 database through Room v5 so migration SQL and schema validation both run. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FavoriteMigrationTest {
    @Test fun versionFourUpgradeKeepsEveryExistingRowAndMarksOldFavoritesLocal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "favorite_v4_to_v5.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        versionFourTables.forEach(db::execSQL)
                        versionFourRows.forEach(db::execSQL)
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val before = helper.writableDatabase.let { db ->
            oldColumnQueries.mapValues { (_, sql) -> queryRows(db, sql) }
        }
        helper.close()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(favoriteMigration4To5())
            .allowMainThreadQueries()
            .build()
        try {
            val db = room.openHelper.writableDatabase
            oldColumnQueries.forEach { (table, sql) ->
                assertEquals("v4 $table rows changed", before.getValue(table), queryRows(db, sql))
            }
            assertEquals(
                listOf("account-a|77|科技收藏|local", "account-a|88|仅本地|local", "account-b|77|另一账户|local"),
                queryRows(db, "SELECT ownerUid,tid,folder,source FROM favorites ORDER BY ownerUid,tid")
            )
            assertEquals(emptyList<String>(), queryRows(db, "SELECT * FROM server_favorite_folders"))
            assertEquals(emptyList<String>(), queryRows(db, "SELECT * FROM server_favorite_memberships"))
        } finally {
            room.close()
        }
    }

    @Test fun newMembershipKeySupportsTwoFoldersWithoutMixingAccountsOrLocalRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val room = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val db = room.openHelper.writableDatabase
            db.execSQL("INSERT INTO server_favorite_folders VALUES ('account-a','10','阅读',2,1),('account-a','20','视频',1,0),('account-b','10','乙目录',1,1)")
            db.execSQL("INSERT INTO server_favorite_memberships VALUES ('account-a','77','10'),('account-a','77','20'),('account-b','77','10')")
            assertEquals(
                listOf("10", "20"),
                queryRows(db, "SELECT folderId FROM server_favorite_memberships WHERE ownerUid='account-a' AND tid='77' ORDER BY folderId")
            )
            db.execSQL("DELETE FROM server_favorite_memberships WHERE ownerUid='account-a' AND tid='77' AND folderId='10'")
            assertEquals(
                listOf("account-a|77|20", "account-b|77|10"),
                queryRows(db, "SELECT ownerUid,tid,folderId FROM server_favorite_memberships ORDER BY ownerUid,folderId")
            )
        } finally {
            room.close()
        }
    }

    private fun queryRows(db: SupportSQLiteDatabase, sql: String): List<String> = db.query(sql).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add((0 until cursor.columnCount).joinToString("|") { cursor.getString(it) })
            }
        }
    }

    private val oldColumnQueries = linkedMapOf(
        "favorites" to "SELECT ownerUid,tid,title,fid,author,folder,ts FROM favorites ORDER BY ownerUid,tid",
        "history" to "SELECT ownerUid,tid,title,author,fid,lastVisited,lastFloor FROM history ORDER BY ownerUid,tid",
        "favorite_boards" to "SELECT ownerUid,`key`,fid,stid,name,info,addedAt FROM favorite_boards ORDER BY ownerUid,`key`",
        "response_cache" to "SELECT ownerUid,`key`,payload,updatedAt FROM response_cache ORDER BY ownerUid,`key`",
        "drafts" to "SELECT ownerUid,`key`,kind,tid,fid,stid,targetPid,targetAuthor,targetFloor,subject,content,updatedAt FROM drafts ORDER BY ownerUid,`key`",
        "watched_threads" to "SELECT ownerUid,tid,title,fid,lastKnownReplies,lastSeenReplies,updatedAt FROM watched_threads ORDER BY ownerUid,tid"
    )

    private val versionFourTables = listOf(
        "CREATE TABLE favorites (ownerUid TEXT NOT NULL,tid TEXT NOT NULL,title TEXT NOT NULL,fid TEXT NOT NULL,author TEXT NOT NULL,folder TEXT NOT NULL,ts INTEGER NOT NULL,PRIMARY KEY(ownerUid,tid))",
        "CREATE TABLE history (ownerUid TEXT NOT NULL,tid TEXT NOT NULL,title TEXT NOT NULL,author TEXT NOT NULL,fid TEXT NOT NULL,lastVisited INTEGER NOT NULL,lastFloor INTEGER NOT NULL,PRIMARY KEY(ownerUid,tid))",
        "CREATE TABLE favorite_boards (ownerUid TEXT NOT NULL,`key` TEXT NOT NULL,fid TEXT NOT NULL,stid TEXT NOT NULL,name TEXT NOT NULL,info TEXT NOT NULL,addedAt INTEGER NOT NULL,PRIMARY KEY(ownerUid,`key`))",
        "CREATE TABLE response_cache (ownerUid TEXT NOT NULL,`key` TEXT NOT NULL,payload TEXT NOT NULL,updatedAt INTEGER NOT NULL,PRIMARY KEY(ownerUid,`key`))",
        "CREATE TABLE drafts (ownerUid TEXT NOT NULL,`key` TEXT NOT NULL,kind TEXT NOT NULL,tid TEXT NOT NULL,fid TEXT NOT NULL,stid TEXT NOT NULL,targetPid TEXT NOT NULL,targetAuthor TEXT NOT NULL,targetFloor INTEGER NOT NULL,subject TEXT NOT NULL,content TEXT NOT NULL,updatedAt INTEGER NOT NULL,PRIMARY KEY(ownerUid,`key`))",
        "CREATE TABLE watched_threads (ownerUid TEXT NOT NULL,tid TEXT NOT NULL,title TEXT NOT NULL,fid TEXT NOT NULL,lastKnownReplies INTEGER NOT NULL,lastSeenReplies INTEGER NOT NULL,updatedAt INTEGER NOT NULL,PRIMARY KEY(ownerUid,tid))"
    )

    private val versionFourRows = listOf(
        "INSERT INTO favorites VALUES ('account-a','77','甲帖','10','甲','科技收藏',123),('account-a','88','本地帖','20','甲','仅本地',124),('account-b','77','乙帖','30','乙','另一账户',125)",
        "INSERT INTO history VALUES ('account-a','77','读过','甲','10',901,5)",
        "INSERT INTO favorite_boards VALUES ('account-a','fid:10','10','','版块','说明',902)",
        "INSERT INTO response_cache VALUES ('account-a','posts:77','正文缓存',903)",
        "INSERT INTO drafts VALUES ('account-a','reply:77','reply','77','10','','','','0','标题','草稿',904)",
        "INSERT INTO watched_threads VALUES ('account-a','77','关注帖','10',3,2,905)"
    )
}
