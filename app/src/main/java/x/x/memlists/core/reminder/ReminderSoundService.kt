package x.x.memlists.core.reminder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import x.x.memlists.R

class ReminderSoundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundValue = intent.getStringExtra(EXTRA_SOUND)
                val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 10)
                startForeground(NOTIFICATION_ID, buildNotification())
                Log.d(TAG, "Service ACTION_PLAY repeatCount=$repeatCount")
                ReminderSoundPlayer.start(applicationContext, soundValue, repeatCount)
                scheduleSelfStop(repeatCount)
            }
            ACTION_STOP -> {
                ReminderSoundPlayer.stop()
                stopSelf()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Foreground service must stop itself after playback finishes — otherwise the
     * "Reminder Sound" notification lingers. Estimate generously: cap*10s.
     */
    private fun scheduleSelfStop(repeatCount: Int) {
        stopRunnable?.let { handler.removeCallbacks(it) }
        val cycles = repeatCount.coerceIn(1, 26)
        val totalMs = cycles * 10_000L + 5_000L
        val r = Runnable {
            Log.d(TAG, "Service self-stop after ${totalMs}ms")
            ReminderSoundPlayer.stop()
            stopSelf()
        }
        stopRunnable = r
        handler.postDelayed(r, totalMs)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ReminderSoundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("MemLists")
            .setContentText("Playing reminder sound")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        ReminderSoundPlayer.stop()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MemLists"
        const val CHANNEL_ID = "memlists_sound"
        const val NOTIFICATION_ID = 99999
        const val ACTION_PLAY = "x.x.memlists.PLAY_SOUND"
        const val ACTION_STOP = "x.x.memlists.STOP_SOUND"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_REPEAT_COUNT = "repeatCount"

        fun play(context: Context, soundValue: String?, repeatCount: Int = 10) {
            val intent = Intent(context, ReminderSoundService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SOUND, soundValue)
                putExtra(EXTRA_REPEAT_COUNT, repeatCount)
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
