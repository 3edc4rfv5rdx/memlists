package x.x.memlists.core.data

data class SettingsData(
    val languageCode: String = "en",
    val themeName: String = "Light",
    val newestFirst: Boolean = true,
    val remindersEnabled: Boolean = true,
    val debugLogs: Boolean = false,
    val defaultSound: String? = null,
    val soundRepeats: Int = 10,
    val hiddenPin: String? = null,
    val largeFontWakeLock: Boolean = true,
    val isFirstLaunch: Boolean = true,
    val timeMorning: String = "09:30",
    val timeDay: String = "12:30",
    val timeEvening: String = "18:30"
)

