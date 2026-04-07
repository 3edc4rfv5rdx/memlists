# Reminder system testing plan

## Basic flow
- Create one-time reminder (future date+time) -> alarm fires -> notification appears
- Create one-time reminder with fullscreen=on -> fullscreen alert appears on lock screen
- Create daily reminder with specific times -> fires at each scheduled time
- Create period reminder (date range) -> fires on each day in range

## FullScreenAlertActivity
- Barrier circle: drag down 600px+ to reveal OK button
- OK button: dismisses alert, stops sound
- Back button: disabled (must use barrier + OK)
- Snooze buttons: 10/20/30 min, 1/3 hours, 1 day -> alarm reschedules
- Period mode: Continue (dismiss, alarms continue) / Done (cancel remaining)

## Sound
- Sound plays via ReminderSoundService (foreground service, not local MediaPlayer)
- Persistent notification with Stop button always visible while sound plays
- If fullscreen alert dismissed accidentally -> service notification stays, sound can be stopped
- Loop sound: repeats with 2-second pause between cycles
- Sound routes to built-in speaker (bypasses Bluetooth)

## Recurring
- Yearly reminder: after fire, date advances to next year, alarm rescheduled
- Monthly reminder: after fire, date advances to next month (day clamped)
- Auto-remove: one-time non-recurring reminder deleted after fire

## Boot / maintenance
- Reboot device -> all alarms rescheduled (BootReceiver)
- App launch -> expired items deleted, yearly/monthly dates advanced, all alarms rescheduled

## Verify speaker routing
- Sound must play through built-in speaker even with headphones/Bluetooth connected
- SoundUtils.routeToSpeaker() handles this via setPreferredDevice(TYPE_BUILTIN_SPEAKER)

## Future
- Gradual volume increase (fade-in) for reminder sound

## Edge cases
- Reminder in the past -> skipped (not fired)
- Daily with day mask -> only fires on selected days
- Period with periodDoneUntil -> suppressed until that date
- Reminders globally disabled in settings -> nothing fires
