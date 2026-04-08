# TODO

- Sound fade-in: gradual volume ramp-up at the start of reminder sound playback (softer alarm start instead of immediate full volume). Implement in FullScreenAlertActivity / ReminderSoundService — start at low volume, ramp up over N seconds.

## From external code review (2026-04-08)

- P2 SQLite migrations: `MemListsDatabaseHelper.onUpgrade()` is empty. Defer until first real schema change — at that point introduce `when (oldVersion)` skeleton and a real ALTER/CREATE migration in the same commit. Don't write the skeleton speculatively.
- P4 Tests: no `app/src/test` or `app/src/androidTest` exists yet. Highest-value targets after the recent reminder fixes:
  - `ReminderScheduler` — daysMask, monthly/yearly rollover, period date generation, isDayInMask edge cases
  - `ReminderReceiver` decision logic — master switch off, active=0, daily reschedule, isDaily propagation
  - `ReminderMaintenance` — auto-remove deferral, expired cleanup
  - `MemListsRepository` — sort/filter, bulk count helpers
- Minor leftovers from review (not blocking, not urgent):
  - Disabled menu items in UI (placeholder entries)
  - Single TODO comments in memo editor
  - Cosmetic Compose-layer cleanups
