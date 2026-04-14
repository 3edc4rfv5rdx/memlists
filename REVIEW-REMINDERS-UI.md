## Reminder UI review

Scope: reminders UI only.
Date: 2026-04-08

General impression:
- Reminder editor is already functional and structured well enough for real use.
- Fullscreen alert UI is still rough and feels closer to a prototype than to a finished product.

Main remarks:

1. The reminder editor flow is too technical.
   File: `app/src/main/java/x/x/memlists/feature/memos/MemoEditorScreen.kt`
   Area: `ReminderSection`
   Notes:
   - The form presents several low-level switches in a row: `Active`, `Fullscreen alert`, `Loop sound`, sound picker, and only then the scenario-specific fields.
   - This reads more like a configuration panel than a user flow.
   - Better order would be: when -> recurrence/details -> sound/alert behavior -> advanced flags.

2. Period reminder input is visually ambiguous.
   File: `app/src/main/java/x/x/memlists/feature/memos/MemoEditorScreen.kt`
   Lines: around period `From` / `To` fields
   Notes:
   - The fields look like normal date inputs.
   - In reality they support two different formats: full date and day-of-month.
   - That behavior is powerful but not discoverable from the UI.

3. Daily time chips have weak affordance for removal.
   File: `app/src/main/java/x/x/memlists/feature/memos/MemoEditorScreen.kt`
   Lines: around `DailyTimesEditor`
   Notes:
   - Each time is shown as a button, but deletion is hidden in a small clear icon inside it.
   - The tap target for remove is small.
   - The chip visually suggests one action, while interaction is split into two zones.

4. Fullscreen alert layout is rigid and likely fragile on small screens / long translations.
   File: `app/src/main/res/layout/activity_fullscreen_alert.xml`
   Notes:
   - Many controls use fixed widths/heights such as `70dp`, `40dp`, `200dp`.
   - The whole screen relies on hardcoded proportions and spacing.
   - Long localized labels can overflow or make the layout feel cramped.

5. Fullscreen alert visual design is very rough.
   File: `app/src/main/res/layout/activity_fullscreen_alert.xml`
   Notes:
   - Large flat orange background, black text, and uniformly styled buttons feel like a prototype.
   - Hierarchy is functional but not refined.
   - The lock + arrow drag affordance is understandable, but overall polish is still low.

6. The editor is functional, but the reminder section could use stronger grouping.
   File: `app/src/main/java/x/x/memlists/feature/memos/MemoEditorScreen.kt`
   Area: `ReminderSection`
   Notes:
   - Recurrence mode, sound behavior, and exceptional flags all live in one continuous block.
   - Section headers or clearer grouping would reduce cognitive load.

Summary:
- Reminder editor: usable, but still too configuration-heavy in presentation.
- Fullscreen alert: concept is clear, but layout and styling need a proper UI pass.
