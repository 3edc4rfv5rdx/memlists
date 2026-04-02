package x.x.memlists.feature.welcome

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    initialLanguageCode: String,
    initialThemeName: String,
    lw: (String) -> String,
    onStart: (languageCode: String, themeName: String) -> Unit
) {
    var languageCode by remember(initialLanguageCode) { mutableStateOf(initialLanguageCode) }
    var themeName by remember(initialThemeName) { mutableStateOf(initialThemeName) }

    ScreenScaffold(
        title = lw("Welcome"),
        canNavigateBack = false,
        onNavigateBack = {}
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            HeroCard(
                title = lw("Welcome to MemLists"),
                body = lw("Combined memos and universal lists"),
                icon = Icons.Default.AutoAwesome
            )
            OptionGroup(
                title = lw("Choose language"),
                options = languages.map { it.code },
                selectedOption = languageCode,
                labelForOption = { code ->
                    val option = languages.first { it.code == code }
                    lw(option.labelKey)
                },
                onOptionSelected = { languageCode = it }
            )
            OptionGroup(
                title = lw("Choose theme"),
                options = themes.map { it.name },
                selectedOption = themeName,
                labelForOption = { lw(it) },
                onOptionSelected = { themeName = it }
            )
            PrimaryActionButton(
                text = lw("Start"),
                onClick = { onStart(languageCode, themeName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}
