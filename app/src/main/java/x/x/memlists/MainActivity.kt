package x.x.memlists

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import x.x.memlists.core.reminder.ReminderPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask once on cold start to disable battery optimization — needed for reliable
        // alarm delivery on Samsung. Skip if user already declined.
        if (ReminderPermissions.isBatteryOptimized(this) && !ReminderPermissions.batteryOptDeclined(this)) {
            ReminderPermissions.requestBatteryOptimization(this)
            ReminderPermissions.markBatteryOptDeclined(this)
        }
        setContent {
            MemListsApp()
        }
    }
}

