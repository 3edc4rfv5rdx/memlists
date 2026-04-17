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
            soundRepeats = values[KEY_SOUND_REPEATS]?.toIntOrNull() ?: 10,
            hiddenPin = values[KEY_HIDDEN_PIN],
            largeFontWakeLock = values[KEY_LARGE_FONT_WAKELOCK].asBoolean(default = true),
            isFirstLaunch = !(hasLanguage && hasTheme),
            timeMorning = values[KEY_TIME_MORNING] ?: "09:30",
            timeDay = values[KEY_TIME_DAY] ?: "12:30",
            timeEvening = values[KEY_TIME_EVENING] ?: "18:30"
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
                putSetting(KEY_LARGE_FONT_WAKELOCK, settings.largeFontWakeLock.toString())
                putSetting(KEY_TIME_MORNING, settings.timeMorning)
                putSetting(KEY_TIME_DAY, settings.timeDay)
                putSetting(KEY_TIME_EVENING, settings.timeEvening)
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    suspend fun loadMemosHome(
        folder: MemoFolderType?,
        newestFirst: Boolean,
        hiddenMode: Boolean = false,
        selectedTags: Set<String> = emptySet(),
        userFilter: x.x.memlists.feature.memos.UserFilter = x.x.memlists.feature.memos.UserFilter()
    ): MemosHomeData = withContext(Dispatchers.IO) {
        val items = loadMemoItems(
            folder = folder,
            hiddenMode = hiddenMode,
            newestFirst = newestFirst,
            selectedTags = selectedTags,
            userFilter = userFilter
        )
        val folders = if (folder == null) {
            loadMemoFolders(
                hiddenMode = hiddenMode,
                selectedTags = selectedTags,
                userFilter = userFilter
            )
        } else emptyList()
        MemosHomeData(items = items, folders = folders)
    }

    suspend fun loadTodayReminderItems(hiddenMode: Boolean = false): List<MemoItemSummary> = withContext(Dispatchers.IO) {
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
                "monthly",
                "period_done_until"
            ),
            "hidden = ? AND reminder_type != 0 AND active = 1",
            arrayOf(if (hiddenMode) "1" else "0"),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows += MemoItemSummary(
                    id = cursor.getLong(0),
                    title = cursor.getString(1) ?: "",
                    content = cursor.getStringOrNull(2),
                    tags = cursor.getStringOrNull(3),
                    priority = cursor.getInt(4),
                    created = cursor.getInt(5),
                    hidden = cursor.getInt(6) == 1,
                    reminderType = cursor.getInt(7),
                    active = cursor.getInt(8) == 1,
                    date = cursor.getIntOrNull(9),
                    time = cursor.getIntOrNull(10),
                    timesJson = cursor.getStringOrNull(11),
                    dateTo = cursor.getIntOrNull(12),
                    daysMask = cursor.getIntOrNull(13),
                    yearly = cursor.getInt(14) == 1,
                    monthly = cursor.getInt(15) == 1,
                    photoCount = 0
                ).takeIf {
                    isReminderDueToday(
                        reminderType = cursor.getInt(7),
                        date = cursor.getIntOrNull(9),
                        time = cursor.getIntOrNull(10),
                        timesJson = cursor.getStringOrNull(11),
                        dateTo = cursor.getIntOrNull(12),
                        daysMask = cursor.getIntOrNull(13),
                        periodDoneUntil = cursor.getIntOrNull(16)
                    )
                } ?: continue
            }
        }

        rows.sortedWith(todayReminderComparator())
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

        val rawRows = mutableListOf<RawListRow>()
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
                rawRows += RawListRow(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    comment = cursor.getStringOrNull(2),
                    parentId = cursor.getLongOrNull(3),
                    isFolder = cursor.getInt(4) == 1,
                    isLocked = !cursor.getStringOrNull(5).isNullOrBlank()
                )
            }
        }

        val nonFolderIds = rawRows.filter { !it.isFolder }.map { it.id }
        val countsById = loadListEntryCountsBulk(nonFolderIds)
        val containers = rawRows.map { row ->
            val (unchecked, total) = if (row.isFolder) 0 to 0 else countsById[row.id] ?: (0 to 0)
            ListContainerSummary(
                id = row.id,
                name = row.name,
                comment = row.comment,
                parentId = row.parentId,
                isFolder = row.isFolder,
                isLocked = row.isLocked,
                uncheckedCount = unchecked,
                totalCount = total
            )
        }

        ListsHomeData(
            currentFolderId = parentId,
            currentFolderName = currentFolderName,
            containers = containers
        )
    }

    suspend fun insertMemo(
        title: String,
        content: String?,
        tags: String?,
        priority: Int,
        date: Int?,
        reminderType: Int,
        active: Boolean,
        time: Int?,
        timesJson: String?,
        dateTo: Int?,
        daysMask: Int?,
        soundUri: String?,
        fullscreen: Boolean,
        loopSound: Boolean,
        yearly: Boolean,
        monthly: Boolean,
        remove: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", title)
            put("content", content)
            put("tags", tags)
            put("priority", priority)
            put("created", todayAsInt())
            put("hidden", 0)
            put("reminder_type", reminderType)
            put("active", if (active) 1 else 0)
            if (date == null) {
                putNull("date")
            } else {
                put("date", date)
            }
            if (time == null) {
                putNull("time")
            } else {
                put("time", time)
            }
            put("times", timesJson)
            if (dateTo == null) {
                putNull("date_to")
            } else {
                put("date_to", dateTo)
            }
            if (daysMask == null) {
                putNull("days_mask")
            } else {
                put("days_mask", daysMask)
            }
            if (soundUri == null) {
                putNull("sound")
            } else {
                put("sound", soundUri)
            }
            put("fullscreen", if (fullscreen) 1 else 0)
            put("loop_sound", if (loopSound) 1 else 0)
            put("yearly", if (yearly) 1 else 0)
            put("monthly", if (monthly) 1 else 0)
            put("remove", if (remove) 1 else 0)
            putNull("period_done_until")
        }
        databaseHelper.writableDatabase.insertOrThrow("items", null, values)
    }

    suspend fun loadMemoForEdit(id: Long): MemoEditable? = withContext(Dispatchers.IO) {
        databaseHelper.readableDatabase.query(
            "items",
            arrayOf(
                "id", "title", "content", "tags", "priority",
                "date", "reminder_type", "active", "time", "times",
                "date_to", "days_mask", "sound", "fullscreen", "loop_sound",
                "yearly", "monthly", "remove"
            ),
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            MemoEditable(
                id = cursor.getLong(0),
                title = cursor.getString(1) ?: "",
                content = if (cursor.isNull(2)) null else cursor.getString(2),
                tags = if (cursor.isNull(3)) null else cursor.getString(3),
                priority = cursor.getInt(4),
                date = if (cursor.isNull(5)) null else cursor.getInt(5),
                reminderType = cursor.getInt(6),
                active = cursor.getInt(7) == 1,
                time = if (cursor.isNull(8)) null else cursor.getInt(8),
                timesJson = if (cursor.isNull(9)) null else cursor.getString(9),
                dateTo = if (cursor.isNull(10)) null else cursor.getInt(10),
                daysMask = if (cursor.isNull(11)) null else cursor.getInt(11),
                soundUri = if (cursor.isNull(12)) null else cursor.getString(12),
                fullscreen = cursor.getInt(13) == 1,
                loopSound = if (cursor.isNull(14)) true else cursor.getInt(14) == 1,
                yearly = cursor.getInt(15) == 1,
                monthly = cursor.getInt(16) == 1,
                remove = cursor.getInt(17) == 1
            )
        }
    }

    suspend fun updateMemo(
        id: Long,
        title: String,
        content: String?,
        tags: String?,
        priority: Int,
        date: Int?,
        reminderType: Int,
        active: Boolean,
        time: Int?,
        timesJson: String?,
        dateTo: Int?,
        daysMask: Int?,
        soundUri: String?,
        fullscreen: Boolean,
        loopSound: Boolean,
        yearly: Boolean,
        monthly: Boolean,
        remove: Boolean
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", title)
            put("content", content)
            put("tags", tags)
            put("priority", priority)
            put("reminder_type", reminderType)
            put("active", if (active) 1 else 0)
            if (date == null) putNull("date") else put("date", date)
            if (time == null) putNull("time") else put("time", time)
            put("times", timesJson)
            if (dateTo == null) putNull("date_to") else put("date_to", dateTo)
            if (daysMask == null) putNull("days_mask") else put("days_mask", daysMask)
            if (soundUri == null) putNull("sound") else put("sound", soundUri)
            put("fullscreen", if (fullscreen) 1 else 0)
            put("loop_sound", if (loopSound) 1 else 0)
            put("yearly", if (yearly) 1 else 0)
            put("monthly", if (monthly) 1 else 0)
            put("remove", if (remove) 1 else 0)
            putNull("period_done_until")
        }
        databaseHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    suspend fun deleteMemo(id: Long) = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.delete("items", "id = ?", arrayOf(id.toString()))
    }

    suspend fun toggleMemoActive(id: Long, currentActive: Boolean) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("active", if (currentActive) 0 else 1)
        }
        databaseHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    suspend fun loadKnownTags(): List<String> = withContext(Dispatchers.IO) {
        val tags = linkedSetOf<String>()
        databaseHelper.readableDatabase.query(
            "items",
            arrayOf("tags"),
            "tags IS NOT NULL AND tags != ''",
            null,
            null,
            null,
            "tags COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(0)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.forEach { tags += it }
            }
        }
        tags.toList().sortedBy { it.lowercase() }
    }

    suspend fun loadTagCloud(hiddenMode: Boolean = false): Map<String, Int> = withContext(Dispatchers.IO) {
        val counts = mutableMapOf<String, Int>()
        databaseHelper.readableDatabase.query(
            "items",
            arrayOf("tags"),
            "tags IS NOT NULL AND tags != '' AND hidden = ?",
            arrayOf(if (hiddenMode) "1" else "0"),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getStringOrNull(0)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.forEach { tag ->
                        val key = tag.lowercase()
                        counts[key] = (counts[key] ?: 0) + 1
                    }
            }
        }
        counts.toSortedMap()
    }

    suspend fun insertList(
        name: String,
        comment: String?,
        isFolder: Boolean,
        parentId: Long?
    ): Long = withContext(Dispatchers.IO) {
        val sortOrder = databaseHelper.readableDatabase.compileStatement(
            "SELECT COALESCE(MAX(sort_order), 0) FROM lists WHERE " +
                if (parentId == null) "parent_id IS NULL" else "parent_id = ?"
        ).apply {
            if (parentId != null) {
                bindLong(1, parentId)
            }
        }.simpleQueryForLong().toInt() + 1

        val values = ContentValues().apply {
            put("name", name)
            put("comment", comment)
            put("sort_order", sortOrder)
            if (parentId == null) {
                putNull("parent_id")
            } else {
                put("parent_id", parentId)
            }
            put("is_folder", if (isFolder) 1 else 0)
            putNull("pin")
        }
        databaseHelper.writableDatabase.insertOrThrow("lists", null, values)
    }

    data class ListEditRow(val name: String, val comment: String?, val isFolder: Boolean)

    suspend fun loadListById(listId: Long): ListEditRow = withContext(Dispatchers.IO) {
        databaseHelper.readableDatabase.query(
            "lists",
            arrayOf("name", "comment", "is_folder"),
            "id = ?",
            arrayOf(listId.toString()),
            null, null, null
        ).use { cursor ->
            require(cursor.moveToFirst()) { "List not found" }
            ListEditRow(
                name = cursor.getString(0),
                comment = cursor.getStringOrNull(1),
                isFolder = cursor.getInt(2) == 1
            )
        }
    }

    suspend fun updateList(
        listId: Long,
        name: String,
        comment: String?
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("name", name)
            if (comment == null) putNull("comment") else put("comment", comment)
        }
        databaseHelper.writableDatabase.update(
            "lists", values, "id = ?", arrayOf(listId.toString())
        )
    }

    suspend fun deleteList(listId: Long) = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        // If folder, move children to root before deleting
        db.execSQL(
            "UPDATE lists SET parent_id = NULL WHERE parent_id = ?",
            arrayOf(listId.toString())
        )
        // CASCADE deletes entries
        db.delete("lists", "id = ?", arrayOf(listId.toString()))
    }

    suspend fun deleteListEntry(entryId: Long) = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.delete("entries", "id = ?", arrayOf(entryId.toString()))
    }

    fun listEntryIds(listId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        databaseHelper.readableDatabase.query(
            "entries", arrayOf("id"), "list_id = ?",
            arrayOf(listId.toString()), null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) ids += cursor.getLong(0)
        }
        return ids
    }

    suspend fun loadListDetail(listId: Long): ListDetailData = withContext(Dispatchers.IO) {
        val listRow = databaseHelper.readableDatabase.query(
            "lists",
            arrayOf("name", "comment"),
            "id = ?",
            arrayOf(listId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            require(cursor.moveToFirst()) { "List not found" }
            cursor.getString(0) to cursor.getStringOrNull(1)
        }

        val allEntries = mutableListOf<ListEntrySummary>()
        databaseHelper.readableDatabase.query(
            "entries",
            arrayOf("id", "list_id", "dict_id", "name", "unit", "quantity", "is_checked", "sort_order"),
            "list_id = ?",
            arrayOf(listId.toString()),
            null,
            null,
            "is_checked ASC, sort_order ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                allEntries += cursor.toListEntrySummary()
            }
        }

        val photoCounts = loadEntryPhotoCountsBulk(allEntries.map { it.id })
        val dictNames = loadDictionaryLowerNames()
        val enriched = allEntries.map { entry ->
            entry.copy(
                photoCount = photoCounts[entry.id] ?: 0,
                isInDictionary = entry.name.trim().lowercase() in dictNames
            )
        }

        ListDetailData(
            listId = listId,
            name = listRow.first,
            comment = listRow.second,
            uncheckedEntries = enriched.filter { !it.isChecked },
            checkedEntries = enriched.filter { it.isChecked }
        )
    }

    private fun loadDictionaryLowerNames(): Set<String> {
        val set = HashSet<String>()
        databaseHelper.readableDatabase.query(
            "dictionary", arrayOf("name"), null, null, null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                set += cursor.getString(0).trim().lowercase()
            }
        }
        return set
    }

    suspend fun searchDictionary(query: String, limit: Int = 20): List<DictionaryItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val pattern = "%${trimmed.replace("%", "\\%").replace("_", "\\_")}%"
        val result = mutableListOf<DictionaryItem>()
        databaseHelper.readableDatabase.rawQuery(
            "SELECT id, name, unit FROM dictionary WHERE name LIKE ? ESCAPE '\\' ORDER BY name COLLATE NOCASE ASC LIMIT ?",
            arrayOf(pattern, limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += DictionaryItem(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    unit = cursor.getStringOrNull(2)
                )
            }
        }
        result
    }

    suspend fun findDictionaryByName(name: String): DictionaryItem? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        databaseHelper.readableDatabase.rawQuery(
            "SELECT id, name, unit FROM dictionary WHERE LOWER(name) = LOWER(?) LIMIT 1",
            arrayOf(trimmed)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                DictionaryItem(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    unit = cursor.getStringOrNull(2)
                )
            } else null
        }
    }

    suspend fun loadDictionaryAll(query: String = ""): List<DictionaryItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DictionaryItem>()
        val trimmed = query.trim()
        val (selection, args) = if (trimmed.isEmpty()) {
            null to null
        } else {
            val pattern = "%${trimmed.replace("%", "\\%").replace("_", "\\_")}%"
            "name LIKE ? ESCAPE '\\'" to arrayOf(pattern)
        }
        databaseHelper.readableDatabase.query(
            "dictionary",
            arrayOf("id", "name", "unit"),
            selection, args, null, null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += DictionaryItem(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    unit = cursor.getStringOrNull(2)
                )
            }
        }
        result
    }

    suspend fun updateDictionary(id: Long, name: String, unit: String?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("name", name)
            if (unit.isNullOrBlank()) putNull("unit") else put("unit", unit)
        }
        databaseHelper.writableDatabase.update(
            "dictionary", values, "id = ?", arrayOf(id.toString())
        )
    }

    suspend fun deleteDictionary(id: Long) = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.delete(
            "dictionary", "id = ?", arrayOf(id.toString())
        )
    }

    suspend fun insertDictionary(name: String, unit: String?): Long = withContext(Dispatchers.IO) {
        val sortOrder = databaseHelper.readableDatabase.compileStatement(
            "SELECT COALESCE(MAX(sort_order), 0) FROM dictionary"
        ).simpleQueryForLong().toInt() + 1
        val values = ContentValues().apply {
            put("name", name)
            if (unit.isNullOrBlank()) putNull("unit") else put("unit", unit)
            put("sort_order", sortOrder)
        }
        databaseHelper.writableDatabase.insertOrThrow("dictionary", null, values)
    }

    suspend fun reorderEntries(listId: Long, orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            orderedIds.forEachIndexed { index, entryId ->
                val values = ContentValues().apply { put("sort_order", index + 1) }
                db.update(
                    "entries", values,
                    "id = ? AND list_id = ?",
                    arrayOf(entryId.toString(), listId.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun insertListEntry(
        listId: Long,
        name: String,
        quantity: String?,
        unit: String?
    ): Long = withContext(Dispatchers.IO) {
        val sortOrder = databaseHelper.readableDatabase.compileStatement(
            "SELECT COALESCE(MAX(sort_order), 0) FROM entries WHERE list_id = ?"
        ).apply {
            bindLong(1, listId)
        }.simpleQueryForLong().toInt() + 1

        val values = ContentValues().apply {
            put("list_id", listId)
            putNull("dict_id")
            put("name", name)
            put("unit", unit)
            put("quantity", quantity)
            put("is_checked", 0)
            put("sort_order", sortOrder)
        }
        databaseHelper.writableDatabase.insertOrThrow("entries", null, values)
    }

    suspend fun loadListEntry(entryId: Long): ListEntrySummary = withContext(Dispatchers.IO) {
        databaseHelper.readableDatabase.query(
            "entries",
            arrayOf("id", "list_id", "dict_id", "name", "unit", "quantity", "is_checked", "sort_order"),
            "id = ?",
            arrayOf(entryId.toString()),
            null, null, null
        ).use { cursor ->
            require(cursor.moveToFirst()) { "Entry not found" }
            cursor.toListEntrySummary()
        }
    }

    suspend fun updateListEntry(
        entryId: Long,
        name: String,
        quantity: String?,
        unit: String?
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("name", name)
            if (quantity == null) putNull("quantity") else put("quantity", quantity)
            if (unit == null) putNull("unit") else put("unit", unit)
        }
        databaseHelper.writableDatabase.update(
            "entries", values, "id = ?", arrayOf(entryId.toString())
        )
    }

    suspend fun updateEntryChecked(entryId: Long, isChecked: Boolean) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("is_checked", if (isChecked) 1 else 0)
        }
        databaseHelper.writableDatabase.update(
            "entries",
            values,
            "id = ?",
            arrayOf(entryId.toString())
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
        newestFirst: Boolean,
        selectedTags: Set<String> = emptySet(),
        userFilter: x.x.memlists.feature.memos.UserFilter = x.x.memlists.feature.memos.UserFilter()
    ): List<MemoItemSummary> {
        val (selection, selectionArgs) = buildMemoSelection(folder, hiddenMode, userFilter)
        val requiredTags = mergedFilterTags(selectedTags, userFilter)
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
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val row = cursor.toMemoItemSummary(photoCount = 0)
                if (requiredTags.isEmpty() || itemMatchesTags(row.tags, requiredTags)) {
                    rows += row
                }
            }
        }

        val photoCounts = loadPhotoCountsBulk(rows.map { it.id })
        val withCounts = rows.map { it.copy(photoCount = photoCounts[it.id] ?: 0) }

        return withCounts.sortedWith(memoComparator(folder = folder, newestFirst = newestFirst))
    }

    private fun buildMemoSelection(
        folder: MemoFolderType?,
        hiddenMode: Boolean,
        userFilter: x.x.memlists.feature.memos.UserFilter
    ): Pair<String, Array<String>> {
        val parts = mutableListOf<String>()
        val args = mutableListOf<String>()

        parts += "hidden = ?"
        args += if (hiddenMode) "1" else "0"

        when (folder) {
            MemoFolderType.Notes -> parts += "reminder_type = 0"
            MemoFolderType.Daily -> parts += "reminder_type = 2"
            MemoFolderType.Periods -> parts += "reminder_type = 3"
            MemoFolderType.Monthly -> parts += "reminder_type = 1 AND monthly = 1"
            MemoFolderType.Yearly -> parts += "reminder_type = 1 AND yearly = 1"
            null -> parts += "reminder_type = 1 AND monthly = 0 AND yearly = 0"
        }

        userFilter.dateFrom?.let {
            parts += "date IS NOT NULL AND date >= ?"
            args += it.toString()
        }
        userFilter.dateTo?.let {
            parts += "date IS NOT NULL AND date <= ?"
            args += it.toString()
        }
        if (userFilter.priorityMin > 0) {
            parts += "priority >= ?"
            args += userFilter.priorityMin.toString()
        }
        when (userFilter.hasReminder) {
            x.x.memlists.feature.memos.HasReminder.Yes -> parts += "reminder_type != 0"
            x.x.memlists.feature.memos.HasReminder.No -> parts += "reminder_type = 0"
            x.x.memlists.feature.memos.HasReminder.Any -> {}
        }

        return parts.joinToString(" AND ") to args.toTypedArray()
    }

    private fun mergedFilterTags(
        selectedTags: Set<String>,
        userFilter: x.x.memlists.feature.memos.UserFilter
    ): Set<String> {
        val fromUser = userFilter.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        return selectedTags + fromUser
    }

    private fun itemMatchesTags(tagsColumn: String?, required: Set<String>): Boolean {
        val itemTags = tagsColumn
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        return required.all { it in itemTags }
    }

    private fun loadMemoFolders(
        hiddenMode: Boolean,
        selectedTags: Set<String> = emptySet(),
        userFilter: x.x.memlists.feature.memos.UserFilter = x.x.memlists.feature.memos.UserFilter()
    ): List<MemoFolderSummary> {
        val requiredTags = mergedFilterTags(selectedTags, userFilter)
        val types = listOf(
            MemoFolderType.Notes,
            MemoFolderType.Daily,
            MemoFolderType.Periods,
            MemoFolderType.Monthly,
            MemoFolderType.Yearly
        )

        return types.mapNotNull { type ->
            val (selection, args) = buildMemoSelection(type, hiddenMode, userFilter)
            val count = if (requiredTags.isEmpty()) {
                databaseHelper.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM items WHERE $selection",
                    args
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
            } else {
                var matched = 0
                databaseHelper.readableDatabase.rawQuery(
                    "SELECT tags FROM items WHERE $selection",
                    args
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        if (itemMatchesTags(cursor.getString(0), requiredTags)) matched++
                    }
                }
                matched
            }

            if (count > 0) MemoFolderSummary(type = type, count = count) else null
        }
    }

    private fun loadPhotoCountsBulk(memoIds: List<Long>): Map<Long, Int> {
        if (memoIds.isEmpty()) return emptyMap()
        val placeholders = memoIds.joinToString(",") { "?" }
        val sql = "SELECT owner_id, COUNT(*) FROM photos " +
            "WHERE owner_type = 'memo' AND owner_id IN ($placeholders) GROUP BY owner_id"
        val args = memoIds.map { it.toString() }.toTypedArray()
        val result = mutableMapOf<Long, Int>()
        databaseHelper.readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getLong(0)] = cursor.getInt(1)
            }
        }
        return result
    }

    private fun loadEntryPhotoCountsBulk(entryIds: List<Long>): Map<Long, Int> {
        if (entryIds.isEmpty()) return emptyMap()
        val placeholders = entryIds.joinToString(",") { "?" }
        val sql = "SELECT owner_id, COUNT(*) FROM photos " +
            "WHERE owner_type = 'entry' AND owner_id IN ($placeholders) GROUP BY owner_id"
        val args = entryIds.map { it.toString() }.toTypedArray()
        val result = mutableMapOf<Long, Int>()
        databaseHelper.readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getLong(0)] = cursor.getInt(1)
            }
        }
        return result
    }

    private fun loadListEntryCountsBulk(listIds: List<Long>): Map<Long, Pair<Int, Int>> {
        if (listIds.isEmpty()) return emptyMap()
        val placeholders = listIds.joinToString(",") { "?" }
        val sql = "SELECT list_id, " +
            "SUM(CASE WHEN is_checked = 0 THEN 1 ELSE 0 END), " +
            "COUNT(*) " +
            "FROM entries WHERE list_id IN ($placeholders) GROUP BY list_id"
        val args = listIds.map { it.toString() }.toTypedArray()
        val result = mutableMapOf<Long, Pair<Int, Int>>()
        databaseHelper.readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val unchecked = cursor.getInt(1)
                val total = cursor.getInt(2)
                result[id] = unchecked to total
            }
        }
        return result
    }

    private data class RawListRow(
        val id: Long,
        val name: String,
        val comment: String?,
        val parentId: Long?,
        val isFolder: Boolean,
        val isLocked: Boolean
    )

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

    private fun Cursor.toListEntrySummary(): ListEntrySummary {
        val dictId = getLongOrNull(2)
        val manualName = getStringOrNull(3).orEmpty()
        val unit = getStringOrNull(4)
        return ListEntrySummary(
            id = getLong(0),
            listId = getLong(1),
            name = if (dictId == null) manualName else manualName,
            quantity = getStringOrNull(5),
            unit = unit,
            isChecked = getInt(6) == 1,
            sortOrder = getInt(7),
            photoCount = 0
        )
    }

    private fun memoComparator(
        folder: MemoFolderType?,
        newestFirst: Boolean
    ): Comparator<MemoItemSummary> {
        // id-based tiebreaker: `created` stores only YYYYMMDD (no time), so items
        // created on the same day tie. Auto-increment id is monotonic — use it as
        // a stable secondary sort that matches "newest first" intent.
        val byIdNewestFirst = Comparator<MemoItemSummary> { l, r ->
            if (newestFirst) compareValues(r.id, l.id) else compareValues(l.id, r.id)
        }
        return when (folder) {
            MemoFolderType.Yearly,
            MemoFolderType.Monthly,
            MemoFolderType.Periods -> Comparator { left, right ->
                compareDate(left.date, right.date, newestFirst).takeIf { it != 0 }
                    ?: compareValues(left.time ?: Int.MAX_VALUE, right.time ?: Int.MAX_VALUE).takeIf { it != 0 }
                    ?: byIdNewestFirst.compare(left, right)
            }
            MemoFolderType.Daily -> compareBy<MemoItemSummary>(
                { firstDailyTime(it.timesJson) ?: "99:99" },
                { it.title.lowercase() }
            ).then(byIdNewestFirst)
            MemoFolderType.Notes -> {
                Comparator { left, right ->
                    compareDate(left.created, right.created, newestFirst).takeIf { it != 0 }
                        ?: byIdNewestFirst.compare(left, right)
                }
            }
            null -> {
                val today = todayAsInt()
                Comparator { left, right ->
                    compareValues(mainSortBucket(left, today), mainSortBucket(right, today))
                        .takeIf { it != 0 }
                        ?: compareValues(right.priority, left.priority).takeIf { it != 0 }
                        ?: compareDate(left.date, right.date, newestFirst).takeIf { it != 0 }
                        ?: compareDate(left.created, right.created, newestFirst).takeIf { it != 0 }
                        ?: byIdNewestFirst.compare(left, right)
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

    private fun todayReminderComparator(): Comparator<MemoItemSummary> {
        return compareBy<MemoItemSummary>(
            { todayReminderSortTime(it) },
            { it.title.lowercase() }
        ).thenBy { it.id }
    }

    private fun todayReminderSortTime(item: MemoItemSummary): String {
        return when (item.reminderType) {
            2 -> firstDailyTime(item.timesJson) ?: "99:99"
            else -> formatTime(item.time) ?: "99:99"
        }
    }

    private fun isReminderDueToday(
        reminderType: Int,
        date: Int?,
        time: Int?,
        timesJson: String?,
        dateTo: Int?,
        daysMask: Int?,
        periodDoneUntil: Int?
    ): Boolean {
        val today = todayAsInt()
        return when (reminderType) {
            1 -> date == today
            2 -> {
                timesJson != null &&
                    !firstDailyTime(timesJson).isNullOrBlank() &&
                    isTodayInDaysMask(daysMask ?: 127)
            }
            3 -> {
                if (periodDoneUntil != null && today < periodDoneUntil) return false
                if (!isTodayInDaysMask(daysMask ?: 127)) return false
                val dateFrom = date ?: return false
                val dateUntil = dateTo ?: return false
                if (dateFrom in 1..31 && dateUntil in 1..31) {
                    val day = today % 100
                    if (dateFrom <= dateUntil) {
                        day in dateFrom..dateUntil
                    } else {
                        day >= dateFrom || day <= dateUntil
                    }
                } else {
                    today in dateFrom..dateUntil
                }
            }
            else -> false
        }
    }

    private fun isTodayInDaysMask(daysMask: Int): Boolean {
        val today = java.time.LocalDate.now()
        val bitIndex = today.dayOfWeek.value - 1 // Mon=1 .. Sun=7
        return daysMask and (1 shl bitIndex) != 0
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
        private const val KEY_LARGE_FONT_WAKELOCK = "large_font_wakelock"
        private const val KEY_TIME_MORNING = "time_morning"
        private const val KEY_TIME_DAY = "time_day"
        private const val KEY_TIME_EVENING = "time_evening"
    }

    // --- Reminder queries (used by ReminderReceiver/Scheduler) ---

    data class ReminderItem(
        val id: Long,
        val title: String,
        val content: String,
        val sound: String?,
        val fullscreen: Int,
        val loopSound: Int,
        val active: Int,
        val reminderType: Int,
        val date: Int?,
        val time: Int?,
        val times: String?,
        val dateTo: Int?,
        val daysMask: Int?,
        val yearly: Int,
        val monthly: Int,
        val remove: Int,
        val periodDoneUntil: Int?
    )

    fun getItemByIdSync(id: Long): ReminderItem? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT id, title, content, sound, fullscreen, loop_sound, active,
               reminder_type, date, time, times, date_to, days_mask,
               yearly, monthly, remove, period_done_until
               FROM items WHERE id = ?""",
            arrayOf(id.toString())
        )
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            ReminderItem(
                id = it.getLong(0),
                title = it.getString(1) ?: "",
                content = it.getString(2) ?: "",
                sound = if (it.isNull(3)) null else it.getString(3),
                fullscreen = it.getInt(4),
                loopSound = if (it.isNull(5)) 1 else it.getInt(5),
                active = it.getInt(6),
                reminderType = it.getInt(7),
                date = if (it.isNull(8)) null else it.getInt(8),
                time = if (it.isNull(9)) null else it.getInt(9),
                times = if (it.isNull(10)) null else it.getString(10),
                dateTo = if (it.isNull(11)) null else it.getInt(11),
                daysMask = if (it.isNull(12)) null else it.getInt(12),
                yearly = it.getInt(13),
                monthly = it.getInt(14),
                remove = it.getInt(15),
                periodDoneUntil = if (it.isNull(16)) null else it.getInt(16)
            )
        }
    }

    fun getActiveOneTimeRemindersSync(): List<ReminderItem> {
        return queryReminders("reminder_type = 1 AND active = 1")
    }

    fun getAutoRemoveOneTimeRemindersSync(): List<ReminderItem> {
        return queryReminders("reminder_type = 1 AND remove = 1 AND yearly = 0 AND monthly = 0")
    }

    fun getActiveDailyRemindersSync(): List<ReminderItem> {
        return queryReminders("reminder_type = 2 AND active = 1")
    }

    fun getActivePeriodRemindersSync(): List<ReminderItem> {
        return queryReminders("reminder_type = 3 AND active = 1")
    }

    private fun queryReminders(where: String): List<ReminderItem> {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery(
            """SELECT id, title, content, sound, fullscreen, loop_sound, active,
               reminder_type, date, time, times, date_to, days_mask,
               yearly, monthly, remove, period_done_until
               FROM items WHERE $where""",
            null
        )
        val result = mutableListOf<ReminderItem>()
        cursor.use {
            while (it.moveToNext()) {
                result += ReminderItem(
                    id = it.getLong(0),
                    title = it.getString(1) ?: "",
                    content = it.getString(2) ?: "",
                    sound = if (it.isNull(3)) null else it.getString(3),
                    fullscreen = it.getInt(4),
                    loopSound = if (it.isNull(5)) 1 else it.getInt(5),
                    active = it.getInt(6),
                    reminderType = it.getInt(7),
                    date = if (it.isNull(8)) null else it.getInt(8),
                    time = if (it.isNull(9)) null else it.getInt(9),
                    times = if (it.isNull(10)) null else it.getString(10),
                    dateTo = if (it.isNull(11)) null else it.getInt(11),
                    daysMask = if (it.isNull(12)) null else it.getInt(12),
                    yearly = it.getInt(13),
                    monthly = it.getInt(14),
                    remove = it.getInt(15),
                    periodDoneUntil = if (it.isNull(16)) null else it.getInt(16)
                )
            }
        }
        return result
    }

    fun updateItemDateSync(id: Long, newDate: Int) {
        val values = ContentValues().apply { put("date", newDate) }
        databaseHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun deactivateItemSync(id: Long) {
        val values = ContentValues().apply { put("active", 0) }
        databaseHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteItemSync(id: Long) {
        databaseHelper.writableDatabase.delete("items", "id = ?", arrayOf(id.toString()))
    }

    fun updatePeriodDoneUntilSync(id: Long, doneUntil: Int?) {
        val values = ContentValues().apply {
            if (doneUntil == null) putNull("period_done_until")
            else put("period_done_until", doneUntil)
        }
        databaseHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun getDefaultSoundSync(): String? {
        return getSettingSync(KEY_DEFAULT_SOUND)
    }

    fun getSoundRepeatsSync(): Int {
        return getSettingSync(KEY_SOUND_REPEATS)?.toIntOrNull() ?: 10
    }

    fun isRemindersEnabledSync(): Boolean {
        return getSettingSync(KEY_ENABLE_REMINDERS) != "false"
    }

    fun getLanguageSync(): String {
        return getSettingSync(KEY_LANGUAGE) ?: "en"
    }

    private fun getSettingSync(key: String): String? {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT value FROM settings WHERE key = ?",
            arrayOf(key)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
}
