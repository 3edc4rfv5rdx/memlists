package x.x.memlists.core.data

import android.content.ContentValues
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

