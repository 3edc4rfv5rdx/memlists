package x.x.memlists.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.data.SettingsData
import x.x.memlists.core.theme.AppThemePalette

class AppViewModel(
    application: MemListsApplication
) : AndroidViewModel(application) {
    private val repository = application.repository
    val localizer = application.localizer
    private val themeRepository = application.themeRepository

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
        }
    }

    fun resolveTheme(themeName: String): AppThemePalette {
        return themeRepository.resolveTheme(themeName)
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
        persistSettings(uiState.value.settings.copy(remindersEnabled = enabled, isFirstLaunch = false))
    }

    fun updateAutoSortDictionary(enabled: Boolean) {
        persistSettings(uiState.value.settings.copy(autoSortDictionary = enabled, isFirstLaunch = false))
    }

    fun updateLargeFontWakeLock(enabled: Boolean) {
        persistSettings(uiState.value.settings.copy(largeFontWakeLock = enabled, isFirstLaunch = false))
    }

    private fun persistSettings(settings: SettingsData) {
        _uiState.update { it.copy(settings = settings, isLoading = false) }
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }
}

