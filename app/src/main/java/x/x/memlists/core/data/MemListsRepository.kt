package x.x.memlists.core.data

import android.content.ContentValues
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemListsRepository(
    private val databaseHelper: MemListsDatabaseHelper
) {
    suspend fun loadSettings(): SettingsData = withContext(Dispatchers.IO) {
        val values = buildMap {
            databaseHelper.readableDatabase.query(
                "settings",
                arrayOf("key", "value"),
                null,
                null,
                null,
                null,
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    put(cursor.getString(0), cursor.getString(1))
                }
            }
        }

        val hasLanguage = values.containsKey(KEY_LANGUAGE)
        val hasTheme = values.containsKey(KEY_THEME)

        SettingsData(
            languageCode = values[KEY_LANGUAGE] ?: "en",
            themeName = values[KEY_THEME] ?: "Light",
            newestFirst = values[KEY_NEWEST_FIRST].asBoolean(default = true),
            remindersEnabled = values[KEY_ENABLE_REMINDERS].asBoolean(default = true),
            debugLogs = values[KEY_DEBUG_LOGS].asBoolean(default = false),
            defaultSound = values[KEY_DEFAULT_SOUND],
            soundRepeats = values[KEY_SOUND_REPEATS]?.toIntOrNull() ?: 25,
            hiddenPin = values[KEY_HIDDEN_PIN],
            autoSortDictionary = values[KEY_AUTO_SORT_DICT].asBoolean(default = true),
            largeFontWakeLock = values[KEY_LARGE_FONT_WAKELOCK].asBoolean(default = true),
            isFirstLaunch = !(hasLanguage && hasTheme)
        )
    }

    suspend fun saveSettings(settings: SettingsData) = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.apply {
            beginTransaction()
            try {
                putSetting(KEY_LANGUAGE, settings.languageCode)
                putSetting(KEY_THEME, settings.themeName)
                putSetting(KEY_NEWEST_FIRST, settings.newestFirst.toString())
                putSetting(KEY_ENABLE_REMINDERS, settings.remindersEnabled.toString())
                putSetting(KEY_DEBUG_LOGS, settings.debugLogs.toString())
                putOptionalSetting(KEY_DEFAULT_SOUND, settings.defaultSound)
                putSetting(KEY_SOUND_REPEATS, settings.soundRepeats.toString())
                putOptionalSetting(KEY_HIDDEN_PIN, settings.hiddenPin)
                putSetting(KEY_AUTO_SORT_DICT, settings.autoSortDictionary.toString())
                putSetting(KEY_LARGE_FONT_WAKELOCK, settings.largeFontWakeLock.toString())
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    suspend fun loadMemosHome(
        folder: MemoFolderType?,
        newestFirst: Boolean,
        hiddenMode: Boolean = false
    ): MemosHomeData = withContext(Dispatchers.IO) {
        val items = loadMemoItems(folder = folder, hiddenMode = hiddenMode, newestFirst = newestFirst)
        val folders = if (folder == null) loadMemoFolders(hiddenMode = hiddenMode) else emptyList()
        MemosHomeData(items = items, folders = folders)
    }

    suspend fun loadListsHome(parentId: Long?): ListsHomeData = withContext(Dispatchers.IO) {
        val currentFolderName = parentId?.let { folderId ->
            databaseHelper.readableDatabase.query(
                "lists",
                arrayOf("name"),
                "id = ?",
                arrayOf(folderId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }

        val selection = if (parentId == null) {
            "parent_id IS NULL"
        } else {
            "parent_id = ?"
        }
        val args = if (parentId == null) emptyArray() else arrayOf(parentId.toString())

        val containers = mutableListOf<ListContainerSummary>()
        databaseHelper.readableDatabase.query(
            "lists",
            arrayOf("id", "name", "comment", "parent_id", "is_folder", "pin", "sort_order"),
            selection,
            args,
            null,
            null,
            "sort_order ASC, name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isFolder = cursor.getInt(4) == 1
                val counts = if (isFolder) {
                    0 to 0
                } else {
                    loadListEntryCounts(id)
                }
                containers += ListContainerSummary(
                    id = id,
                    name = cursor.getString(1),
                    comment = cursor.getStringOrNull(2),
                    parentId = cursor.getLongOrNull(3),
                    isFolder = isFolder,
                    isLocked = !cursor.getStringOrNull(5).isNullOrBlank(),
                    uncheckedCount = counts.first,
                    totalCount = counts.second
                )
            }
        }

        ListsHomeData(
            currentFolderId = parentId,
            currentFolderName = currentFolderName,
            containers = containers
        )
    }

    private fun android.database.sqlite.SQLiteDatabase.putSetting(key: String, value: String) {
        val contentValues = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        insertWithOnConflict("settings", null, contentValues, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun android.database.sqlite.SQLiteDatabase.putOptionalSetting(key: String, value: String?) {
        if (value == null) {
            delete("settings", "key = ?", arrayOf(key))
        } else {
            putSetting(key, value)
        }
    }

    private fun String?.asBoolean(default: Boolean): Boolean {
        return when (this) {
            null -> default
            "true" -> true
            "false" -> false
            else -> default
        }
    }

    private fun loadMemoItems(
        folder: MemoFolderType?,
        hiddenMode: Boolean,
        newestFirst: Boolean
    ): List<MemoItemSummary> {
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        selectionParts += "hidden = ?"
        selectionArgs += if (hiddenMode) "1" else "0"

        when (folder) {
            MemoFolderType.Notes -> selectionParts += "reminder_type = 0"
            MemoFolderType.Daily -> selectionParts += "reminder_type = 2"
            MemoFolderType.Periods -> selectionParts += "reminder_type = 3"
            MemoFolderType.Monthly -> selectionParts += "reminder_type = 1 AND monthly = 1"
            MemoFolderType.Yearly -> selectionParts += "reminder_type = 1 AND yearly = 1"
            null -> Unit
        }

        val selection = selectionParts.joinToString(" AND ")
        val rows = mutableListOf<MemoItemSummary>()
        databaseHelper.readableDatabase.query(
            "items",
            arrayOf(
                "id",
                "title",
                "content",
                "tags",
                "priority",
                "created",
                "hidden",
                "reminder_type",
                "active",
                "date",
                "time",
                "times",
                "date_to",
                "days_mask",
                "yearly",
                "monthly"
            ),
            selection,
            selectionArgs.toTypedArray(),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows += cursor.toMemoItemSummary(photoCount = loadPhotoCount(cursor.getLong(0)))
            }
        }

        return rows.sortedWith(memoComparator(folder = folder, newestFirst = newestFirst))
    }

    private fun loadMemoFolders(hiddenMode: Boolean): List<MemoFolderSummary> {
        val hiddenValue = if (hiddenMode) 1 else 0
        val specs = listOf(
            MemoFolderType.Notes to "reminder_type = 0",
            MemoFolderType.Daily to "reminder_type = 2",
            MemoFolderType.Periods to "reminder_type = 3",
            MemoFolderType.Monthly to "reminder_type = 1 AND monthly = 1",
            MemoFolderType.Yearly to "reminder_type = 1 AND yearly = 1"
        )

        return specs.mapNotNull { (type, expression) ->
            val count = databaseHelper.readableDatabase.compileStatement(
                "SELECT COUNT(*) FROM items WHERE hidden = ? AND $expression"
            ).apply {
                bindLong(1, hiddenValue.toLong())
            }.simpleQueryForLong().toInt()

            if (count > 0) MemoFolderSummary(type = type, count = count) else null
        }
    }

    private fun loadPhotoCount(itemId: Long): Int {
        return databaseHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM photos WHERE owner_type = 'memo' AND owner_id = ?"
        ).apply {
            bindLong(1, itemId)
        }.simpleQueryForLong().toInt()
    }

    private fun loadListEntryCounts(listId: Long): Pair<Int, Int> {
        val total = databaseHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM entries WHERE list_id = ?"
        ).apply {
            bindLong(1, listId)
        }.simpleQueryForLong().toInt()

        val unchecked = databaseHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM entries WHERE list_id = ? AND is_checked = 0"
        ).apply {
            bindLong(1, listId)
        }.simpleQueryForLong().toInt()

        return unchecked to total
    }

    private fun Cursor.toMemoItemSummary(photoCount: Int): MemoItemSummary {
        return MemoItemSummary(
            id = getLong(0),
            title = getString(1),
            content = getStringOrNull(2),
            tags = getStringOrNull(3),
            priority = getInt(4),
            created = getInt(5),
            hidden = getInt(6) == 1,
            reminderType = getInt(7),
            active = getInt(8) == 1,
            date = getIntOrNull(9),
            time = getIntOrNull(10),
            timesJson = getStringOrNull(11),
            dateTo = getIntOrNull(12),
            daysMask = getIntOrNull(13),
            yearly = getInt(14) == 1,
            monthly = getInt(15) == 1,
            photoCount = photoCount
        )
    }

    private fun memoComparator(
        folder: MemoFolderType?,
        newestFirst: Boolean
    ): Comparator<MemoItemSummary> {
        return when (folder) {
            MemoFolderType.Yearly,
            MemoFolderType.Monthly,
            MemoFolderType.Periods -> compareBy<MemoItemSummary>({ it.date ?: Int.MAX_VALUE }, { it.time ?: Int.MAX_VALUE })
            MemoFolderType.Daily -> compareBy({ firstDailyTime(it.timesJson) ?: "99:99" }, { it.title.lowercase() })
            MemoFolderType.Notes -> {
                if (newestFirst) compareByDescending<MemoItemSummary> { it.created } else compareBy { it.created }
            }
            null -> {
                val today = todayAsInt()
                Comparator { left, right ->
                    compareValues(mainSortBucket(left, today), mainSortBucket(right, today))
                        .takeIf { it != 0 }
                        ?: compareValues(right.priority, left.priority).takeIf { it != 0 }
                        ?: compareDate(left.date, right.date, newestFirst).takeIf { it != 0 }
                        ?: compareDate(left.created, right.created, newestFirst)
                }
            }
        }
    }

    private fun mainSortBucket(item: MemoItemSummary, today: Int): Int {
        val date = item.date ?: return 2
        return when {
            date == today -> 0
            date > today -> 1
            else -> 2
        }
    }

    private fun compareDate(left: Int?, right: Int?, newestFirst: Boolean): Int {
        val leftValue = left ?: if (newestFirst) Int.MIN_VALUE else Int.MAX_VALUE
        val rightValue = right ?: if (newestFirst) Int.MIN_VALUE else Int.MAX_VALUE
        return if (newestFirst) compareValues(rightValue, leftValue) else compareValues(leftValue, rightValue)
    }

    private fun Cursor.getStringOrNull(index: Int): String? {
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getIntOrNull(index: Int): Int? {
        return if (isNull(index)) null else getInt(index)
    }

    private fun Cursor.getLongOrNull(index: Int): Long? {
        return if (isNull(index)) null else getLong(index)
    }

    companion object {
        private const val KEY_LANGUAGE = "Language"
        private const val KEY_THEME = "Color theme"
        private const val KEY_NEWEST_FIRST = "Newest first"
        private const val KEY_ENABLE_REMINDERS = "Enable reminders"
        private const val KEY_DEBUG_LOGS = "Debug logs"
        private const val KEY_DEFAULT_SOUND = "Default sound"
        private const val KEY_SOUND_REPEATS = "Sound repeats"
        private const val KEY_HIDDEN_PIN = "hiddpin"
        private const val KEY_AUTO_SORT_DICT = "auto_sort_dict"
        private const val KEY_LARGE_FONT_WAKELOCK = "large_font_wakelock"
    }
}
