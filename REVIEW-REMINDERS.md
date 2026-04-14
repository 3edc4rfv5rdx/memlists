## Reminder review

Scope: reminders / sound / scheduling only.
Date: 2026-04-08

1. Critical: fullscreen reminder can be silent when `loopSound = false`.
   File: `app/src/main/java/x/x/memlists/FullScreenAlertActivity.kt`
   Lines: around 141-145
   Problem: local playback starts only when `loopSound || repeatCount == 1`.
   Impact: with fullscreen alert, `loopSound = false`, and normal `repeatCount > 1`, the alert screen opens without any sound.

2. Critical: monthly period reminders expire after the current scheduling horizon.
   File: `app/src/main/java/x/x/memlists/core/reminder/ReminderScheduler.kt`
   Lines: around 165-205, 315-345
   Problem: monthly period alarms are scheduled only for the current and next month (`monthOffset in 0..1`).
   Impact: if the app is not launched again, those reminders stop firing after the pre-scheduled window is exhausted.

3. High: pressing `Done` on a monthly period cancels alarms but does not schedule future ones again.
   File: `app/src/main/java/x/x/memlists/FullScreenAlertActivity.kt`
   Lines: around 213-234
   Problem: `ReminderScheduler.cancelItem(...)` is called, then only `period_done_until` is updated.
   Impact: the reminder can remain inactive until next app launch / boot maintenance instead of continuing automatically.

4. Medium: editor accepts invalid date/time values, and scheduler normalizes them silently.
   File: `app/src/main/java/x/x/memlists/feature/memos/MemoEditorScreen.kt`
   Lines: around 1256-1275
   File: `app/src/main/java/x/x/memlists/core/reminder/ReminderScheduler.kt`
   Lines: around 277-292
   Problem: `toDbDateInt()` accepts any 8 digits, `toDbTimeInt()` accepts any 4 digits, and scheduler clamps invalid days to month max.
   Impact: values like `2026-02-31` or `29:99` can be saved and then fire on a different real date/time than the user entered.

5. Medium: foreground sound service may stop long custom sounds too early.
   File: `app/src/main/java/x/x/memlists/core/reminder/ReminderSoundService.kt`
   Lines: around 75-85
   File: `app/src/main/java/x/x/memlists/core/reminder/ReminderSoundPlayer.kt`
   Lines: around 44-69
   Problem: service self-stop uses a fixed estimate (`cycles * 10s + 5s`), while actual playback duration depends on `MediaPlayer.duration`.
   Impact: long custom sounds can be cut off before playback actually finishes.

Open question:
- Item 3 assumes monthly period reminders are expected to continue without requiring app relaunch. If that is intentional product behavior, downgrade it; otherwise it is a real scheduling bug.
