package x.x.memlists.core.reminder

object IntentExtras {
    const val ITEM_ID = "itemId"
    const val TITLE = "title"
    const val BODY = "body"
    const val CONTENT = "content"
    const val SOUND = "sound"
    const val HOUR = "hour"
    const val MINUTE = "minute"
    const val DAYS_MASK = "daysMask"
    const val NOTIFICATION_ID = "notificationId"
    const val IS_DAILY = "isDaily"
    const val IS_PERIOD = "isPeriod"
    const val IS_MONTHLY_PERIOD = "isMonthlyPeriod"
    const val LOOP_SOUND = "loopSound"
    const val REPEAT_COUNT = "repeatCount"
    const val LABEL_REMINDER = "label_reminder"
    const val LABEL_POSTPONE = "label_postpone"
    const val LABEL_MIN = "label_min"
    const val LABEL_HOUR = "label_hour"
    const val LABEL_HOURS = "label_hours"
    const val LABEL_DAY = "label_day"
    const val LABEL_CONTINUE = "label_continue"
    const val LABEL_DONE = "label_done"
}

object ReminderActions {
    private const val PREFIX = "x.x.memlists."
    const val SPECIFIC_REMINDER = "${PREFIX}SPECIFIC_REMINDER"
    const val DAILY_REMINDER = "${PREFIX}DAILY_REMINDER"
    const val PERIOD_REMINDER = "${PREFIX}PERIOD_REMINDER"
    const val SNOOZED_REMINDER = "${PREFIX}SNOOZED_REMINDER"
    const val STOP_SOUND = "${PREFIX}STOP_SOUND"
}
