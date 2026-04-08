package x.x.memlists.core.reminder

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Shared sound playback for reminders.
 *
 * Uses a plain Thread with for-loop and Thread.sleep for inter-cycle pauses.
 * Reason: MediaPlayer.onCompletion does NOT fire reliably for system ringtone
 * URIs (content://settings/system/...), so an onCompletion-based loop hangs and
 * the system ringtone plays continuously without pauses.
 *
 * Single static thread per process — guards prevent stacking on rapid restarts.
 */
object ReminderSoundPlayer {

    private const val TAG = "MemLists"
    private const val PAUSE_MS = 2000L

    private val lock = Any()
    private var activeThread: Thread? = null
    private var activePlayer: MediaPlayer? = null

    fun start(context: Context, soundValue: String?, repeats: Int) {
        synchronized(lock) {
            if (activeThread != null) {
                Log.d(TAG, "ReminderSoundPlayer: thread already running, skip start")
                return
            }
            val uri = SoundUtils.resolveUri(soundValue) ?: SoundUtils.getSystemFallbackUri() ?: run {
                Log.e(TAG, "ReminderSoundPlayer: no uri resolved")
                return
            }
            val cycles = repeats.coerceIn(1, 26)
            val ctx = context.applicationContext
            Log.d(TAG, "ReminderSoundPlayer.start: cycles=$cycles uri=$uri")

            val t = Thread {
                var interrupted = false
                for (i in 1..cycles) {
                    if (Thread.currentThread().isInterrupted) break

                    val player = MediaPlayer()
                    val durationMs: Int
                    try {
                        player.setAudioAttributes(SoundUtils.alarmAudioAttributes)
                        SoundUtils.routeToSpeaker(player, ctx)
                        player.setDataSource(ctx, uri)
                        player.isLooping = false
                        player.prepare()
                        durationMs = player.duration.coerceAtLeast(500)
                        player.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "ReminderSoundPlayer: play error: ${e.message}")
                        try { player.release() } catch (_: Exception) {}
                        break
                    }
                    synchronized(lock) { activePlayer = player }
                    Log.d(TAG, "ReminderSoundPlayer: cycle $i/$cycles duration=${durationMs}ms")

                    try {
                        Thread.sleep(durationMs.toLong())
                    } catch (_: InterruptedException) {
                        interrupted = true
                    }

                    try { player.stop() } catch (_: Exception) {}
                    try { player.release() } catch (_: Exception) {}
                    synchronized(lock) { activePlayer = null }
                    if (interrupted) break

                    if (i < cycles) {
                        try {
                            Thread.sleep(PAUSE_MS)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
                Log.d(TAG, "ReminderSoundPlayer: thread finished (interrupted=$interrupted)")
                synchronized(lock) { activeThread = null }
            }
            activeThread = t
            t.start()
        }
    }

    fun stop() {
        synchronized(lock) {
            activeThread?.interrupt()
            activeThread = null
            try {
                activePlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
            } catch (_: Exception) {}
            activePlayer = null
        }
    }
}
