package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qingyi5427.ngaqing.data.model.ThreadItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val ds = context.dataStore

    val uid: Flow<String> = ds.data.map { it[KEY_UID].orEmpty() }
    val cid: Flow<String> = ds.data.map { it[KEY_CID].orEmpty() }
    val uname: Flow<String> = ds.data.map { it[KEY_UNAME].orEmpty() }
    val isLoggedIn: Flow<Boolean> = ds.data.map { !it[KEY_UID].isNullOrBlank() && !it[KEY_CID].isNullOrBlank() }

    val themeMode: Flow<String> = ds.data.map { it[KEY_THEME] ?: "system" }
    val ngaDomain: Flow<String> = ds.data.map { NgaDomains.normalizeHost(it[KEY_NGA_DOMAIN]) }

    val blacklistUsers: Flow<Set<String>> = ds.data.map { csv(it[KEY_BL_USERS]) }
    val blacklistKeywords: Flow<Set<String>> = ds.data.map { csv(it[KEY_BL_KEYWORDS]) }

    suspend fun saveAuth(uid: String, cid: String, uname: String) {
        ds.edit {
            it[KEY_UID] = uid
            it[KEY_CID] = cid
            it[KEY_UNAME] = uname
        }
    }

    suspend fun clearAuth() {
        ds.edit {
            it.remove(KEY_UID)
            it.remove(KEY_CID)
            it.remove(KEY_UNAME)
        }
    }

    suspend fun setTheme(mode: String) {
        ds.edit { it[KEY_THEME] = mode }
    }

    suspend fun setNgaDomain(host: String) {
        ds.edit { it[KEY_NGA_DOMAIN] = NgaDomains.normalizeHost(host) }
    }

    suspend fun setBlacklistUsers(set: Set<String>) {
        ds.edit { it[KEY_BL_USERS] = set.joinToString(",") }
    }

    suspend fun setBlacklistKeywords(set: Set<String>) {
        ds.edit { it[KEY_BL_KEYWORDS] = set.joinToString(",") }
    }

    fun toThread(item: ThreadItem) = item

    private fun csv(s: String?): Set<String> =
        s?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    companion object {
        val KEY_UID = stringPreferencesKey("uid")
        val KEY_CID = stringPreferencesKey("cid")
        val KEY_UNAME = stringPreferencesKey("uname")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_NGA_DOMAIN = stringPreferencesKey("nga_domain")
        val KEY_BL_USERS = stringPreferencesKey("bl_users")
        val KEY_BL_KEYWORDS = stringPreferencesKey("bl_keywords")
    }
}
