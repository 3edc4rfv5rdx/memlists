package x.x.memlists.core.reminder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import x.x.memlists.MainActivity
import x.x.memlists.R

/**
 * Foreground service that plays the reminder sound and owns the visible
 * reminder notification at the same time.
 *
 * Why one notification, not two: Android requires foreground services to show
 * a notification while running. Posting a separate "now playing sound" notification
 * alongside the reminder produces a confusing flash of two items in the shade.
 * Instead the service builds the same reminder notification (channel, content,
 * Stop action) and calls startForeground with the reminder's id. When playback
 * ends we use stopForeground(STOP_FOREGROUND_DETACH) so the notification stays
 * visible after the service is gone.
 */
class ReminderSoundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var currentNotificationId: Int = 0
    private var currentTitle: String = ""
    private var currentContent: String = ""
    private var currentChannelId: String = FALLBACK_CHANNEL_ID

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundValue = intent.getStringExtra(EXTRA_SOUND)
                val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 10)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, FALLBACK_NOTIFICATION_ID)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "MemLists"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: FALLBACK_CHANNEL_ID
                currentNotificationId = notificationId
                currentTitle = title
                currentContent = content
                currentChannelId = channelId

                val notification = buildReminderNotification(notificationId, title, content, channelId, ongoing = true)
                startForeground(notificationId, notification)
                Log.d(TAG, "Service ACTION_PLAY notifId=$notificationId repeatCount=$repeatCount")
                ReminderSoundPlayer.start(applicationContext, soundValue, repeatCount)
                scheduleSelfStop(repeatCount)
            }
            ACTION_STOP -> {
                ReminderSoundPlayer.stop()
                detachAndStop()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Foreground service must stop itself after playback finishes — otherwise the
     * reminder notification keeps the "ongoing" flag forever. Estimate generously: cap*10s.
     */
    private fun scheduleSelfStop(repeatCount: Int) {
        stopRunnable?.let { handler.removeCallbacks(it) }
        val cycles = repeatCount.coerceIn(1, 26)
        val totalMs = cycles * 10_000L + 5_000L
        val r = Runnable {
            Log.d(TAG, "Service self-stop after ${totalMs}ms")
            ReminderSoundPlayer.stop()
            detachAndStop()
        }
        stopRunnable = r
        handler.postDelayed(r, totalMs)
    }

    /**
     * Re-post the notification as dismissable (no ongoing flag, no Stop action),
     * then detach foreground so the notification survives stopSelf().
     */
    private fun detachAndStop() {
        if (currentNotificationId != 0) {
            try {
                val dismissable = buildReminderNotification(
                    currentNotificationId, currentTitle, currentContent, currentChannelId,
                    ongoing = false
                )
                androidx.core.app.NotificationManagerCompat.from(this)
                    .notify(currentNotificationId, dismissable)
            } catch (e: Exception) {
                Log.e(TAG, "Error re-posting dismissable notification: ${e.message}")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        stopSelf()
    }

    private fun buildReminderNotification(
        notificationId: Int, title: String, content: String, channelId: String, ongoing: Boolean
    ): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPi = PendingIntent.getActivity(
            this, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content).setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setOngoing(ongoing)
            .setSound(null)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)

        if (ongoing) {
            val stopIntent = Intent(this, ReminderSoundService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPi = PendingIntent.getService(
                this, notificationId + 900000, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notification, "Stop", stopPi)
        }

        return builder.build()
    }

    override fun onDestroy() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        ReminderSoundPlayer.stop()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MemLists"
        private const val FALLBACK_NOTIFICATION_ID = 99999
        private const val FALLBACK_CHANNEL_ID = "memlists_reminders"
        const val ACTION_PLAY = "x.x.memlists.PLAY_SOUND"
        const val ACTION_STOP = "x.x.memlists.STOP_SOUND"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_REPEAT_COUNT = "repeatCount"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_CHANNEL_ID = "channelId"

        fun play(
            context: Context,
            soundValue: String?,
            repeatCount: Int,
            notificationId: Int,
            title: String,
            content: String,
            channelId: String
        ) {
            val intent = Intent(context, ReminderSoundService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SOUND, soundValue)
                putExtra(EXTRA_REPEAT_COUNT, repeatCount)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_CHANNEL_ID, channelId)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderSoundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                // Service not running, ignore
            }
        }
    }
}
