package x.x.memlists.feature.settings

import androidx.compose.runtime.Composable
import x.x.memlists.core.data.SettingsData
import x.x.memlists.core.i18n.LanguageOption
import x.x.memlists.core.theme.AppThemePalette
import x.x.memlists.core.ui.OptionGroup
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.SettingSwitchCard

@Composable
fun SettingsScreen(
    settings: SettingsData,
    languages: List<LanguageOption>,
    themes: List<AppThemePalette>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onThemeChanged: (String) -> Unit,
    onNewestFirstChanged: (Boolean) -> Unit,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    onAutoSortDictionaryChanged: (Boolean) -> Unit,
    onLargeFontWakeLockChanged: (Boolean) -> Unit
) {
    ScreenScaffold(
        title = lw("Settings"),
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            SectionTitle(title = lw("General"))
            OptionGroup(
                title = lw("Language"),
                options = languages.map { it.code },
                selectedOption = settings.languageCode,
                labelForOption = { code ->
                    val option = languages.first { it.code == code }
                    lw(option.labelKey)
                },
                onOptionSelected = onLanguageChanged
            )
            OptionGroup(
                title = lw("Theme"),
                options = themes.map { it.name },
                selectedOption = settings.themeName,
                labelForOption = { lw(it) },
                onOptionSelected = onThemeChanged
            )
            SettingSwitchCard(
                title = lw("Newest first"),
                body = lw("Sort preference"),
                checked = settings.newestFirst,
                onCheckedChange = onNewestFirstChanged
            )
            SettingSwitchCard(
                title = lw("Enable reminders"),
                body = lw("Reminders master switch"),
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersEnabledChanged
            )
            SettingSwitchCard(
                title = lw("Auto-sort dictionary"),
                body = lw("Items dictionary"),
                checked = settings.autoSortDictionary,
                onCheckedChange = onAutoSortDictionaryChanged
            )
            SettingSwitchCard(
                title = lw("Keep screen on"),
                body = lw("Theme applied immediately"),
                checked = settings.largeFontWakeLock,
                onCheckedChange = onLargeFontWakeLockChanged
            )
        }
    }
}

