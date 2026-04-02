package x.x.memlists.core.data

data class SettingsData(
    val languageCode: String = "en",
    val themeName: String = "Light",
    val newestFirst: Boolean = true,
    val remindersEnabled: Boolean = true,
    val debugLogs: Boolean = false,
    val defaultSound: String? = null,
    val soundRepeats: Int = 25,
    val hiddenPin: String? = null,
    val autoSortDictionary: Boolean = true,
    val largeFontWakeLock: Boolean = true,
    val isFirstLaunch: Boolean = true
)

