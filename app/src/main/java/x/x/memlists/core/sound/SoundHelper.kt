package x.x.memlists.core.sound

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

data class SoundItem(
    val name: String,
    val uri: String
)

class SoundHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun getSystemSounds(): List<SoundItem> {
        val sounds = mutableListOf<SoundItem>()

        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (defaultUri != null) {
            val defaultRingtone = RingtoneManager.getRingtone(context, defaultUri)
            val defaultName = defaultRingtone?.getTitle(context) ?: "Default"
            sounds += SoundItem(name = defaultName, uri = defaultUri.toString())
        }

        for (type in intArrayOf(RingtoneManager.TYPE_NOTIFICATION, RingtoneManager.TYPE_ALARM)) {
            val manager = RingtoneManager(context)
            manager.setType(type)
            val cursor = manager.cursor
            val suffix = if (type == RingtoneManager.TYPE_ALARM) " (Alarm)" else ""
            while (cursor.moveToNext()) {
                val name = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) + suffix
                val uri = manager.getRingtoneUri(cursor.position).toString()
                if (sounds.none { it.uri == uri }) {
                    sounds += SoundItem(name = name, uri = uri)
                }
            }
        }

        return sounds
    }

    var onPlaybackComplete: (() -> Unit)? = null

    fun play(uri: String) {
        stop()
        try {
            val source = if (uri.startsWith("/")) {
                // local file path
                null
            } else {
                uri
            }
            mediaPlayer = MediaPlayer().apply {
                if (uri.startsWith("/")) {
                    setDataSource(uri)
                } else {
                    setDataSource(context, Uri.parse(uri))
                }
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                    onPlaybackComplete?.invoke()
                }
            }
        } catch (_: Exception) {
            mediaPlayer = null
            onPlaybackComplete?.invoke()
        }
    }

    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    fun release() {
        stop()
    }
}
