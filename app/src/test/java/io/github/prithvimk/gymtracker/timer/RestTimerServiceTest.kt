package io.github.prithvimk.gymtracker.timer

import android.app.Application
import android.app.Notification
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** An application with only a rest timer, so the service runs without the app's database. */
class RestTimerTestApp :
    Application(),
    RestTimerOwner {
    override val restTimer: RestTimer by lazy {
        RestTimer(SharedPreferencesRestTimerStore(getSharedPreferences("rest-timer-test", MODE_PRIVATE)))
    }
}

@RunWith(AndroidJUnit4::class)
@Config(application = RestTimerTestApp::class)
class RestTimerServiceTest {
    private val app = ApplicationProvider.getApplicationContext<RestTimerTestApp>()
    private val timer = app.restTimer

    @After
    fun tearDown() {
        timer.skip()
    }

    @Test
    fun `a running rest shows a countdown and skipping it stops the service`() {
        timer.start(90, "Bench Press")
        val controller = Robolectric.buildService(RestTimerService::class.java).create().startCommand(0, 1)
        shadowOf(Looper.getMainLooper()).idle()
        val service = shadowOf(controller.get())

        val notification = service.lastForegroundNotification
        assertEquals("Bench Press", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(timer.state.value!!.endsAt, notification.`when`)
        assertFalse(service.isStoppedBySelf)

        controller.withIntent(Intent(app, RestTimerService::class.java).setAction(RestTimerService.ACTION_SKIP))
            .startCommand(0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(null, timer.state.value)
        assertTrue(service.isStoppedBySelf)
    }

    @Test
    fun `started with no rest running, it leaves the foreground and stops`() {
        val controller = Robolectric.buildService(RestTimerService::class.java).create().startCommand(0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        val service = shadowOf(controller.get())
        assertTrue(service.isForegroundStopped)
        assertTrue(service.isStoppedBySelf)
    }
}
