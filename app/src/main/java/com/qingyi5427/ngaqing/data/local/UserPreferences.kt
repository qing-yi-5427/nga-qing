package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qingyi5427.ngaqing.data.model.ThreadItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(val uid: String, val cid: String, val username: String)
data class RequestPreferences(val uid: String, val cid: String, val ngaDomain: String)

private val Context.dataStore by preferencesDataStore("user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val ds = context.dataStore
    private val preferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var cachedRequestPreferences: RequestPreferences? = null

    init {
        preferenceScope.launch {
            ds.data.collect { values ->
                cachedRequestPreferences = values.toRequestPreferences()
            }
        }
    }

    val uid: Flow<String> = ds.data.map { it[KEY_UID].orEmpty() }
    val cid: Flow<String> = ds.data.map { it[KEY_CID].orEmpty() }
    val uname: Flow<String> = ds.data.map { it[KEY_UNAME].orEmpty() }
    val isLoggedIn: Flow<Boolean> = ds.data.map { !it[KEY_UID].isNullOrBlank() && !it[KEY_CID].isNullOrBlank() }

    val themeMode: Flow<String> = ds.data.map { it[KEY_THEME] ?: "system" }
    val ngaDomain: Flow<String> = ds.data.map { NgaDomains.normalizeHost(it[KEY_NGA_DOMAIN]) }
    val readingTextScale: Flow<Float> = ds.data.map { (it[KEY_READING_TEXT_SCALE] ?: 1f).coerceIn(0.9f, 1.35f) }
    val readingLineSpacing: Flow<Float> = ds.data.map { (it[KEY_READING_LINE_SPACING] ?: 1f).coerceIn(0.9f, 1.25f) }
    val showSignatures: Flow<Boolean> = ds.data.map { it[KEY_SHOW_SIGNATURES] ?: true }
    val accounts: Flow<List<SavedAccount>> = ds.data.map { parseAccounts(it[KEY_ACCOUNTS]) }

    /** OkHttp 每个请求只读取一次 DataStore 快照，避免分别启动三个 Flow collector。 */
    fun requestPreferencesOrNull(): RequestPreferences? = cachedRequestPreferences

    suspend fun requestPreferences(): RequestPreferences =
        cachedRequestPreferences ?: ds.data.first().toRequestPreferences().also {
            cachedRequestPreferences = it
        }

    val blacklistUsers: Flow<Set<String>> = ds.data.map { csv(it[KEY_BL_USERS]) }
    val blacklistKeywords: Flow<Set<String>> = ds.data.map { csv(it[KEY_BL_KEYWORDS]) }

    suspend fun saveAuth(uid: String, cid: String, uname: String) {
        ds.edit {
            it[KEY_UID] = uid
            it[KEY_CID] = cid
            it[KEY_UNAME] = uname
            val accounts = parseAccounts(it[KEY_ACCOUNTS]).filterNot { account -> account.uid == uid } +
                SavedAccount(uid, cid, uname)
            it[KEY_ACCOUNTS] = encodeAccounts(accounts)
        }
    }

    suspend fun clearAuth() {
        ds.edit {
            val activeUid = it[KEY_UID]
            if (!activeUid.isNullOrBlank()) {
                it[KEY_ACCOUNTS] = encodeAccounts(
                    parseAccounts(it[KEY_ACCOUNTS]).filterNot { account -> account.uid == activeUid }
                )
            }
            it.remove(KEY_UID)
            it.remove(KEY_CID)
            it.remove(KEY_UNAME)
        }
    }

    suspend fun switchAccount(uid: String): Boolean {
        var switched = false
        ds.edit {
            val account = parseAccounts(it[KEY_ACCOUNTS]).firstOrNull { account -> account.uid == uid }
            if (account != null) {
                it[KEY_UID] = account.uid
                it[KEY_CID] = account.cid
                it[KEY_UNAME] = account.username
                switched = true
            }
        }
        return switched
    }

    suspend fun removeAccount(uid: String) {
        ds.edit {
            it[KEY_ACCOUNTS] = encodeAccounts(
                parseAccounts(it[KEY_ACCOUNTS]).filterNot { account -> account.uid == uid }
            )
            if (it[KEY_UID] == uid) {
                it.remove(KEY_UID)
                it.remove(KEY_CID)
                it.remove(KEY_UNAME)
            }
        }
    }

    suspend fun setTheme(mode: String) {
        ds.edit { it[KEY_THEME] = mode }
    }

    suspend fun setNgaDomain(host: String) {
        ds.edit { it[KEY_NGA_DOMAIN] = NgaDomains.normalizeHost(host) }
    }

    suspend fun setReadingTextScale(value: Float) {
        ds.edit { it[KEY_READING_TEXT_SCALE] = value.coerceIn(0.9f, 1.35f) }
    }

    suspend fun setReadingLineSpacing(value: Float) {
        ds.edit { it[KEY_READING_LINE_SPACING] = value.coerceIn(0.9f, 1.25f) }
    }

    suspend fun setShowSignatures(value: Boolean) {
        ds.edit { it[KEY_SHOW_SIGNATURES] = value }
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

    private fun parseAccounts(raw: String?): List<SavedAccount> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uid = item.optString("uid")
                val cid = item.optString("cid")
                if (uid.isNotBlank() && cid.isNotBlank()) {
                    add(SavedAccount(uid, cid, item.optString("username")))
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeAccounts(accounts: List<SavedAccount>): String = JSONArray().apply {
        accounts.forEach { account ->
            put(JSONObject().apply {
                put("uid", account.uid)
                put("cid", account.cid)
                put("username", account.username)
            })
        }
    }.toString()

    private fun androidx.datastore.preferences.core.Preferences.toRequestPreferences() =
        RequestPreferences(
            uid = this[KEY_UID].orEmpty(),
            cid = this[KEY_CID].orEmpty(),
            ngaDomain = NgaDomains.normalizeHost(this[KEY_NGA_DOMAIN])
        )

    companion object {
        val KEY_UID = stringPreferencesKey("uid")
        val KEY_CID = stringPreferencesKey("cid")
        val KEY_UNAME = stringPreferencesKey("uname")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_NGA_DOMAIN = stringPreferencesKey("nga_domain")
        val KEY_READING_TEXT_SCALE = floatPreferencesKey("reading_text_scale")
        val KEY_READING_LINE_SPACING = floatPreferencesKey("reading_line_spacing")
        val KEY_SHOW_SIGNATURES = booleanPreferencesKey("show_signatures")
        val KEY_ACCOUNTS = stringPreferencesKey("accounts")
        val KEY_BL_USERS = stringPreferencesKey("bl_users")
        val KEY_BL_KEYWORDS = stringPreferencesKey("bl_keywords")
    }
}
