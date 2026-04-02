package x.x.memlists.feature.welcome

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import x.x.memlists.core.i18n.LanguageOption
import x.x.memlists.core.theme.AppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.OptionGroup
import x.x.memlists.core.ui.PrimaryActionButton
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen

@Composable
fun WelcomeScreen(
    languages: List<LanguageOption>,
    themes: List<AppThemePalette>,
    selectedLanguageCode: String,
    selectedThemeName: String,
    lw: (String) -> String,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (String) -> Unit,
    onStart: (languageCode: String, themeName: String) -> Unit
) {
    ScreenScaffold(
        title = lw("Welcome"),
        navigationButtonMode = x.x.memlists.core.ui.NavigationButtonMode.None,
        onNavigateBack = {}
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            HeroCard(
                title = lw("Welcome to MemLists"),
                icon = Icons.Default.AutoAwesome
            )
            OptionGroup(
                title = lw("Choose language"),
                options = languages.map { it.code },
                selectedOption = selectedLanguageCode,
                labelForOption = { code ->
                    val option = languages.first { it.code == code }
                    lw(option.labelKey)
                },
                onOptionSelected = onLanguageSelected
            )
            OptionGroup(
                title = lw("Choose theme"),
                options = themes.map { it.name },
                selectedOption = selectedThemeName,
                labelForOption = { lw(it) },
                onOptionSelected = onThemeSelected
            )
            PrimaryActionButton(
                text = lw("Start"),
                onClick = { onStart(selectedLanguageCode, selectedThemeName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}
