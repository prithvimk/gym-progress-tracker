package io.github.prithvimk.gymtracker.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.prithvimk.gymtracker.MainActivity
import io.github.prithvimk.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shows the running rest as a countdown notification and vibrates when it is up. It runs in the
 * foreground and holds a wake lock until the rest ends, so the alert comes on time with the screen
 * off. It stops itself once there is no rest running.
 */
class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var timer: RestTimer
    private var wakeLock: PowerManager.WakeLock? = null
    private var watching = false
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        timer = (application as RestTimerOwner).restTimer
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Started with startForegroundService, so it must go foreground even if the rest has just ended.
        goForeground(timer.state.value)
        // Watching only starts once in the foreground: stopping before that would crash the app.
        if (!watching) {
            watching = true
            watchTimer()
        }
        when (intent?.action) {
            ACTION_ADD -> timer.adjust(RestTimer.ADJUST_STEP_SECONDS)
            ACTION_SUBTRACT -> timer.adjust(-RestTimer.ADJUST_STEP_SECONDS)
            ACTION_SKIP -> timer.skip()
        }
        if (timer.state.value == null) stopFor(startId)
        return START_NOT_STICKY
    }

    private fun watchTimer() {
        scope.launch {
            timer.state.collectLatest { state ->
                if (state == null) {
                    stopFor(lastStartId)
                    return@collectLatest
                }
                NotificationManagerCompat.from(this@RestTimerService).cancel(FINISHED_NOTIFICATION_ID)
                notifyIfAllowed(COUNTDOWN_NOTIFICATION_ID, countdownNotification(state))
                val remaining = state.remainingMillis(System.currentTimeMillis())
                holdWakeLock(remaining + WAKE_LOCK_SLACK_MS)
                delay(remaining)
                timer.finishIfDue()?.let { finished ->
                    vibrate()
                    notifyIfAllowed(FINISHED_NOTIFICATION_ID, finishedNotification(finished))
                }
            }
        }
    }

    /** Stops, unless a start newer than [startId] has come in since; that start then decides. */
    private fun stopFor(startId: Int) {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun goForeground(state: RestTimerState?) {
        val notification = countdownNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(COUNTDOWN_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(COUNTDOWN_NOTIFICATION_ID, notification)
        }
    }

    private fun countdownNotification(state: RestTimerState?): Notification {
        val builder =
            NotificationCompat
                .Builder(this, COUNTDOWN_CHANNEL)
                .setSmallIcon(R.drawable.ic_rest_timer)
                .setContentTitle(getString(R.string.rest_notification_title))
                .setContentIntent(openAppIntent())
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Without this, Android 12+ may hold back the notification for its first 10 seconds.
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (state != null) {
            // The system draws the countdown itself, so the notification never needs updating per second.
            builder
                .setContentText(state.exerciseName)
                .setWhen(state.endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, getString(R.string.rest_subtract), actionIntent(ACTION_SUBTRACT))
                .addAction(0, getString(R.string.rest_add), actionIntent(ACTION_ADD))
                .addAction(0, getString(R.string.rest_skip), actionIntent(ACTION_SKIP))
        }
        return builder.build()
    }

    private fun finishedNotification(finished: RestTimerState): Notification = NotificationCompat
        .Builder(this, FINISHED_CHANNEL)
        .setSmallIcon(R.drawable.ic_rest_timer)
        .setContentTitle(getString(R.string.rest_finished_title))
        .setContentText(getString(R.string.rest_finished_body, finished.exerciseName))
        .setContentIntent(openAppIntent())
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setTimeoutAfter(FINISHED_TIMEOUT_MS)
        .build()

    // Notifications are optional: without the permission the timer still runs and vibrates.
    @SuppressLint("MissingPermission")
    private fun notifyIfAllowed(id: Int, notification: Notification) {
        val granted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent {
        // Like tapping the launcher icon: brings the app back as it was rather than starting it over.
        val intent =
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, RestTimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun holdWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply { acquire(timeoutMs) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun vibrate() {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
        runCatching {
            // Alarm usage, so the buzz still comes when the screen is off or the phone is on silent.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, attributes)
            }
        }.onFailure { Log.w(TAG, "Vibrating at the end of a rest failed", it) }
    }

    companion object {
        private const val TAG = "RestTimerService"
        private const val ACTION_ADD = "io.github.prithvimk.gymtracker.rest.ADD"
        private const val ACTION_SUBTRACT = "io.github.prithvimk.gymtracker.rest.SUBTRACT"
        internal const val ACTION_SKIP = "io.github.prithvimk.gymtracker.rest.SKIP"
        private const val COUNTDOWN_CHANNEL = "rest_timer"
        private const val FINISHED_CHANNEL = "rest_finished"
        private const val COUNTDOWN_NOTIFICATION_ID = 1
        private const val FINISHED_NOTIFICATION_ID = 2
        private const val WAKE_LOCK_TAG = "GymTracker:RestTimer"
        private const val WAKE_LOCK_SLACK_MS = 10_000L
        private const val FINISHED_TIMEOUT_MS = 60_000L
        private val VIBRATION_PATTERN = longArrayOf(0L, 500L, 250L, 500L, 250L, 500L)

        /** Starts the service for the running rest. Only works while the app is in the foreground. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java)) }
                .onFailure { Log.w(TAG, "Could not start the rest timer service", it) }
        }

        private fun createChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val countdown =
                NotificationChannel(
                    COUNTDOWN_CHANNEL,
                    context.getString(R.string.rest_channel_countdown),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) }
            // The service vibrates itself, so this channel stays quiet and only pops up.
            val finished =
                NotificationChannel(
                    FINISHED_CHANNEL,
                    context.getString(R.string.rest_channel_finished),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            manager.createNotificationChannels(listOf(countdown, finished))
        }
    }
}
