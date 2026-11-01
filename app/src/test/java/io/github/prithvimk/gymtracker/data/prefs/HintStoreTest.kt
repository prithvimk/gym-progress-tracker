package io.github.prithvimk.gymtracker.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HintStoreTest {
    private val prefs =
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("hints_test", Context.MODE_PRIVATE)

    @Test
    fun `a hint is not shown until it is marked, and stays shown`() {
        val store = SharedPreferencesHintStore(prefs)
        assertFalse(store.isShown(Hint.PEEK_ELAPSED))

        store.markShown(Hint.PEEK_ELAPSED)

        assertTrue(store.isShown(Hint.PEEK_ELAPSED))
        assertTrue(SharedPreferencesHintStore(prefs).isShown(Hint.PEEK_ELAPSED))
    }
}
