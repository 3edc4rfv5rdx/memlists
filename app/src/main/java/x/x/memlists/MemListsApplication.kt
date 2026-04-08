package x.x.memlists

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import x.x.memlists.core.data.MemListsDatabaseHelper
import x.x.memlists.core.data.MemListsRepository
import x.x.memlists.core.i18n.AppLocalizer
import x.x.memlists.core.theme.ThemeRepository

class MemListsApplication : Application() {
    val databaseHelper: MemListsDatabaseHelper by lazy { MemListsDatabaseHelper(this) }
    val repository: MemListsRepository by lazy { MemListsRepository(databaseHelper) }
    val localizer: AppLocalizer by lazy { AppLocalizer(this) }
    val themeRepository: ThemeRepository by lazy { ThemeRepository(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "One-time and period reminders"
            enableLights(true)
            enableVibration(true)
            setSound(null, null)
        }

        val daily = NotificationChannel(
            CHANNEL_DAILY, "Daily Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily reminders"
            enableLights(true)
            enableVibration(true)
            setSound(null, null)
        }

        val fullscreen = NotificationChannel(
            CHANNEL_FULLSCREEN, "Fullscreen Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Fullscreen reminder alerts"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            setBypassDnd(true)
        }

        manager.createNotificationChannels(listOf(reminders, daily, fullscreen))

        // Cleanup: remove the legacy "memlists_sound" channel (sound service now reuses
        // the reminder channel for its single foreground notification).
        try { manager.deleteNotificationChannel("memlists_sound") } catch (_: Exception) {}
    }

    companion object {
        const val CHANNEL_REMINDERS = "memlists_reminders"
        const val CHANNEL_DAILY = "memlists_daily"
        const val CHANNEL_FULLSCREEN = "memlists_fullscreen"
    }
}

