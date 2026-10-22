package io.github.prithvimk.gymtracker.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerTest {
    private var clock = 10_000L
    private var runs = 0
    private val store = FakeStore()

    private fun timer() = RestTimer(store, now = { clock }, onRun = { runs++ })

    @Test
    fun `start sets the end time, saves it and runs the service`() {
        val timer = timer()

        timer.start(90, "Bench Press")

        val expected = RestTimerState(endsAt = 100_000L, durationMillis = 90_000L, exerciseName = "Bench Press")
        assertEquals(expected, timer.state.value)
        assertEquals(expected, store.saved)
        assertEquals(1, runs)
    }

    @Test
    fun `a new start replaces the running rest`() {
        val timer = timer()
        timer.start(90, "Bench Press")
        clock = 40_000L

        timer.start(60, "Overhead Press")

        assertEquals(RestTimerState(100_000L, 60_000L, "Overhead Press"), timer.state.value)
    }

    @Test
    fun `starting with no rest stops the running one`() {
        val timer = timer()
        timer.start(90, "Bench Press")

        timer.start(0, "Curl")

        assertNull(timer.state.value)
        assertNull(store.saved)
        assertEquals(1, runs)
    }

    @Test
    fun `adjust moves the end and the length`() {
        val timer = timer()
        timer.start(90, "Bench Press")

        timer.adjust(15)
        assertEquals(RestTimerState(115_000L, 105_000L, "Bench Press"), timer.state.value)

        timer.adjust(-30)
        assertEquals(RestTimerState(85_000L, 75_000L, "Bench Press"), timer.state.value)
    }

    @Test
    fun `taking off more than is left ends the rest`() {
        val timer = timer()
        timer.start(30, "Bench Press")
        clock = 30_000L

        timer.adjust(-15)

        assertNull(timer.state.value)
    }

    @Test
    fun `the length never drops below the time left`() {
        val timer = timer()
        timer.start(30, "Bench Press")
        timer.adjust(60)
        clock = 20_000L

        timer.adjust(-60)

        val state = timer.state.value!!
        assertEquals(40_000L, state.endsAt)
        assertEquals(30_000L, state.durationMillis)
    }

    @Test
    fun `finishIfDue only ends a rest whose time is up`() {
        val timer = timer()
        timer.start(30, "Bench Press")
        clock = 39_999L
        assertNull(timer.finishIfDue())

        clock = 40_000L
        assertEquals("Bench Press", timer.finishIfDue()?.exerciseName)
        assertNull(timer.state.value)
        assertNull(timer.finishIfDue())
    }

    @Test
    fun `a saved rest comes back while it is still running`() {
        store.saved = RestTimerState(50_000L, 60_000L, "Squat")

        val timer = timer()
        timer.resumeIfRunning()

        assertEquals(store.saved, timer.state.value)
        assertEquals(1, runs)
    }

    @Test
    fun `a saved rest that ran out while the app was gone is dropped`() {
        store.saved = RestTimerState(5_000L, 60_000L, "Squat")

        val timer = timer()
        timer.resumeIfRunning()

        assertNull(timer.state.value)
        assertEquals(0, runs)
    }

    private class FakeStore : RestTimerStore {
        var saved: RestTimerState? = null

        override fun load(): RestTimerState? = saved

        override fun save(state: RestTimerState?) {
            saved = state
        }
    }
}
