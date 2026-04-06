package x.x.memlists.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import x.x.memlists.MemListsApplication

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "Boot completed, rescheduling all reminders")

        try {
            val app = context.applicationContext as MemListsApplication
            ReminderMaintenance.runAll(context, app.repository)
        } catch (e: Exception) {
            Log.e(TAG, "Error on boot reschedule: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MemLists"
    }
}
