package x.x.memlists.core.reminder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import x.x.memlists.R

class ReminderSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentRepeat = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundValue = intent.getStringExtra(EXTRA_SOUND)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, false)
                val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 25)
                startForeground(NOTIFICATION_ID, buildNotification())
                playSound(soundValue, loop, repeatCount)
            }
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playSound(soundValue: String?, loop: Boolean, repeatCount: Int) {
        stopPlayback()
        currentRepeat = 0

        val primaryUri = SoundUtils.resolveUri(soundValue)
        val fallbackUri = SoundUtils.getSystemFallbackUri()

        if (playSoundUri(primaryUri, loop, repeatCount)) return
        if (fallbackUri != null && fallbackUri != primaryUri) {
            Log.w(TAG, "Primary sound failed, trying fallback")
            if (playSoundUri(fallbackUri, loop, repeatCount)) return
        }

        Log.e(TAG, "Unable to play any reminder sound")
        stopSelf()
    }

    private fun playSoundUri(uri: Uri?, loop: Boolean, repeatCount: Int): Boolean {
        if (uri == null) return false
        return try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(SoundUtils.alarmAudioAttributes)
                SoundUtils.routeToSpeaker(this, applicationContext)
                setDataSource(applicationContext, uri)
                prepare()
                start()

                setOnCompletionListener { mp ->
                    currentRepeat++
                    if (loop && currentRepeat < repeatCount) {
                        try {
                            mp.seekTo(0)
                            handler.postDelayed({
                                try {
                                    if (mediaPlayer != null) mp.start()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error restarting sound: ${e.message}")
                                    stopPlayback()
                                    stopSelf()
                                }
                            }, 2000)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error seeking sound: ${e.message}")
                            stopPlayback()
                            stopSelf()
                        }
                    } else {
                        stopPlayback()
                        stopSelf()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound uri=$uri: ${e.message}")
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            false
        }
    }

    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
        mediaPlayer = null
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
        stopPlayback()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MemLists"
        const val CHANNEL_ID = "memlists_sound"
        const val NOTIFICATION_ID = 99999
        const val ACTION_PLAY = "x.x.memlists.PLAY_SOUND"
        const val ACTION_STOP = "x.x.memlists.STOP_SOUND"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_LOOP = "loop"
        const val EXTRA_REPEAT_COUNT = "repeatCount"

        fun play(context: Context, soundValue: String?, loop: Boolean = false, repeatCount: Int = 25) {
            val intent = Intent(context, ReminderSoundService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SOUND, soundValue)
                putExtra(EXTRA_LOOP, loop)
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
