package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountMigrationTest {
    @Test fun migrationKeepsEveryRowAndAllowsSameKeysForDifferentAccounts() {
        val db = migrate("account_owned.db", "account-a")
        try {
            tables.forEach { table ->
                assertEquals(2, count(db, table, "account-a"))
                assertEquals(0, count(db, table, "account-b"))
                db.execSQL("INSERT INTO `$table` SELECT 'account-b', ${columns.getValue(table)} FROM `$table` WHERE ownerUid = 'account-a' LIMIT 1")
                assertEquals(2, count(db, table, "account-a"))
                assertEquals(1, count(db, table, "account-b"))
            }
        } finally {
            db.close()
        }
    }

    @Test fun unknownOwnerRemainsPreservedAndInvisibleToLaterAccounts() {
        val db = migrate("account_unknown.db", "")
        try {
            tables.forEach { table ->
                assertEquals(2, count(db, table, UNASSIGNED_LEGACY_OWNER))
                assertEquals(0, count(db, table, "account-a"))
            }
        } finally {
            db.close()
        }
    }

    private fun migrate(name: String, owner: String): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        schemas.forEach(db::execSQL)
                        rows.forEach(db::execSQL)
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(accountMigration3To4 { owner })
            .allowMainThreadQueries()
            .build()
        val migrated = room.openHelper.writableDatabase
        // Database stays open for direct SQL assertions; closing this connection closes the helper.
        return migrated
    }

    private fun count(db: SupportSQLiteDatabase, table: String, owner: String): Int =
        db.query("SELECT COUNT(*) FROM `$table` WHERE ownerUid = ?", arrayOf(owner)).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private val tables = listOf("favorites", "history", "favorite_boards", "response_cache", "drafts", "watched_threads")
    private val columns = mapOf(
        "favorites" to "tid,title,fid,author,folder,ts",
        "history" to "tid,title,author,fid,lastVisited,lastFloor",
        "favorite_boards" to "`key`,fid,stid,name,info,addedAt",
        "response_cache" to "`key`,payload,updatedAt",
        "drafts" to "`key`,kind,tid,fid,stid,targetPid,targetAuthor,targetFloor,subject,content,updatedAt",
        "watched_threads" to "tid,title,fid,lastKnownReplies,lastSeenReplies,updatedAt"
    )
    private val schemas = listOf(
        "CREATE TABLE favorites (tid TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,fid TEXT NOT NULL,author TEXT NOT NULL,folder TEXT NOT NULL,ts INTEGER NOT NULL)",
        "CREATE TABLE history (tid TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,author TEXT NOT NULL,fid TEXT NOT NULL,lastVisited INTEGER NOT NULL,lastFloor INTEGER NOT NULL)",
        "CREATE TABLE favorite_boards (`key` TEXT NOT NULL PRIMARY KEY,fid TEXT NOT NULL,stid TEXT NOT NULL,name TEXT NOT NULL,info TEXT NOT NULL,addedAt INTEGER NOT NULL)",
        "CREATE TABLE response_cache (`key` TEXT NOT NULL PRIMARY KEY,payload TEXT NOT NULL,updatedAt INTEGER NOT NULL)",
        "CREATE TABLE drafts (`key` TEXT NOT NULL PRIMARY KEY,kind TEXT NOT NULL,tid TEXT NOT NULL,fid TEXT NOT NULL,stid TEXT NOT NULL,targetPid TEXT NOT NULL,targetAuthor TEXT NOT NULL,targetFloor INTEGER NOT NULL,subject TEXT NOT NULL,content TEXT NOT NULL,updatedAt INTEGER NOT NULL)",
        "CREATE TABLE watched_threads (tid TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,fid TEXT NOT NULL,lastKnownReplies INTEGER NOT NULL,lastSeenReplies INTEGER NOT NULL,updatedAt INTEGER NOT NULL)"
    )
    private val rows = listOf(
        "INSERT INTO favorites VALUES ('1','f1','10','a','默认',1),('2','f2','10','b','默认',2)",
        "INSERT INTO history VALUES ('1','h1','a','10',1,0),('2','h2','b','10',2,1)",
        "INSERT INTO favorite_boards VALUES ('fid:1','1','','b1','',1),('fid:2','2','','b2','',2)",
        "INSERT INTO response_cache VALUES ('k1','p1',1),('k2','p2',2)",
        "INSERT INTO drafts VALUES ('k1','reply','1','10','','','','0','s1','c1',1),('k2','reply','2','10','','','','0','s2','c2',2)",
        "INSERT INTO watched_threads VALUES ('1','w1','10',0,0,1),('2','w2','10',0,0,2)"
    )
}
