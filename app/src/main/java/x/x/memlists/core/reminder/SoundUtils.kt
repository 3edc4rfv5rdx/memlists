package x.x.memlists.core.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import java.io.File

object SoundUtils {

    private const val TAG = "MemLists"

    val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun getSystemFallbackUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun resolveUri(soundValue: String?): Uri? {
        return when {
            soundValue.isNullOrEmpty() || soundValue == "default" -> getSystemFallbackUri()
            soundValue.startsWith("/") -> Uri.fromFile(File(soundValue))
            else -> try {
                Uri.parse(soundValue)
            } catch (_: Exception) {
                getSystemFallbackUri()
            }
        }
    }

    fun routeToSpeaker(player: MediaPlayer, context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val speaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                player.setPreferredDevice(speaker)
                Log.d(TAG, "Forced audio output to built-in speaker")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error routing to speaker: ${e.message}")
        }
    }
}
