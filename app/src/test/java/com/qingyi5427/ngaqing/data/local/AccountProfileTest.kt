package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountProfileTest {
    private val prefs = UserPreferences(ApplicationProvider.getApplicationContext<Context>())

    @Test fun oldLoginCanGainProfileWithoutChangingSessionRevision() = runBlocking {
        prefs.saveAuth("profile-old-101", "fixture-cid-101", "")
        val session = prefs.requestPreferences()
        assertEquals("", prefs.accountDisplay.first().username)

        assertTrue(prefs.saveProfileIfCurrent(session, session.uid,
            "示例用户", "https://img.nga.cn/example-avatar.png"))
        assertEquals(session.revision, prefs.requestPreferences().revision)
        assertEquals(AccountDisplay(session.uid, "示例用户",
            "https://img.nga.cn/example-avatar.png", session.revision),
            prefs.accountDisplay.first())

        // The WebView login cookie does not contain a name. Re-authentication must not erase it.
        prefs.saveAuth(session.uid, "fixture-new-cid-101", "")
        assertEquals("示例用户", prefs.accountDisplay.first().username)
        assertEquals("https://img.nga.cn/example-avatar.png", prefs.accountDisplay.first().avatar)
    }

    @Test fun accountSwitchAndStaleRoundTripCannotMixProfileMetadata() = runBlocking {
        prefs.saveAuth("profile-a-102", "fixture-cid-a", "")
        val oldA = prefs.requestPreferences()
        assertTrue(prefs.saveProfileIfCurrent(oldA, oldA.uid, "甲用户", "https://img.nga.cn/a.png"))

        prefs.saveAuth("profile-b-102", "fixture-cid-b", "")
        val b = prefs.requestPreferences()
        assertFalse(prefs.saveProfileIfCurrent(oldA, oldA.uid, "过期甲用户", "https://img.nga.cn/stale.png"))
        assertTrue(prefs.saveProfileIfCurrent(b, b.uid, "乙用户", "https://img.nga.cn/b.png"))
        assertEquals("乙用户", prefs.accountDisplay.first().username)

        assertTrue(prefs.switchAccount(oldA.uid))
        val newA = prefs.requestPreferences()
        assertFalse(prefs.saveProfileIfCurrent(oldA, oldA.uid, "过期甲用户", "https://img.nga.cn/stale.png"))
        assertFalse(prefs.saveProfileIfCurrent(newA, b.uid, "乙用户", "https://img.nga.cn/b.png"))
        assertEquals("甲用户", prefs.accountDisplay.first().username)
        assertEquals("https://img.nga.cn/a.png", prefs.accountDisplay.first().avatar)
        assertTrue(prefs.switchAccount(b.uid))
        assertEquals("乙用户", prefs.accountDisplay.first().username)
        assertEquals("https://img.nga.cn/b.png", prefs.accountDisplay.first().avatar)
    }

    @Test fun missingOrNumericNameAndEmptyAvatarDoNotEraseKnownProfile() = runBlocking {
        prefs.saveAuth("profile-known-103", "fixture-cid-103", "保留昵称")
        val session = prefs.requestPreferences()
        assertTrue(prefs.saveProfileIfCurrent(session, session.uid,
            "保留昵称", "https://img.nga.cn/known.png"))
        assertTrue(prefs.saveProfileIfCurrent(session, session.uid, session.uid, ""))
        assertEquals("保留昵称", prefs.accountDisplay.first().username)
        assertEquals("https://img.nga.cn/known.png", prefs.accountDisplay.first().avatar)
        assertEquals(session.revision, prefs.requestPreferences().revision)
    }
}
