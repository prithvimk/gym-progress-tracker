package io.github.prithvimk.gymtracker.timer

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A running rest: it ends at [endsAt]; [durationMillis] is how long it was set for, for progress. */
data class RestTimerState(val endsAt: Long, val durationMillis: Long, val exerciseName: String) {
    fun remainingMillis(now: Long): Long = (endsAt - now).coerceAtLeast(0L)
}

/** The application, as the rest timer service sees it: it only needs the timer. */
interface RestTimerOwner {
    val restTimer: RestTimer
}

/** Keeps the running rest across process death. */
interface RestTimerStore {
    fun load(): RestTimerState?

    fun save(state: RestTimerState?)
}

class SharedPreferencesRestTimerStore(private val prefs: SharedPreferences) : RestTimerStore {
    override fun load(): RestTimerState? {
        val endsAt = prefs.getLong(KEY_ENDS_AT, 0L).takeIf { it > 0L } ?: return null
        return RestTimerState(
            endsAt = endsAt,
            durationMillis = prefs.getLong(KEY_DURATION, 0L),
            exerciseName = prefs.getString(KEY_EXERCISE, null).orEmpty(),
        )
    }

    override fun save(state: RestTimerState?) {
        prefs.edit {
            if (state == null) {
                remove(KEY_ENDS_AT)
                remove(KEY_DURATION)
                remove(KEY_EXERCISE)
            } else {
                putLong(KEY_ENDS_AT, state.endsAt)
                putLong(KEY_DURATION, state.durationMillis)
                putString(KEY_EXERCISE, state.exerciseName)
            }
        }
    }

    private companion object {
        const val KEY_ENDS_AT = "rest_timer_ends_at"
        const val KEY_DURATION = "rest_timer_duration"
        const val KEY_EXERCISE = "rest_timer_exercise"
    }
}

/**
 * The rest timer. There is at most one rest at a time; starting another replaces it. The time left is
 * derived from the end time, so the timer keeps counting while the app is not running. [onRun] is called
 * whenever a rest starts or resumes, so the countdown service can be started.
 */
class RestTimer(
    private val store: RestTimerStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val onRun: () -> Unit = {},
) {
    private val _state = MutableStateFlow(store.load()?.takeIf { it.remainingMillis(now()) > 0L })
    val state: StateFlow<RestTimerState?> = _state.asStateFlow()

    /** Starts a rest of [seconds] after a set of [exerciseName]. Zero or less means no rest, and stops any. */
    fun start(seconds: Int, exerciseName: String) {
        if (seconds <= 0) {
            skip()
            return
        }
        val millis = seconds * MILLIS_PER_SECOND
        update(RestTimerState(endsAt = now() + millis, durationMillis = millis, exerciseName = exerciseName))
        onRun()
    }

    /** Adds [seconds] to the rest, or takes them off when negative. Taking off more than is left ends it. */
    fun adjust(seconds: Int) {
        val current = _state.value ?: return
        val time = now()
        val endsAt = current.endsAt + seconds * MILLIS_PER_SECOND
        if (endsAt <= time) {
            update(null)
            return
        }
        val duration = (current.durationMillis + seconds * MILLIS_PER_SECOND).coerceAtLeast(endsAt - time)
        update(current.copy(endsAt = endsAt, durationMillis = duration))
    }

    fun skip() {
        update(null)
    }

    /** Restarts the countdown service for a rest still running, say after the app was killed. */
    fun resumeIfRunning() {
        val current = _state.value ?: return
        if (current.remainingMillis(now()) > 0L) onRun() else update(null)
    }

    /** Ends the rest if its time is up. Returns the rest that ended, or null if it isn't due. */
    fun finishIfDue(): RestTimerState? {
        val current = _state.value ?: return null
        if (current.remainingMillis(now()) > 0L) return null
        update(null)
        return current
    }

    private fun update(state: RestTimerState?) {
        _state.value = state
        store.save(state)
    }

    companion object {
        const val ADJUST_STEP_SECONDS = 15
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
