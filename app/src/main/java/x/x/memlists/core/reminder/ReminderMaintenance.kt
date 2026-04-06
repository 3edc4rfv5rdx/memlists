package x.x.memlists.core.reminder

import android.content.Context
import android.util.Log
import x.x.memlists.core.data.MemListsRepository
import x.x.memlists.core.data.todayAsInt
import java.util.Calendar

object ReminderMaintenance {

    private const val TAG = "MemLists"

    /**
     * Run all maintenance tasks and reschedule alarms.
     * Called on boot and on app launch.
     */
    fun runAll(context: Context, repository: MemListsRepository) {
        val today = todayAsInt()
        Log.d(TAG, "Maintenance: today=$today")

        deleteExpired(repository, today)
        advanceYearly(repository, today)
        advanceMonthly(repository, today)
        ReminderScheduler.rescheduleAll(context, repository)
    }

    /**
     * Delete one-time reminders with auto-remove flag that are in the past
     * and are not yearly/monthly.
     */
    private fun deleteExpired(repository: MemListsRepository, today: Int) {
        val items = repository.getActiveOneTimeRemindersSync()
        var count = 0
        for (item in items) {
            if (item.remove == 1 && item.yearly == 0 && item.monthly == 0) {
                val date = item.date ?: continue
                if (date < today) {
                    repository.deleteItemSync(item.id)
                    count++
                }
            }
        }
        if (count > 0) Log.d(TAG, "Maintenance: deleted $count expired items")
    }

    /**
     * Advance yearly reminders: increment year until date >= today.
     */
    private fun advanceYearly(repository: MemListsRepository, today: Int) {
        val items = repository.getActiveOneTimeRemindersSync()
        for (item in items) {
            if (item.yearly != 1) continue
            val date = item.date ?: continue
            if (date >= today) continue

            val origMonth = (date % 10000) / 100
            val origDay = date % 100

            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, origMonth - 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(maxDay))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)

            // Advance year until future
            val todayCal = Calendar.getInstance()
            while (!cal.after(todayCal)) {
                cal.add(Calendar.YEAR, 1)
                val md = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(md))
            }

            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val newDate = y * 10000 + m * 100 + origDay
            repository.updateItemDateSync(item.id, newDate)
            Log.d(TAG, "Maintenance: advanced yearly item ${item.id} from $date to $newDate")
        }
    }

    /**
     * Advance monthly reminders: increment month until date >= today.
     */
    private fun advanceMonthly(repository: MemListsRepository, today: Int) {
        val items = repository.getActiveOneTimeRemindersSync()
        for (item in items) {
            if (item.monthly != 1) continue
            val date = item.date ?: continue
            if (date >= today) continue

            val origDay = date % 100

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, date / 10000)
            cal.set(Calendar.MONTH, ((date % 10000) / 100) - 1)
            cal.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)

            val todayCal = Calendar.getInstance()
            while (!cal.after(todayCal)) {
                cal.add(Calendar.MONTH, 1)
                val md = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, origDay.coerceAtMost(md))
            }

            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val newDate = y * 10000 + m * 100 + origDay
            repository.updateItemDateSync(item.id, newDate)
            Log.d(TAG, "Maintenance: advanced monthly item ${item.id} from $date to $newDate")
        }
    }
}
