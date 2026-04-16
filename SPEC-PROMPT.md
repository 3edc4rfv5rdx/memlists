# MemoLists — Technical Specification Prompt

You are building a mobile application called **MemLists** — a combined app that merges two modules into one shell: **Memos** (reminders, notes, events) and **Lists** (universal lists with a dictionary). Both modules share themes, localization, and common UI patterns but maintain separate databases.

---

## SPEC REVIEW STATUS — 2026-04-15

This spec was reviewed against the current codebase.

### Overall status

- **Memos core flow:** mostly implemented
- **Reminder editor / scheduler / fullscreen alert:** mostly implemented, with a few lifecycle risks
- **Memo photos:** mostly implemented
- **Welcome / theme / localization / settings / backup-restore:** mostly implemented
- **Lists module:** only partially implemented compared to this spec
- **Hidden mode / private mode:** largely specified here, but not actually wired in the current app flow

### Confirmed in code

- Shared single SQLite database with `items`, `settings`, `photos`, `lists`, `dictionary`, `entries`
- Welcome screen with language/theme selection
- Theme and localization loading from assets
- Memo editor with title/content/tags/priority/date/reminder controls
- Daily, one-time, and period reminder scheduling
- Reminder maintenance on app launch / restore / boot path
- Settings screen with reminder master switch, default sound, sound repeats, backup/restore, CSV export
- Memo photo gallery with camera/gallery import, fullscreen viewer, delete, share, temp-to-final move on save
- Tag cloud and user filters

### Spec/code mismatches and gaps

- **§2.5 Edit/Add Item Screen:** the spec says one-time reminder dates must be rejected if they are already in the past. Current editor validation checks required fields and formats, but does not block saving a past one-time reminder.
- **§2.9 Hidden Mode (Privacy):** this section is not implemented as described. The settings key exists, but the app flow reviewed here does not include the 4-tap activation flow, PIN prompts, orange private-mode app-bar behavior, or the 5-minute inactivity auto-exit described below.
- **§3 Lists Module:** the current Lists implementation covers the basic home/detail/create flows, but many advanced behaviors in this section are not present yet.

### Lists module gaps versus this spec

- No visible PIN lock flow for opening lists
- No long-press menus for home/list items
- No move into folder / remove from folder actions
- No edit flow for existing lists from the reviewed navigation
- No edit flow for existing list entries from the reviewed navigation
- No drag-reorder for unchecked entries
- No top-right menu with move/share/comment/delete-done/clear-done actions
- No large-font mode flow
- No list-entry photo flow in the reviewed UI

### Current code risks worth keeping in mind

- Snoozed reminders are scheduled as separate alarms and are not cancelled together with the original reminder in all lifecycle paths.
- Auto-deletion of expired one-time reminders can remove the memo without fully cleaning related photo rows/files.
- Non-fullscreen reminder playback uses a service timeout estimate, so long custom sounds may be cut off early.

### How to use this spec now

- Treat the **Memos**, **Reminder**, **Photos**, **Welcome**, **Settings**, and **Backup/Restore** parts as mostly aligned with the shipped code.
- Treat **Hidden Mode** and large parts of **Lists** as target-state spec, not as a precise description of the current implementation.

---

## 1. ARCHITECTURE OVERVIEW

### 1.1 Two Modules, One App

- The app launches into the **Memos** module by default.
- The **Lists** module is accessible via a navigation button ("Lists") on the Memos main screen. It appears as a permanent entry at the top of the item list. Tap opens Lists home screen; back button returns to Memos.
- Single SQLite database (`memlists.db`) with 6 tables:
  - `items` — memo records (see 2.1)
  - `settings` — key-value app settings (see 2.2)
  - `photos` — photos for both modules (see 2.3)
  - `lists` — lists and folders (see 3.1)
  - `dictionary` — reusable item templates (see 3.1)
  - `entries` — items within lists (see 3.1)
- Shared across modules: theme engine, localization system, font constants, color variables, backup/restore, photo utilities.
- **Code organization rule:** all shared UI components, dialogs, widgets, and utility functions must be extracted into separate shared files. No duplication between modules — common code lives in shared library files.

### 1.2 Shared UI Constants

**Font sizes:** Small=14, Normal=16 (default), Medium=18, Large=20, Title=24.
**Font weights:** Normal, Bold.
**Default font size:** All UI text uses Normal (16) unless explicitly specified otherwise.
**Buttons:** All dialog and picker buttons must have a background color and rounded corners.
**Color variables (applied from current theme):**
- `clText` — text color
- `clBgrnd` — background color
- `clUpBar` — app bar / top bar color
- `clFill` — fill / container color
- `clSel` — selection highlight color
- `clMenu` — menu / dropdown background color

### 1.3 Themes

Four built-in color themes loaded from a config file:

| Theme | Text | Background | AppBar | Fill | Selected | Menu |
|-------|------|-----------|--------|------|----------|------|
| Light | #000000 | #F5EFD5 | #E6C94C | #F9F3E3 | #FFCC80 | #ADD8E6 |
| Dark | #FFFFFF | #212121 | #424242 | #303030 | #616161 | #263238 |
| Blue | #000000 | #E3F2FD | #2196F3 | #BBDEFB | #90CAF9 | #CFD8DC |
| Green | #121E0A | #F3F7ED | #97BA60 | #FFFFFF | #4D4C6B3D | #D4E2C6 |

Default theme: Light. User selects theme on the welcome screen (first launch) or in settings; change applies immediately to all screens.

### 1.4 Localization

- Three languages: English (default), Russian, Ukrainian.
- All UI strings go through a translation function `lw(key)`.
- Translations stored in a single JSON file: key = English text, values for {ru, ua}.
- Punctuation is NOT stored in the localization file — added in code.
- Language selected in settings or on first-launch welcome screen.

### ✅ 1.5 First Launch

On first launch, a **Welcome Screen** appears:
- App icon, title, subtitle.
- User selects language (dropdown).
- User selects color theme (dropdown).
- "Start" button saves preferences and navigates to the main screen.

---

## 2. MEMOS MODULE

### 2.1 Data Model — Items Table

Each memo/reminder is stored with these fields:

**Core fields:**

| Field | Type | Description |
|-------|------|-------------|
| id | int, PK, auto | Unique identifier |
| title | text, required | Memo title |
| content | text | Detailed content (optional) |
| tags | text | Comma-separated tags (optional) |
| priority | int 0-3 | Priority level, default 0 |
| created | int YYYYMMDD | Creation date |
| hidden | int 0/1 | Private item (filtered by hidden mode) |

**Unified reminder fields:**

| Field | Type | Description |
|-------|------|-------------|
| reminder_type | int 0-3 | 0=none, 1=one-time, 2=daily, 3=period |
| active | int 0/1 | Reminder is active (can be paused), default 1 |
| date | int YYYYMMDD | Date for one-time / "from" for period |
| time | int HHMM | Time for one-time and period (e.g. 930=09:30) |
| times | text (JSON) | Array of times for daily: ["09:00", "14:00"] |
| date_to | int | Period end: day-of-month (1-31) or YYYYMMDD |
| days_mask | int bitmask | Days of week for daily and period: bit0=Mon...bit6=Sun, 127=all |
| sound | text | Custom sound URI (all types) |
| fullscreen | int 0/1 | Show fullscreen alert on reminder |
| loop_sound | int 0/1 | Loop sound in fullscreen mode (only meaningful when fullscreen=1), default 1 |
| yearly | int 0/1 | One-time modifier: repeats every year |
| monthly | int 0/1 | One-time modifier: repeats every month |
| remove | int 0/1 | One-time modifier: auto-remove after reminder fires |
| period_done_until | int YYYYMMDD | Period: completion marker |

**Field usage by reminder type:**

| Field | none (0) | one-time (1) | daily (2) | period (3) |
|-------|----------|-------------|-----------|------------|
| date | — | ✓ reminder date | — | ✓ from date |
| time | — | ✓ | — | ✓ |
| times | — | — | ✓ | — |
| date_to | — | — | — | ✓ end date |
| days_mask | — | — | ✓ | ✓ |
| sound | — | ✓ | ✓ | ✓ |
| fullscreen | — | ✓ | ✓ | ✓ |
| loop_sound | — | ✓ (if fullscreen) | ✓ (if fullscreen) | ✓ (if fullscreen) |
| yearly | — | ✓ | — | — |
| monthly | — | ✓ | — | — |
| remove | — | ✓ | — | — |
| period_done_until | — | — | — | ✓ |

### 2.2 Settings Table

Key-value table (`settings`) in the single shared database:

| Key | Default | Description |
|-----|---------|-------------|
| Language | "en" | App language |
| Color theme | "Light" | Active theme name |
| Newest first | "true" | Sort order preference |
| Enable reminders | "true" | Master reminder switch |
| Debug logs | "false" | Console/file logging |
| Default sound | null | Default reminder sound |
| Sound repeats | "10" | Fullscreen alert sound repetitions |
| hiddpin | text | PIN for hidden mode |
| auto_sort_dict | "true" | Auto-sort lists dictionary |
| large_font_wakelock | "true" | Keep screen on in large font mode |

### ✅ 2.3 Photos Table

Shared table for photos across both modules:

| Field | Type | Description |
|-------|------|-------------|
| id | int, PK, auto | Unique identifier |
| owner_type | text | "memo" or "entry" |
| owner_id | int | ID of the memo item or list entry |
| path | text | Absolute file path under app's internal `filesDir/photo/` |
| sort_order | int | Display order |

Photos stored in `{appDir}/photo/{owner_type}/{owner_id}/photo-{timestamp}.jpg`.
New items use temp directory; photos moved to permanent path after save.
Shared photo viewer widget for both modules (fullscreen, zoom, pan, pagination).
Max 10 photos per owner.
Cleanup: orphaned temp directories removed on app start.
Operations: add from camera/gallery, delete (with confirmation), view fullscreen, share to device gallery.

### 2.4 Main Screen (Home Page)

**App Bar:**
- Title: current section name (MemLists / Yearly / Notes / Daily / Monthly / Periods).
- Hidden mode indicator (eye-off icon) when private mode is active — app bar turns orange (#f29238).
- Buttons: Back (context-aware), Check Reminders, Filter Status indicator, Menu.

**Filter Status Indicator:**
- `(All)` — no filters active
- `(F)` — user filter active
- `(T)` — tag cloud filter active
- `(FT)` — both filters active

**Navigation:**
- **Lists** button is always first (before all items), opens the Lists module.
- Other folders appear at the **end** of the list (after all items), only if they contain items, in this order:
  1. **Notes** — items with reminder_type=0 (no reminder)
  2. **Daily Reminders** — items with reminder_type=2
  3. **Periods** — items with reminder_type=3
  4. **Monthly Events** — items with reminder_type=1 and monthly=1
  5. **Yearly Events** — items with reminder_type=1 and yearly=1
- Each folder shown as ListTile with colored circular icon, name, and item count.

Tapping a folder enters it (shows only matching items). The title changes to the folder name, and back button returns to the main view.

**Item Display** (each row shows):
- Checkbox "active" (only for items with any reminder — toggle active/inactive)
- Title (bold; red if today's date)
- Priority stars (★ icons, right of title, if priority > 0)
- Content preview with status icons: lock (if hidden), refresh (if yearly), calendar (if monthly)
- Tags line ("Tags: ...") if tags present
- Date + time line (if set; red if today or has reminder)
- Daily: days compact + times list (blue)
- Period: date range + time + days (teal)
- Photo icon with count badge (trailing, if photos attached)
- Row background: selection color if selected, semi-transparent red if today

**Item Actions:**
- Swipe right → edit
- Swipe left → delete
- Long-press → context menu: Edit, Copy, Delete

**Menu:**
- Clear All Filters
- Filters
- Tag Filter (Tag Cloud)
- Settings
- About
- Exit private mode (only in hidden mode)

**Add Button (FAB):**
- Creates a new memo (opens Edit screen with empty fields).
- Color: orange in hidden mode, app bar color otherwise.

**About Dialog:**
- App icon (large).
- Version string and build number.
- Copyright line with author and year.

### 2.5 Edit/Add Item Screen

Organized in sections:

**1. Title & Content:**
- Title text field (required, validated non-empty).
- Content text area (multi-line, optional).
- Tags field with comma separation.

**✅ 2. Photo Gallery:**
- Shows attached photos as thumbnails.
- Photo button → menu: Camera / Gallery.
- Tap photo → fullscreen viewer (swipe to navigate, counter overlay).
- Delete individual photos (with confirmation).
- Max 10 photos per item.

**3. Priority:**
- +/− buttons to adjust (range 0–3).
- Visual number display in bordered box.

**4. Date field** (hidden when Period mode is active — Period has its own From/To):
- Date text field with date-picker button and clear (×) button on the right.

**5. Reminder Section** (bordered container):
- Master checkbox: enable/disable all reminders for this item.
- Active checkbox: can pause without losing configuration.
- Type selector (vertical radio buttons): One-time / Daily / Period.

**Shared reminder controls** (shown for all types when reminder is enabled):
- Sound selector (system sounds, custom files).
- Fullscreen alert checkbox.
  - If fullscreen: Loop sound checkbox.

**One-time Reminder (reminder_type=1):**
- Time field with time-picker button and clear (×) button. Presets: Morning (09:30), Day (12:30), Evening (18:30).
- Yearly repeat checkbox.
- Monthly repeat checkbox.
- Auto-remove after firing checkbox (disabled if yearly or monthly is on).

**Daily Reminder (reminder_type=2):**
- List of times (add/remove, format HH:MM). Default first time: 09:00.
- Day checkboxes: Mon–Sun (days_mask bitmask, shortcuts: Every day, Weekdays, Weekend).

**Period Reminder (reminder_type=3):**
- Two modes: full dates (e.g. 2026.03.12–2026.03.21) or day-of-month (e.g. 12–21). Both fields must be the same format.
- From field with date-picker.
- To field with date-picker.
- Time field with time-picker button and clear (×) button. Presets: Morning/Day/Evening.
- Day checkboxes (Mon–Sun, same days_mask widget as daily).

**6. Privacy** (only visible in hidden mode):
- Hidden checkbox: marks item as private (hidden=1).

**7. Actions:** Save button in the top-right corner of the app bar. Cancel — back button (←), discards changes.

**Validation Rules:**
- Title cannot be empty.
- Reminder date cannot be in the past (for non-yearly/monthly).
- Daily reminder needs at least one time and one day.
- Period needs both dates, a time, and at least one day.
- From/to dates must be same format (both day-of-month or both full dates).

### 2.6 Filtering System

**Tag Cloud Screen:**
- Displays all tags as chips in wrap layout, sized by frequency (5 tiers: 15/17/19/22/26px based on relative count).
- Shows count in parentheses for each tag.
- Multi-select: selected tags highlighted.
- Selected tags displayed in fixed container at top.
- AppBar buttons: Clear, Apply, Cancel.
- Filtering logic: item must have ALL selected tags (AND).
- Tags are collected from all items (respecting hidden mode).

**User Filters Screen:**
- Date from — text field with calendar picker and clear button.
- Date to — text field with calendar picker and clear button.
- Tags — text field (comma-separated, AND logic) with tag picker button.
- Priority — +/− buttons (range 0–3) with star visualization.
- Has reminder — dropdown (Any / Yes / No).
- AppBar buttons: Reset (clear all), Apply, Cancel.
- If date-from > date-to, offer to swap.

**Filter Application Order:**
1. Hidden mode filter (show only hidden=1 in private mode, only hidden=0 otherwise).
2. Tag cloud filter.
3. Folder filter (if inside a folder).
4. User filter.

### 2.7 Sorting

**Main list sort order:**
1. Today's items first (date = today).
2. Future-dated items second.
3. Non-dated items last.
4. Within each group: priority DESC → date (per "Newest first" setting) → created date.

**Folder sorting:**
- Yearly: by date ASC, then time.
- Notes: by created date (per setting).
- Daily: by first time in `times` array ASC.
- Monthly: by date ASC.
- Periods: by date ASC.

### 2.8 Reminder System

Three reminder types (reminder_type 1/2/3), unified scheduling via platform notifications:

**One-Time (1):** Scheduled for specific date/time. If yearly=1, auto-advances year when past. If monthly=1, auto-advances month (handles month-end edge cases like Jan 31 → Feb 28).

**Daily (2):** Fires at each time in `times` array, only on enabled days (`days_mask`). Multiple times per day supported.

**Period (3):** Fires daily within date range (`date`–`date_to`), only on enabled days (`days_mask`). Tracks completion via `period_done_until` marker.

**Scheduling rules:**
- Only if "Enable reminders" is on (master setting).
- Only if active=1 on the item.
- Only if notification time is in the future.
- Fullscreen mode: shows large overlay alert, repeats sound N times (configurable).

**Daily maintenance (on app launch):**
- Remove expired: delete items where date < today AND remove=1 (only reminder_type=1 without yearly/monthly).
- Advance yearly: for reminder_type=1, yearly=1 items with past dates — increment year until date >= today.
- Advance monthly: for reminder_type=1, monthly=1 items with past dates — increment month (clamp day to month end).

### 2.9 Hidden Mode (Privacy)

**Activation:** 4 taps on the app title bar.
- If no PIN set: prompts to create 4+ character PIN.
- If PIN set: prompts to verify.
- On success: hidden mode activates, app bar turns orange.
- Auto-exits after 5 minutes of inactivity (timer resets on actions).

**Behavior:**
- Normal mode: hidden items are completely invisible (filtered out by hidden=1).
- Hidden mode: only hidden items are shown (normal items hidden).
- No obfuscation — data stored as plain text, privacy achieved by filtering only.
- PIN stored in settings.

### 2.10 Sounds

- System notification sounds retrieved from platform.
- Custom sounds stored in `{appDir}/Sounds/` (mp3, wav, ogg, m4a, aac).
- File picker to add custom sounds.
- Play/stop test buttons in all sound selectors (Settings and Edit Item).
- Per-item sound override, or use app default.

### 2.11 Settings Screen

- Language selector (dropdown).
- Color theme selector (dropdown).
- Newest first toggle.
- Enable reminders toggle (reschedules/cancels all on change).
- Default sound selector.
- Sound repeats (1–25, default 10, hard cap 26 cycles in code).
- Debug logs toggle.
- Create backup button.
- Restore from backup button.
- Export to CSV button.
- Items Dictionary link (opens Lists dictionary).
- Auto-sort dictionary toggle.
- Keep screen on toggle (for Lists large font mode).

---

## 3. LISTS MODULE

### 3.1 Data Model

**Three tables (in the shared `memlists.db`):**

**lists** (containers — lists and folders):

| Field | Type | Description |
|-------|------|-------------|
| id | int, PK, auto | Unique identifier |
| name | text, required | List or folder name |
| sort_order | int | Display order |
| comment | text | Optional note about the list |
| parent_id | int, FK → lists(id) | Parent folder ID (null = root level) |
| is_folder | int 0/1 | 0=list, 1=folder |
| pin | text | PIN code for protection (null = no lock) |

**dictionary** (reusable item templates):

| Field | Type | Description |
|-------|------|-------------|
| id | int, PK, auto | Unique identifier |
| name | text, required | Item name (e.g. "Apples") |
| unit | text | Default unit (e.g. "kg", "pcs") |
| sort_order | int | Dictionary display order |

**entries** (items within lists):

| Field | Type | Description |
|-------|------|-------------|
| id | int, PK, auto | Unique identifier |
| list_id | int, FK → lists(id) | Which list this belongs to (CASCADE delete) |
| dict_id | int, FK → dictionary(id) | Dictionary reference (SET NULL on delete) |
| name | text | Manual entry name (used if dict_id is null) |
| unit | text | Manual unit (used if dict_id is null) |
| quantity | text | Amount, e.g. "2", "500g" |
| is_checked | int 0/1 | Checked off / completed |
| sort_order | int | Order within list |

**Foreign keys enabled.** CASCADE delete on lists → entries. SET NULL on dictionary → entries (converts to manual entry).

**Entry types** determined by fields:
- **Dictionary entry:** `dict_id` is set — name/unit displayed from dictionary.
- **Manual entry:** `dict_id` is null — uses own `name`/`unit`.

### 3.2 PIN Lock

- Any list can be protected with a 4+ digit numeric PIN.
- PIN stored directly in `lists.pin` field (null = no lock).
- PIN required when opening the list from home screen.
- Lock/unlock via long-press context menu on home screen.
- Visual lock icon on protected lists.

### 3.3 Home Screen

**Root view:** shows top-level lists + all folders.
**Folder view:** shows only child lists of opened folder.

**Visual indicators:**
- Bold/larger font for lists with unchecked items.
- Strikethrough for fully completed lists (all items checked).
- Folder icon for folders.
- Lock icon for PIN-protected lists.

**Actions:**
- Tap list → open list screen (PIN check if locked).
- Tap folder → enter folder view.
- Long-press → context menu: Edit, Move to folder (if not a folder), Remove from folder (if in a folder), Lock/Unlock (lists only, not folders), Delete.
- Swipe right → edit, swipe left → delete.

**FABs:** Add List (main), Add Folder (mini).

**Folder rules:**
- Folders exist only at root level (no nesting).
- Lists can be moved into/out of folders.
- Deleting folder moves children to root (does not delete lists).

### 3.4 List Screen

**Two sections separated by a visual divider:**
1. **Unchecked items** (top) — reorderable via drag handles.
2. **Checked items** (bottom) — strikethrough text, not reorderable.

**Item display:**
- Checkbox.
- Item name (from dictionary or manual).
- Quantity + unit (concatenated, e.g. "2kg").
- Photo thumbnail (if attached).

**Item actions:**
- Tap checkbox → toggle checked status.
- Swipe right → edit, swipe left → delete.
- Drag handle → reorder (unchecked only).

**Add Item dialog:**
- Name field with real-time dictionary search (case-insensitive, max 20 results).
- Clickable search results to select from dictionary.
- Quantity field (optional).
- Unit field (optional, pre-filled from dictionary).
- Duplicate detection (warns if item already in list).

**Edit Item dialog:**
- Same fields as add, but pre-filled.
- Can change dictionary linkage.

**Item long-press menu:**
- Edit
- Add photo (shows count if photos exist)
- View photos (only if photos exist)
- Remove photo (only if photos exist)
- Delete

**Top-right menu:**
- Move items → move/copy screen.
- Share List → export as text.
- Comment → edit list note.
- Delete done → remove checked items (only if checked items exist).
- Clear done → uncheck all checked items (only if checked items exist).

**Large Font Mode:**
- Toggle via long-press on title or long-press on FAB.
- Item font size increases to 33px.
- Checkboxes scaled 1.5×.
- Optional screen wakelock (keep screen on).
- Toast notification on toggle.
- Resets on screen exit.

**Comment system:**
- Optional note attached to list.
- Editable via menu.
- Displayed at bottom of list after divider.

### 3.5 Sharing

Text export format:
```
* =List Name:
> Active Item 1  2kg
> Active Item 2
-------
x Completed Item 1
x Completed Item 2
```

Options dialog: unchecked only / all items, include comment yes/no.

### 3.6 Move/Copy Items Screen

- Checkboxes to select items (select all / unselect all).
- Mode toggle: checkbox "Copy" (unchecked = Move mode by default, checked = Copy mode).
- Destination dropdown (all lists except current, sorted by name).
- Move: transfers items, resets checked status.
- Copy: creates new items, resets checked status.
- Duplicate detection: skips items already in destination (by dict_id for dictionary items, by name+quantity+unit for manual entries).
- Reports count of moved/copied + skipped duplicates.

### 3.7 Items Dictionary Screen

- Full list of dictionary items, format: "Item Name /unit" (unit shown only if set).
- Search (real-time, case-insensitive).
- Add: name (auto-capitalized first letter) + optional unit.
- Edit: name + unit.
- Delete: detaches from all lists (linked entries become manual with name/unit copied).
- Reorder: drag handles (disabled during search).
- Sort alphabetically button.
- Duplicate prevention (case-insensitive).
- Auto-sort on add (if setting enabled).
- Swipe right → edit, swipe left → delete.

### 3.8 Photos (Lists Module)

Uses shared photos table (see 2.3) with `owner_type="entry"`.
Storage: `{appDir}/photo/entry/{entry_id}/`.
- Up to 10 photos per list entry.
- Camera capture or gallery pick.
- Shared fullscreen viewer (same as memos).
- Delete: dialog offers "Move to device gallery" or "Delete permanently".
- Thumbnails: 30px normal, 40px large font mode.

---

## 4. BACKUP, RESTORE & EXPORT

### ✅ 4.1 Backup & Restore

**Backup directory:** `Documents/Memlists/bak-{YYYYMMDD}/x{HHMMSS}/`

**Contents:**
- `memlists.db` — single database (all tables)
- `photo/` — all photos (memos and lists)
- `sounds/` — custom notification sounds

**Create process:**
1. Request storage permission.
2. WAL checkpoint for database integrity.
3. Copy database file.
4. Copy photo and sound directories.
5. Show success message.

**Restore process:**
1. Scan for available backups, show selection dialog.
2. Warn: "Will replace all current data."
3. Close database, delete WAL/SHM files.
4. Copy backup files to app directories.
5. Reopen database with migration support.
6. Restore photos and sounds.
7. Reschedule all reminders from restored data.
8. Show "Please restart app" message.

Multiple backups retained (no auto-cleanup). User selects which to restore.

### ✅ 4.2 CSV Export

Separate menu item in Settings — human-readable data export (not for restore).

**Exports:**
- `memos-items.csv` — all memo records.
- `lists.csv` — all lists and folders.
- `dictionary.csv` — dictionary items.
- `entries.csv` — all list entries.

**Export directory:** `Documents/Memlists/csv-{YYYYMMDD}/`

---

## 5. VERSION SYSTEM

- `build_number.txt` stores: `base_version`, `build` (counter), `version`.
- Version format: `{base_version}.{YYYYMMDD}+{build}` (e.g. `0.4.20260326+143`).
- Build script increments build counter, stamps date, builds APK, creates git tag.

---

## 6. PLATFORM REQUIREMENTS

### Permissions Needed:
- **Notifications** — for all reminders.
- **Storage** (manage external) — for backup/restore.
- **Camera** — for photo capture.
- **Gallery/Photos** — for photo pick and save.
- **System Alert Window** — for fullscreen reminder alerts.
- **Wakelock** — for large font mode screen-on.

### Platform Channels (Native Integration):
- Notification scheduling (specific, daily, period).
- Notification cancellation.
- System sound enumeration.
- Sound playback/stop.
- Permission requests and status checks.

---

## 7. KEY BEHAVIORS & EDGE CASES

1. **Monthly repeat edge cases:** Jan 31 monthly → Feb 28/29 → Mar 31 (preserves original day, clamps to month end).
2. **Leap year:** Feb 29 yearly events handled by date validation.
3. **Hidden mode privacy:** filtering only, no encryption — data stored as plain text in app-private database.
4. **Tag search:** simple substring/contains match.
5. **Hidden mode auto-exit:** 5-minute inactivity timer.
6. **Dictionary deletion cascade:** linked entries become manual (name/unit copied from dictionary).
7. **Large font mode reset:** resets when leaving list screen.
8. **Checked items:** not reorderable, shown below divider with strikethrough.
9. **Filter persistence:** user filter and tag filter are independent, both shown in status indicator.
10. **Folder filter isolation:** entering a folder saves and clears user filter, exiting restores it.

---

## 8. MESSAGE/NOTIFICATION TYPES

Unified snackbar message system with 6 color levels:
- **Default** — neutral info
- **Red** — error / warning
- **Green** — success
- **Blue** — information
- **Orange** — caution / attention
- **Purple** — special action

---

## 9. NAVIGATION MAP

```
App Launch
├─ Welcome Screen (first launch only)
│   └─ → Memos Home
│
└─ Memos Home (main screen)
    ├─ [Lists button] → Lists Home
    │   ├─ [Folder] → Lists Home (folder view)
    │   ├─ [List] → List Screen
    │   │   ├─ Add/Edit Item Dialog
    │   │   ├─ Move/Copy Screen
    │   │   └─ Photo Viewer
    │   └─ Items Dictionary Screen
    │
    ├─ [Folder: Notes/Yearly/Daily/Monthly/Periods]
    │   └─ Filtered memo list
    │
    ├─ [Memo Item] → Edit Item Screen
    │   └─ Photo Viewer
    │
    ├─ Filters Screen
    ├─ Tags Cloud Screen
    ├─ Settings Screen
    └─ About Dialog
```
