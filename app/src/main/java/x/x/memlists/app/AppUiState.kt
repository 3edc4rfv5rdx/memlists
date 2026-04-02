package x.x.memlists.app

import x.x.memlists.core.data.SettingsData
import x.x.memlists.core.i18n.LanguageOption
import x.x.memlists.core.theme.AppThemePalette

data class AppUiState(
    val isLoading: Boolean = true,
    val settings: SettingsData = SettingsData(),
    val languages: List<LanguageOption> = emptyList(),
    val themes: List<AppThemePalette> = emptyList()
)

