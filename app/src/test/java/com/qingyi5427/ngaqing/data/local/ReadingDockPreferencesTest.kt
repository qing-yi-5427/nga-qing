package com.qingyi5427.ngaqing.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReadingDockPreferencesTest {
    @Test fun readingModePersistsWithoutChangingAccountRevision() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        val revision = prefs.requestPreferences().revision
        try {
            prefs.setReadingDockMode(ReadingDockMode.BOOKS)
            assertEquals(ReadingDockMode.BOOKS, UserPreferences(context).readingDockMode.first())
            assertEquals(revision, prefs.requestPreferences().revision)
        } finally {
            prefs.setReadingDockMode(ReadingDockMode.AUTO)
        }
    }
}
