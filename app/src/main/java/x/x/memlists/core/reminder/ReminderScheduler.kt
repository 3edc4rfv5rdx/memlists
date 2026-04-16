package x.x.memlists.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.json.JSONArray
import x.x.memlists.core.data.MemListsRepository
import x.x.memlists.core.data.todayAsInt
import java.util.Calendar

object ReminderScheduler {

    private const val TAG = "MemLists"

    // --- Public API ---

    fun scheduleItem(context: Context, repository: MemListsRepository, itemId: Long) {
        if (!repository.isRemindersEnabledSync()) return
        val item = repository.getItemByIdSync(itemId) ?: return
        if (item.active != 1) return

        when (item.reminderType) {
            1 -> scheduleOneTime(context, item)
            2 -> scheduleDailyItem(context, item)
            3 -> schedulePeriodItem(context, item)
        }
    }

    fun cancelItem(context: Context, itemId: Long) {
        cancelOneTime(context, itemId)
        cancelAllDaily(context, itemId)
        cancelAllPeriod(context, itemId)
        cancelSnooze(context, itemId.toInt())
    }

    fun cancelAll(context: Context, repository: MemListsRepository) {
        val ids = linkedSetOf<Long>()
        repository.getActiveOneTimeRemindersSync().forEach { ids += it.id }
        repository.getActiveDailyRemindersSync().forEach { ids += it.id }
        repository.getActivePeriodRemindersSync().forEach { ids += it.id }
        for (id in ids) cancelItem(context, id)
        Log.d(TAG, "cancelAll: cancelled ${ids.size} items")
    }

    fun rescheduleAll(context: Context, repository: MemListsRepository) {
        if (!repository.isRemindersEnabledSync()) {
            Log.d(TAG, "Reminders disabled, skipping rescheduleAll")
            return
        }

        // One-time reminders
        for (item in repository.getActiveOneTimeRemindersSync()) {
            scheduleOneTime(context, item)
        }

        // Daily reminders
        for (item in repository.getActiveDailyRemindersSync()) {
            scheduleDailyItem(context, item)
        }

        // Period reminders
        for (item in repository.getActivePeriodRemindersSync()) {
            schedulePeriodItem(context, item)
        }

        Log.d(TAG, "All reminders rescheduled")
    }

    // --- One-time ---

    private fun scheduleOneTime(context: Context, item: MemListsRepository.ReminderItem) {
        val date = item.date ?: return
        val time = item.time
        val hour = time?.let { it / 100 } ?: 9
        val minute = time?.let { it % 100 } ?: 30

        val calendar = dateToCalendar(date, hour, minute)
            ?: return

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "OneTime reminder ${item.id} is in the past, skipping")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.SPECIFIC_REMINDER
            putExtra(IntentExtras.ITEM_ID, item.id.toInt())
        }

        scheduleAlarmClock(context, item.id.toInt(), intent, calendar)
        Log.d(TAG, "Scheduled oneTime reminder ${item.id} at ${calendar.time}")
    }

    private fun cancelOneTime(context: Context, itemId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.SPECIFIC_REMINDER
        }
        cancelAlarm(context, itemId.toInt(), intent)
    }

    // --- Daily ---

    private fun scheduleDailyItem(context: Context, item: MemListsRepository.ReminderItem) {
        val times = parseDailyTimes(item.times) ?: return
        val daysMask = item.daysMask ?: 127

        for (timeStr in times) {
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue
            scheduleDailyReminder(context, item.id.toInt(), hour, minute, daysMask, item.title)
        }
    }

    fun scheduleDailyReminder(
        context: Context, itemId: Int, hour: Int, minute: Int,
        daysMask: Int, title: String
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, (itemId % 20) * 3) // spread conflicts
            set(Calendar.MILLISECOND, 0)
        }

        val timePassed = calendar.timeInMillis <= System.currentTimeMillis()
        val todayInMask = isDayInMask(calendar, daysMask)

        if (timePassed || !todayInMask) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            if (!advanceToNextValidDay(calendar, daysMask)) return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.DAILY_REMINDER
            putExtra(IntentExtras.ITEM_ID, itemId)
            putExtra(IntentExtras.HOUR, hour)
            putExtra(IntentExtras.MINUTE, minute)
            putExtra(IntentExtras.DAYS_MASK, daysMask)
            putExtra(IntentExtras.TITLE, title)
        }

        val requestCode = itemId * 10000 + hour * 100 + minute
        scheduleAlarmClock(context, requestCode, intent, calendar)
        Log.d(TAG, "Scheduled daily reminder $itemId at $hour:$minute -> ${calendar.time}")
    }

    private fun cancelAllDaily(context: Context, itemId: Long) {
        val id = itemId.toInt()
        for (hour in 0..23) {
            for (minute in 0..59) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderActions.DAILY_REMINDER
                }
                cancelAlarm(context, id * 10000 + hour * 100 + minute, intent)
            }
        }
    }

    // --- Period ---

    private fun schedulePeriodItem(context: Context, item: MemListsRepository.ReminderItem) {
        val dateFrom = item.date ?: return
        val dateTo = item.dateTo ?: return
        val time = item.time
        val daysMask = item.daysMask ?: 127
        val hour = time?.let { it / 100 } ?: 9
        val minute = time?.let { it % 100 } ?: 30

        val today = todayAsInt()
        if (item.periodDoneUntil != null && today < item.periodDoneUntil) {
            Log.d(TAG, "Period ${item.id} suppressed until ${item.periodDoneUntil}")
            return
        }

        val isMonthly = dateFrom in 1..31
        val dates = calculatePeriodDates(dateFrom, dateTo, daysMask, isMonthly)

        var count = 0
        for (date in dates) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, date.get(Calendar.YEAR))
                set(Calendar.MONTH, date.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderActions.PERIOD_REMINDER
                    putExtra(IntentExtras.ITEM_ID, item.id.toInt())
                }
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val requestCode = item.id.toInt() * 10000 + month * 100 + day
                scheduleAlarmClock(context, requestCode, intent, cal)
                count++
            }
        }
        Log.d(TAG, "Scheduled $count period alarms for item ${item.id}")
    }

    private fun cancelAllPeriod(context: Context, itemId: Long) {
        val id = itemId.toInt()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (month in 1..12) {
            for (day in 1..31) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderActions.PERIOD_REMINDER
                    putExtra(IntentExtras.ITEM_ID, id)
                }
                val requestCode = id * 10000 + month * 100 + day
                val pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    // --- Snooze (called from FullScreenAlertActivity) ---

    fun scheduleSnooze(
        context: Context, itemId: Int, title: String, content: String,
        soundValue: String?, minutesFromNow: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutesFromNow)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.SNOOZED_REMINDER
            putExtra(IntentExtras.ITEM_ID, itemId)
            putExtra(IntentExtras.TITLE, title)
            putExtra(IntentExtras.CONTENT, content)
            putExtra(IntentExtras.SOUND, soundValue)
        }

        val requestCode = 1000000 + itemId
        scheduleAlarmClock(context, requestCode, intent, calendar)
        Log.d(TAG, "Snoozed reminder $itemId for $minutesFromNow min -> ${calendar.time}")
    }

    fun cancelSnooze(context: Context, itemId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderActions.SNOOZED_REMINDER
        }
        cancelAlarm(context, 1000000 + itemId, intent)
    }

    // --- Helpers ---

    private fun scheduleAlarmClock(
        context: Context, requestCode: Int, intent: Intent, calendar: Calendar
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun cancelAlarm(context: Context, requestCode: Int, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun dateToCalendar(dateInt: Int, hour: Int, minute: Int): Calendar? {
        val year = dateInt / 10000
        val month = (dateInt % 10000) / 100
        val day = dateInt % 100
        if (year < 2000 || month < 1 || month > 12 || day < 1 || day > 31) return null

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, if (day > maxDay) maxDay else day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isDayInMask(calendar: Calendar, daysMask: Int): Boolean {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val bitIndex = if (dow == Calendar.SUNDAY) 6 else dow - 2
        return (daysMask and (1 shl bitIndex)) != 0
    }

    private fun advanceToNextValidDay(calendar: Calendar, daysMask: Int): Boolean {
        for (i in 0 until 7) {
            if (isDayInMask(calendar, daysMask)) return true
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return false
    }

    private fun calculatePeriodDates(
        dateFrom: Int, dateTo: Int, daysMask: Int, isMonthly: Boolean
    ): List<Calendar> {
        val result = mutableListOf<Calendar>()
        val now = Calendar.getInstance()

        if (isMonthly) {
            for (monthOffset in 0..1) {
                val base = Calendar.getInstance().apply {
                    add(Calendar.MONTH, monthOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val daysInMonth = base.getActualMaximum(Calendar.DAY_OF_MONTH)
                val startDay = dateFrom.coerceIn(1, daysInMonth)
                val endDay = dateTo.coerceIn(1, daysInMonth)

                if (startDay <= endDay) {
                    for (d in startDay..endDay) {
                        val cal = base.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        if (isDayInMask(cal, daysMask)) result += cal
                    }
                } else {
                    for (d in startDay..daysInMonth) {
                        val cal = base.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        if (isDayInMask(cal, daysMask)) result += cal
                    }
                    val nextMonth = (base.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val nextDays = nextMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (d in 1..endDay.coerceAtMost(nextDays)) {
                        val cal = nextMonth.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        if (isDayInMask(cal, daysMask)) result += cal
                    }
                }
            }
        } else {
            val fromCal = dateToCalendar(dateFrom, 0, 0) ?: return result
            val toCal = dateToCalendar(dateTo, 23, 59) ?: return result

            val current = fromCal.clone() as Calendar
            while (!current.after(toCal)) {
                if (isDayInMask(current, daysMask)) {
                    result += current.clone() as Calendar
                }
                current.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return result
    }

    fun parseDailyTimes(timesJson: String?): List<String>? {
        if (timesJson.isNullOrBlank()) return null
        return try {
            val arr = JSONArray(timesJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            null
        }
    }
}
