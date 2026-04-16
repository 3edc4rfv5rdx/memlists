## MemLists
Android memory assistant

## Features
- English, Ukrainian, Russian localization (languages can be easily added)
- Record events with description, priority, tags, photos, dates and times
- Reminder types:
  - One-time reminders (with optional yearly/monthly repeat)
  - Daily reminders (multiple times per day, weekday selection)
  - Period reminders (date range with weekday mask)
- Fullscreen alert with sound looping, snooze (10min to 1 day), drag-to-dismiss
- Customizable sounds per item, always plays through speaker (bypasses Bluetooth)
- Virtual folders: Notes, Daily, Periods, Monthly, Yearly
- Filters by date range, priority, tags, reminder type; tag cloud
- Backup/restore in DB (SQLite) and CSV formats
- Color themes
* PIN-protected hidden area (accessed by tapping the header four times)

## Scripts
- `10-MakeRelease.sh` — build APK
- `20-MakeTag.sh` — git new tag
- `21-PushTag.sh` — git push with version tag
- `22-RelUpload.sh` — upload APK to GitHub release

Vibe coding with Anthropic Claude and OpenAI codex review :)
