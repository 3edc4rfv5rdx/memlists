package x.x.memlists

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import x.x.memlists.core.reminder.IntentExtras
import x.x.memlists.core.reminder.ReminderScheduler
import x.x.memlists.core.reminder.ReminderSoundPlayer
import x.x.memlists.core.reminder.ReminderSoundService
import java.util.Calendar

class FullScreenAlertActivity : Activity() {

    private lateinit var draggableCircle: View
    private lateinit var barrierOverlay: View
    private var dragStartY = 0f
    private var initialY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()
        setContentView(R.layout.activity_fullscreen_alert)

        val itemId = intent.getIntExtra(IntentExtras.ITEM_ID, -1)
        val title = intent.getStringExtra(IntentExtras.TITLE) ?: "Reminder"
        val content = intent.getStringExtra(IntentExtras.CONTENT) ?: ""
        val soundValue = intent.getStringExtra(IntentExtras.SOUND)
        val isPeriod = intent.getBooleanExtra(IntentExtras.IS_PERIOD, false)
        val isMonthlyPeriod = intent.getBooleanExtra(IntentExtras.IS_MONTHLY_PERIOD, false)
        val isDaily = intent.getBooleanExtra(IntentExtras.IS_DAILY, false)
        val loopSound = intent.getBooleanExtra(IntentExtras.LOOP_SOUND, true)
        val repeatCount = intent.getIntExtra(IntentExtras.REPEAT_COUNT, 25)

        // Translated labels
        val labelReminder = intent.getStringExtra(IntentExtras.LABEL_REMINDER) ?: "Reminder:"
        val labelPostpone = intent.getStringExtra(IntentExtras.LABEL_POSTPONE) ?: "Postpone for:"
        val labelMin = intent.getStringExtra(IntentExtras.LABEL_MIN) ?: "min"
        val labelHour = intent.getStringExtra(IntentExtras.LABEL_HOUR) ?: "hour"
        val labelHours = intent.getStringExtra(IntentExtras.LABEL_HOURS) ?: "hours"
        val labelDay = intent.getStringExtra(IntentExtras.LABEL_DAY) ?: "day"
        val labelContinue = intent.getStringExtra(IntentExtras.LABEL_CONTINUE) ?: "Continue"
        val labelDone = intent.getStringExtra(IntentExtras.LABEL_DONE) ?: "Done"

        Log.d(TAG, "FullScreenAlert created: itemId=$itemId, title=$title, isPeriod=$isPeriod")

        // Cancel the notification (fullscreen is shown instead)
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(itemId)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification: ${e.message}")
        }

        // Current time
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        findViewById<TextView>(R.id.alert_time).text = currentTime

        // Labels
        findViewById<TextView>(R.id.label_reminder).text = labelReminder
        findViewById<TextView>(R.id.label_postpone).text = labelPostpone

        // Content
        findViewById<TextView>(R.id.alert_title).text = title
        findViewById<TextView>(R.id.alert_content).apply {
            text = content
            visibility = if (content.isEmpty()) View.GONE else View.VISIBLE
        }

        // Snooze button texts
        findViewById<Button>(R.id.snooze_10min).text = "10 $labelMin"
        findViewById<Button>(R.id.snooze_20min).text = "20 $labelMin"
        findViewById<Button>(R.id.snooze_30min).text = "30 $labelMin"
        findViewById<Button>(R.id.snooze_1hour).text = "1 $labelHour"
        findViewById<Button>(R.id.snooze_3hours).text = "3 $labelHours"

        // OK button
        findViewById<Button>(R.id.alert_ok_button).setOnClickListener { dismissAlert() }

        // Snooze buttons
        findViewById<Button>(R.id.snooze_10min).setOnClickListener {
            snoozeReminder(itemId, title, content, soundValue, 10)
        }
        findViewById<Button>(R.id.snooze_20min).setOnClickListener {
            snoozeReminder(itemId, title, content, soundValue, 20)
        }
        findViewById<Button>(R.id.snooze_30min).setOnClickListener {
            snoozeReminder(itemId, title, content, soundValue, 30)
        }
        findViewById<Button>(R.id.snooze_1hour).setOnClickListener {
            snoozeReminder(itemId, title, content, soundValue, 60)
        }
        findViewById<Button>(R.id.snooze_3hours).setOnClickListener {
            snoozeReminder(itemId, title, content, soundValue, 180)
        }

        // Postpone 1 day (hidden for daily)
        val postpone1day = findViewById<Button>(R.id.postpone_1day)
        if (isDaily) {
            postpone1day.visibility = View.GONE
        } else {
            postpone1day.text = "1 $labelDay"
            postpone1day.setOnClickListener {
                snoozeReminder(itemId, title, content, soundValue, 1440)
            }
        }

        // Period mode: hide snooze, show Continue/Done
        if (isPeriod) {
            findViewById<View>(R.id.snooze_section).visibility = View.GONE
            val periodSection = findViewById<View>(R.id.period_section)
            periodSection.visibility = View.VISIBLE

            val continueButton = findViewById<Button>(R.id.period_continue_button)
            val doneButton = findViewById<Button>(R.id.period_done_button)

            continueButton.text = labelContinue
            doneButton.text = labelDone

            continueButton.setOnClickListener { dismissAlert() }

            doneButton.setOnClickListener {
                completePeriodReminder(itemId, isMonthlyPeriod)
                dismissAlert()
            }
        }

        // Barrier overlay and draggable circle
        barrierOverlay = findViewById(R.id.barrier_overlay)
        draggableCircle = findViewById(R.id.draggable_circle)
        setupDragGesture()

        // Sync cleanup of any leftover player thread, then play locally.
        // Do NOT call ReminderSoundService.stop here — it sends an async ACTION_STOP
        // Intent that the service processes on the main looper AFTER onCreate returns,
        // and its handler then calls ReminderSoundPlayer.stop() which would kill the
        // thread we just started.
        ReminderSoundPlayer.stop()
        ReminderSoundPlayer.start(this, soundValue, repeatCount)
    }

    private fun setupWindowFlags() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun setupDragGesture() {
        draggableCircle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartY = view.y
                    initialY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    if (deltaY > 0) view.y = dragStartY + deltaY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - initialY
                    if (deltaY > 600) {
                        hideBarrier()
                    } else {
                        view.animate().y(dragStartY).setDuration(200).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun hideBarrier() {
        draggableCircle.setOnTouchListener(null)
        ReminderSoundPlayer.stop()
        ReminderSoundService.stop(this)
        barrierOverlay.animate().alpha(0f).setDuration(300)
            .withEndAction { barrierOverlay.visibility = View.GONE }.start()
        draggableCircle.animate().alpha(0f).setDuration(300)
            .withEndAction { draggableCircle.visibility = View.GONE }.start()
    }

    private fun snoozeReminder(itemId: Int, title: String, content: String, soundValue: String?, minutes: Int) {
        try {
            ReminderScheduler.scheduleSnooze(this, itemId, title, content, soundValue, minutes)
            Log.d(TAG, "Snoozed item $itemId for $minutes min")
        } catch (e: Exception) {
            Log.e(TAG, "Error snoozing: ${e.message}", e)
        }
        dismissAlert()
    }

    private fun completePeriodReminder(itemId: Int, isMonthlyPeriod: Boolean) {
        try {
            val app = applicationContext as MemListsApplication
            val repo = app.repository

            // Cancel all period alarms
            ReminderScheduler.cancelItem(this, itemId.toLong())

            if (!isMonthlyPeriod) {
                // Date-based period: deactivate
                repo.deactivateItemSync(itemId.toLong())
                Log.d(TAG, "Period item $itemId deactivated (date-based)")
            } else {
                // Monthly period: suppress until next period start
                val item = repo.getItemByIdSync(itemId.toLong())
                val startDay = item?.date
                if (startDay != null && startDay in 1..31) {
                    val cal = Calendar.getInstance()
                    val today = cal.get(Calendar.DAY_OF_MONTH)
                    if (today >= startDay) cal.add(Calendar.MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDay))
                    val doneUntil = cal.get(Calendar.YEAR) * 10000 +
                            (cal.get(Calendar.MONTH) + 1) * 100 +
                            cal.get(Calendar.DAY_OF_MONTH)
                    repo.updatePeriodDoneUntilSync(itemId.toLong(), doneUntil)
                    Log.d(TAG, "Monthly period item $itemId suppressed until $doneUntil")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing period: ${e.message}", e)
        }
    }

    private fun dismissAlert() {
        ReminderSoundPlayer.stop()
        ReminderSoundService.stop(this)
        finish()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Disabled — user must drag circle and tap OK
    }

    private val backCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.window.OnBackInvokedCallback {
            // Swallow predictive back gesture — user must drag circle and tap OK
        }
    } else null

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                backCallback!!
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReminderSoundPlayer.stop()
        ReminderSoundService.stop(this)
    }

    companion object {
        private const val TAG = "MemLists"
    }
}
