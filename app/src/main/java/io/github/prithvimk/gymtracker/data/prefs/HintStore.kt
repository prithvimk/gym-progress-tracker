package io.github.prithvimk.gymtracker.data.prefs

import android.content.SharedPreferences
import androidx.core.content.edit

/** A tip the app shows once. */
enum class Hint(val key: String) {
    /** During the first rest: tapping the workout's title peeks at the elapsed time. */
    PEEK_ELAPSED("hint_peek_elapsed_shown"),
}

/** Remembers which one-time hints have been shown. */
interface HintStore {
    fun isShown(hint: Hint): Boolean

    fun markShown(hint: Hint)
}

class SharedPreferencesHintStore(private val prefs: SharedPreferences) : HintStore {
    override fun isShown(hint: Hint): Boolean = prefs.getBoolean(hint.key, false)

    override fun markShown(hint: Hint) {
        prefs.edit { putBoolean(hint.key, true) }
    }
}
