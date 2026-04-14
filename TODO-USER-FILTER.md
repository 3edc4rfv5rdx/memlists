# User Filter — implementation plan

Spec: SPEC-PROMPT.md §2.6 (User Filters Screen), §2.9.9–10.

## Data model

### 1. `UserFilter` data class
- `dateFrom: Int?` (YYYYMMDD)
- `dateTo: Int?`
- `tags: List<String>` (AND)
- `priorityMin: Int` (0..3, default 0)
- `hasReminder: HasReminder` (Any / Yes / No)
- `isActive: Boolean` computed — any field non-default

### 2. `MemosUiState` extend
- `userFilter: UserFilter = UserFilter()`
- `savedFolderUserFilter: UserFilter? = null` — stash on folder entry per §2.9.10

## ViewModel

### 3. `MemosViewModel`
- `setUserFilter(f)` / `clearUserFilter()` — update state + `reloadFolders()` + reload items
- `openFolder`: stash current userFilter → savedFolderUserFilter, clear userFilter, then load
- `leaveFolder`: restore userFilter from savedFolderUserFilter, then load

## Repository

### 4. `loadMemoFolders` / `loadMemoItems` accept `userFilter`
- SQL: date range `date BETWEEN ? AND ?`, priority `priority >= ?`, reminder `reminder_type != 0 / = 0`
- Tags: client-side (same as tag-cloud folder counts) or SQL LIKE chain
- Apply order: hidden → tag cloud → folder → user filter

## UI

### 5. `UserFiltersScreen.kt`
Every text field: trailing picker icon + clear (x) icon.
- Date from / Date to — `CompactOutlinedField` + calendar picker icon + clear icon
- Tags — `CompactOutlinedField` (comma-separated) + `#` → `TagDictionaryDialog` (extract to shared `core/ui` with `appendTag`) + clear icon
- Priority — `(-) N (+)` row with stars
- Has reminder — `DropdownCard` (Any / Yes / No)
- AppBar: Back + Reset icon + Apply icon
- If `dateFrom > dateTo` on Apply — auto-swap and show `SnackbarTone.Info` (blue) "Dates swapped"

### 6. `MemosHomeScreen`
- Status indicator `(All)/(F)/(T)/(FT)` derived from `selectedTags.isNotEmpty()` + `userFilter.isActive`
- Menu "Filters" enabled → `onOpenFilters()`
- "Clear All Filters" clears both tag filter and userFilter

## Navigation

### 7. `MemListsApp.kt`
- New route `Routes.Filters`
- `composable(Routes.Filters)`: get shared `MemosViewModel` via `viewModel(parentEntry)` (same pattern as TagCloud)
- `onOpenFilters = { navController.navigate(Routes.Filters) }`

## Localization

### 8. i18n.json
- `Filters`, `Date from`, `Date to`, `Priority`, `Has reminder`, `Any`, `Yes`, `No`, `Reset`, `Swap dates?`, indicators `(F)`, `(FT)`
- Already present: `Apply`, `Clear`, `Cancel`, `Tags`, `Tag Filter`

## Order

1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
