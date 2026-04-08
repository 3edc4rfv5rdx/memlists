package x.x.memlists

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import x.x.memlists.core.reminder.ReminderPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask for POST_NOTIFICATIONS on Android 13+ — without this all reminder
        // notifications are silently dropped (importance=NONE at app level).
        if (!ReminderPermissions.hasNotifications(this)) {
            ReminderPermissions.requestNotifications(this)
        }
        // Ask on cold start to disable battery optimization — needed for reliable alarm
        // delivery on Samsung. Re-prompt at most once per BATTERY_OPT_REPROMPT_INTERVAL_MS
        // (7 days) so a user who dismissed the system screen with Back still gets another
        // chance later. Once granted, isBatteryOptimized() returns false and we stop asking.
        if (ReminderPermissions.shouldPromptBatteryOpt(this)) {
            ReminderPermissions.requestBatteryOptimization(this)
            ReminderPermissions.markBatteryOptPrompted(this)
        }
        setContent {
            MemListsApp()
        }
    }
}

