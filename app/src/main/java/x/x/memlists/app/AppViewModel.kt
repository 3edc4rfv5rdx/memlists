package x.x.memlists.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import x.x.memlists.core.data.SettingsData
import x.x.memlists.core.reminder.ReminderMaintenance
import x.x.memlists.core.reminder.ReminderScheduler
import x.x.memlists.core.reminder.ReminderSoundService
import x.x.memlists.core.theme.AppThemePalette

class AppViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val memListsApplication = application as x.x.memlists.MemListsApplication
    private val repository = memListsApplication.repository
    val localizer = memListsApplication.localizer
    private val themeRepository = memListsApplication.themeRepository

    private val _uiState = MutableStateFlow(
        AppUiState(
            isLoading = true,
            languages = localizer.languageOptions,
            themes = themeRepository.themes
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.loadSettings()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    settings = settings
                )
            }
            // Run maintenance after DB is ready (no race)
            launch(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    ReminderMaintenance.runAll(context, repository)
                } catch (e: Exception) {
                    android.util.Log.e("MemLists", "Maintenance failed: ${e.message}", e)
                }
            }
        }
    }

    fun resolveTheme(themeName: String): AppThemePalette {
        return themeRepository.resolveTheme(themeName)
    }

    fun previewWelcomeLanguage(languageCode: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(languageCode = languageCode)
            )
        }
    }

    fun previewWelcomeTheme(themeName: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(themeName = themeName)
            )
        }
    }

    fun saveWelcomeSelection(languageCode: String, themeName: String) {
        val updated = uiState.value.settings.copy(
            languageCode = languageCode,
            themeName = themeName,
            isFirstLaunch = false
        )
        persistSettings(updated)
    }

    fun updateLanguage(languageCode: String) {
        persistSettings(uiState.value.settings.copy(languageCode = languageCode, isFirstLaunch = false))
    }

    fun updateTheme(themeName: String) {
        persistSettings(uiState.value.settings.copy(themeName = themeName, isFirstLaunch = false))
    }

    fun updateNewestFirst(enabled: Boolean) {
        persistSettings(uiState.value.settings.copy(newestFirst = enabled, isFirstLaunch = false))
    }

    fun updateRemindersEnabled(enabled: Boolean) {
        val updated = uiState.value.settings.copy(remindersEnabled = enabled, isFirstLaunch = false)
        _uiState.update { it.copy(settings = updated, isLoading = false) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(updated)
            val context = getApplication<Application>()
            if (enabled) {
                ReminderScheduler.rescheduleAll(context, repository)
            } else {
                ReminderScheduler.cancelAll(context, repository)
                ReminderSoundService.stop(context)
            }
        }
    }

    fun updateAutoSortDictionary(enabled: Boolean) {
        persistSettings(uiState.value.settings.copy(autoSortDictionary = enabled, isFirstLaunch = false))
    }

    fun updateLargeFontWakeLock(enabled: Boolean) {
        persistSettings(uiState.value.settings.copy(largeFontWakeLock = enabled, isFirstLaunch = false))
    }

    fun updateTimeMorning(time: String) {
        persistSettings(uiState.value.settings.copy(timeMorning = time, isFirstLaunch = false))
    }

    fun updateTimeDay(time: String) {
        persistSettings(uiState.value.settings.copy(timeDay = time, isFirstLaunch = false))
    }

    fun updateTimeEvening(time: String) {
        persistSettings(uiState.value.settings.copy(timeEvening = time, isFirstLaunch = false))
    }

    fun updateDefaultSound(sound: String?) {
        persistSettings(uiState.value.settings.copy(defaultSound = sound, isFirstLaunch = false))
    }

    fun updateSoundRepeats(value: Int) {
        persistSettings(
            uiState.value.settings.copy(
                soundRepeats = value.coerceIn(1, 25),
                isFirstLaunch = false
            )
        )
    }

    private fun persistSettings(settings: SettingsData) {
        _uiState.update { it.copy(settings = settings, isLoading = false) }
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }
}
