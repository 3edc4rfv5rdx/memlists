package x.x.memlists.core.reminder

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Runtime permission helpers for the reminder pipeline.
 *
 * Samsung (and stock Android 12+) silently drop background activity launches and
 * fullscreen notifications unless the user grants:
 *  - SYSTEM_ALERT_WINDOW (Settings.canDrawOverlays) — needed for direct startActivity
 *  - USE_FULL_SCREEN_INTENT (NotificationManager.canUseFullScreenIntent) — Android 14+
 *  - REQUEST_IGNORE_BATTERY_OPTIMIZATIONS — needed for reliable alarm delivery
 *
 * Without these, the reminder fires but the alert never appears.
 */
object ReminderPermissions {

    private const val TAG = "MemLists"
    private const val PREFS = "memlists_perms"
    private const val KEY_BATTERY_DECLINED = "battery_opt_declined"
    const val REQ_POST_NOTIFICATIONS = 4711

    fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotifications(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotifications(activity)) return
        try {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot request POST_NOTIFICATIONS: ${e.message}")
        }
    }

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun requestOverlay(activity: Activity) {
        if (hasOverlay(activity)) return
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open overlay settings: ${e.message}")
        }
    }

    fun hasFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }

    fun requestFullScreenIntent(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (hasFullScreenIntent(activity)) return
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open full-screen intent settings: ${e.message}")
        }
    }

    fun isBatteryOptimized(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryOptDeclined(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_DECLINED, false)
    }

    fun markBatteryOptDeclined(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BATTERY_DECLINED, true).apply()
    }

    fun requestBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!isBatteryOptimized(activity)) return
        try {
            @SuppressWarnings("BatteryLife")
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open battery optimization settings: ${e.message}")
        }
    }
}
