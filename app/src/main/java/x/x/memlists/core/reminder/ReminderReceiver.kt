package x.x.memlists.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import x.x.memlists.FullScreenAlertActivity
import x.x.memlists.MemListsApplication
import x.x.memlists.R
import x.x.memlists.core.data.MemListsRepository
import x.x.memlists.core.data.todayAsInt
import x.x.memlists.core.i18n.AppLocalizer
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        try {
            when (intent.action) {
                ReminderActions.SPECIFIC_REMINDER -> handleSpecific(context, intent)
                ReminderActions.DAILY_REMINDER -> handleDaily(context, intent)
                ReminderActions.PERIOD_REMINDER -> handlePeriod(context, intent)
                ReminderActions.SNOOZED_REMINDER -> handleSnoozed(context, intent)
                ReminderActions.STOP_SOUND -> {
                    ReminderSoundService.stop(context)
                    val notifId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, 0)
                    if (notifId != 0) {
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive: ${e.message}", e)
        }
    }

    // --- Specific (one-time) reminder ---

    private fun handleSpecific(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(IntentExtras.ITEM_ID, 0).toLong()
        val repo = getRepository(context)

        if (!repo.isRemindersEnabledSync()) {
            Log.d(TAG, "Reminders disabled, skipping specific $itemId")
            return
        }

        val item = repo.getItemByIdSync(itemId) ?: run {
            Log.d(TAG, "Item $itemId not found")
            return
        }

        if (item.active == 0) {
            Log.d(TAG, "Item $itemId inactive, skipping")
            return
        }

        val sound = item.sound ?: repo.getDefaultSoundSync()
        val repeatCount = repo.getSoundRepeatsSync()
        Log.d(TAG, "handleSpecific item=$itemId loopSound=${item.loopSound} repeatCount=$repeatCount fullscreen=${item.fullscreen}")

        if (item.fullscreen == 1) {
            launchFullscreen(context, item, sound, repeatCount)
        } else {
            wakeScreen(context)
            playWithNotification(
                context, item.id.toInt(), item.title, item.content,
                sound, repeatCount, MemListsApplication.CHANNEL_REMINDERS
            )
        }

        // Post-fire: reschedule recurring or deactivate one-shot.
        // Auto-remove is deferred to next-day maintenance — see ReminderMaintenance.deleteExpired.
        if (item.yearly == 1 || item.monthly == 1) {
            rescheduleRecurring(context, repo, item)
        } else {
            repo.deactivateItemSync(itemId)
            Log.d(TAG, "Deactivated one-time item $itemId")
        }
    }

    // --- Daily reminder ---

    private fun handleDaily(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(IntentExtras.ITEM_ID, 0).toLong()
        val hour = intent.getIntExtra(IntentExtras.HOUR, 0)
        val minute = intent.getIntExtra(IntentExtras.MINUTE, 0)
        val daysMask = intent.getIntExtra(IntentExtras.DAYS_MASK, 127)
        val title = intent.getStringExtra(IntentExtras.TITLE) ?: "MemLists"

        val repo = getRepository(context)

        if (!repo.isRemindersEnabledSync()) {
            Log.d(TAG, "Reminders disabled, rescheduling daily $itemId")
            ReminderScheduler.scheduleDailyReminder(context, itemId.toInt(), hour, minute, daysMask, title)
            return
        }

        val item = repo.getItemByIdSync(itemId)
        if (item == null || item.active == 0 || item.reminderType != 2) {
            Log.d(TAG, "Daily item $itemId gone/inactive/changed, not rescheduling")
            return
        }

        val sound = item.sound ?: repo.getDefaultSoundSync()
        val repeatCount = repo.getSoundRepeatsSync()

        if (item.fullscreen == 1) {
            launchFullscreen(context, item, sound, repeatCount, isDaily = true)
        } else {
            wakeScreen(context)
            val notifId = (itemId.toInt() * 10000 + hour * 100 + minute)
            playWithNotification(
                context, notifId, item.title, item.content,
                sound, repeatCount, MemListsApplication.CHANNEL_DAILY
            )
        }

        // Reschedule next occurrence
        ReminderScheduler.scheduleDailyReminder(context, itemId.toInt(), hour, minute, daysMask, item.title)
    }

    // --- Period reminder ---

    private fun handlePeriod(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(IntentExtras.ITEM_ID, 0).toLong()
        val repo = getRepository(context)

        if (!repo.isRemindersEnabledSync()) {
            Log.d(TAG, "Reminders disabled, skipping period $itemId")
            return
        }

        val item = repo.getItemByIdSync(itemId) ?: run {
            Log.d(TAG, "Period item $itemId not found")
            return
        }

        if (item.active == 0) {
            Log.d(TAG, "Period item $itemId inactive, skipping")
            return
        }

        val today = todayAsInt()
        if (item.periodDoneUntil != null && today < item.periodDoneUntil) {
            Log.d(TAG, "Period $itemId suppressed until ${item.periodDoneUntil}")
            return
        }

        val sound = item.sound ?: repo.getDefaultSoundSync()
        val repeatCount = repo.getSoundRepeatsSync()

        if (item.fullscreen == 1) {
            val isMonthlyPeriod = item.date != null && item.date in 1..31
            launchFullscreen(context, item, sound, repeatCount, isPeriod = true, isMonthlyPeriod = isMonthlyPeriod)
        } else {
            wakeScreen(context)
            playWithNotification(
                context, itemId.toInt(), item.title, item.content,
                sound, repeatCount, MemListsApplication.CHANNEL_REMINDERS
            )
        }
    }

    // --- Snoozed reminder (data from intent, no DB check) ---

    private fun handleSnoozed(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(IntentExtras.ITEM_ID, 0)
        val title = intent.getStringExtra(IntentExtras.TITLE) ?: "MemLists"
        val content = intent.getStringExtra(IntentExtras.CONTENT) ?: ""
        val soundValue = intent.getStringExtra(IntentExtras.SOUND)

        // Read loop_sound from DB (may have changed since snooze was set)
        val repo = getRepository(context)
        val item = repo.getItemByIdSync(itemId.toLong())
        val loopSound = item?.loopSound == 1
        val repeatCount = repo.getSoundRepeatsSync()

        val isPeriod = item?.reminderType == 3
        val isMonthlyPeriod = isPeriod && item?.date != null && item.date in 1..31

        launchFullscreen(
            context, itemId, title, content, soundValue,
            loopSound, repeatCount, isPeriod, isMonthlyPeriod
        )
    }

    // --- Fullscreen alert launch ---

    private fun launchFullscreen(
        context: Context, item: MemListsRepository.ReminderItem,
        sound: String?, repeatCount: Int,
        isDaily: Boolean = false, isPeriod: Boolean = false, isMonthlyPeriod: Boolean = false
    ) {
        launchFullscreen(
            context, item.id.toInt(), item.title, item.content, sound,
            item.loopSound == 1, repeatCount, isPeriod, isMonthlyPeriod
        )
    }

    private fun launchFullscreen(
        context: Context, itemId: Int, title: String, content: String,
        soundValue: String?, loopSound: Boolean, repeatCount: Int,
        isPeriod: Boolean = false, isMonthlyPeriod: Boolean = false
    ) {
        val localizer = getLocalizer(context)
        val lang = getRepository(context).getLanguageSync()

        val fullScreenIntent = Intent(context, FullScreenAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(IntentExtras.ITEM_ID, itemId)
            putExtra(IntentExtras.TITLE, title)
            putExtra(IntentExtras.CONTENT, content)
            putExtra(IntentExtras.SOUND, soundValue)
            putExtra(IntentExtras.IS_PERIOD, isPeriod)
            putExtra(IntentExtras.IS_MONTHLY_PERIOD, isMonthlyPeriod)
            putExtra(IntentExtras.LOOP_SOUND, loopSound)
            putExtra(IntentExtras.REPEAT_COUNT, repeatCount)
            putExtra(IntentExtras.LABEL_REMINDER, localizer.lw("Reminder", lang) + ":")
            putExtra(IntentExtras.LABEL_POSTPONE, localizer.lw("Postpone for", lang) + ":")
            putExtra(IntentExtras.LABEL_MIN, localizer.lw("min", lang))
            putExtra(IntentExtras.LABEL_HOUR, localizer.lw("hour", lang))
            putExtra(IntentExtras.LABEL_HOURS, localizer.lw("hours", lang))
            putExtra(IntentExtras.LABEL_DAY, localizer.lw("day", lang))
            putExtra(IntentExtras.LABEL_CONTINUE, localizer.lw("Continue", lang))
            putExtra(IntentExtras.LABEL_DONE, localizer.lw("Done", lang))
        }

        // Diagnostics
        val canOverlay = Settings.canDrawOverlays(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canFullScreen = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nm.canUseFullScreenIntent()
        } else true
        Log.d(TAG, "Fullscreen launch diagnostics item=$itemId canOverlay=$canOverlay canFullScreen=$canFullScreen")

        // Direct start if overlay permission granted
        if (canOverlay) {
            try {
                context.startActivity(fullScreenIntent)
                Log.d(TAG, "Direct startActivity for item $itemId")
            } catch (e: Exception) {
                Log.w(TAG, "Direct startActivity failed: ${e.message}")
            }
        }

        // Always post fullscreen notification (screen OFF → auto-launch, screen ON → heads-up)
        val fullScreenPi = PendingIntent.getActivity(
            context, itemId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop sound action so user can silence even if Activity never appears
        val stopIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.STOP_SOUND
            putExtra(IntentExtras.NOTIFICATION_ID, itemId)
        }
        val stopPi = PendingIntent.getBroadcast(
            context, itemId + 900000, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MemListsApplication.CHANNEL_FULLSCREEN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null)
            .setDeleteIntent(stopPi)
            .addAction(R.drawable.ic_notification, "Stop", stopPi)
            .build()

        nm.notify(itemId, notification)

        // Wake screen. Sound playback is owned by FullScreenAlertActivity itself —
        // do NOT start the service here, otherwise the user hears an extra cycle
        // before the Activity stops the service and starts its own loop.
        wakeScreen(context)

        Log.d(TAG, "Fullscreen alert launched for item $itemId")
    }

    // --- Sound + notification (single foreground notification owned by service) ---

    private fun playWithNotification(
        context: Context, notificationId: Int, title: String, content: String,
        soundValue: String?, repeatCount: Int, channelId: String
    ) {
        ReminderSoundService.play(
            context = context,
            soundValue = soundValue,
            repeatCount = repeatCount,
            notificationId = notificationId,
            title = title,
            content = content,
            channelId = channelId
        )
    }

    // --- Recurring reschedule ---

    private fun rescheduleRecurring(
        context: Context, repo: MemListsRepository, item: MemListsRepository.ReminderItem
    ) {
        val date = item.date ?: return
        val origMonth = (date % 10000) / 100
        val origDay = date % 100

        val calendar = Calendar.getInstance()

        if (item.yearly == 1) {
            calendar.set(Calendar.MONTH, origMonth - 1)
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(maxDay))
            // Advance year until future
            while (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.YEAR, 1)
                val md = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(md))
            }
            Log.d(TAG, "Yearly reschedule item ${item.id} -> ${calendar.time}")
        } else if (item.monthly == 1) {
            calendar.add(Calendar.MONTH, 1)
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(maxDay))
            Log.d(TAG, "Monthly reschedule item ${item.id} -> ${calendar.time}")
        } else {
            return
        }

        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        // Preserve original day in DB for monthly (e.g. 31 even if clamped)
        val dbDay = if (item.monthly == 1) origDay else calendar.get(Calendar.DAY_OF_MONTH)
        val newDate = y * 10000 + m * 100 + dbDay

        repo.updateItemDateSync(item.id, newDate)
        ReminderScheduler.scheduleItem(context, repo, item.id)
    }

    // --- Helpers ---

    private fun wakeScreen(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isInteractive) {
                @Suppress("DEPRECATION")
                val wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "memlists:reminder"
                )
                wl.acquire(5000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error waking screen: ${e.message}")
        }
    }

    private fun getRepository(context: Context): MemListsRepository {
        return (context.applicationContext as MemListsApplication).repository
    }

    private fun getLocalizer(context: Context): AppLocalizer {
        return (context.applicationContext as MemListsApplication).localizer
    }

    companion object {
        private const val TAG = "MemLists"
    }
}
