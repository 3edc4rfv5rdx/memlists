package x.x.memlists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import x.x.memlists.app.AppViewModel
import x.x.memlists.core.theme.MemListsTheme
import x.x.memlists.core.ui.LoadingScreen
import x.x.memlists.feature.lists.ListsHomeScreen
import x.x.memlists.feature.memos.MemosHomeScreen
import x.x.memlists.feature.memos.MemosViewModel
import x.x.memlists.feature.settings.SettingsScreen
import x.x.memlists.feature.welcome.WelcomeScreen

private object Routes {
    const val Welcome = "welcome"
    const val Memos = "memos"
    const val Lists = "lists"
    const val Settings = "settings"
}

@Composable
fun MemListsApp() {
    val viewModel: AppViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading || uiState.themes.isEmpty()) {
        LoadingScreen()
        return
    }

    val palette = viewModel.resolveTheme(uiState.settings.themeName)
    val lw: (String) -> String = { key ->
        viewModel.localizer.lw(key, uiState.settings.languageCode)
    }

    MemListsTheme(palette = palette) {
        val navController = rememberNavController()
        val startDestination = if (uiState.settings.isFirstLaunch) Routes.Welcome else Routes.Memos

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.Welcome) {
                WelcomeRoute(
                    navController = navController,
                    viewModel = viewModel,
                    lw = lw
                )
            }
            composable(Routes.Memos) {
                val memosViewModel: MemosViewModel = viewModel()
                val memosUiState by memosViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.settings.newestFirst) {
                    memosViewModel.refresh(newestFirst = uiState.settings.newestFirst)
                }

                MemosHomeScreen(
                    selectedFolder = memosUiState.selectedFolder,
                    isLoading = memosUiState.isLoading,
                    items = memosUiState.items,
                    folders = memosUiState.folders,
                    lw = lw,
                    onOpenLists = { navController.navigate(Routes.Lists) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenFolder = { folder ->
                        memosViewModel.openFolder(folder, newestFirst = uiState.settings.newestFirst)
                    },
                    onBackFromFolder = {
                        memosViewModel.leaveFolder(newestFirst = uiState.settings.newestFirst)
                    }
                )
            }
            composable(Routes.Lists) {
                ListsHomeScreen(
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    settings = uiState.settings,
                    languages = uiState.languages,
                    themes = uiState.themes,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onLanguageChanged = viewModel::updateLanguage,
                    onThemeChanged = viewModel::updateTheme,
                    onNewestFirstChanged = viewModel::updateNewestFirst,
                    onRemindersEnabledChanged = viewModel::updateRemindersEnabled,
                    onAutoSortDictionaryChanged = viewModel::updateAutoSortDictionary,
                    onLargeFontWakeLockChanged = viewModel::updateLargeFontWakeLock
                )
            }
        }
    }
}

@Composable
private fun WelcomeRoute(
    navController: NavHostController,
    viewModel: AppViewModel,
    lw: (String) -> String
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.settings.isFirstLaunch) {
        if (!uiState.settings.isFirstLaunch) {
            navController.navigate(Routes.Memos) {
                popUpTo(Routes.Welcome) {
                    inclusive = true
                }
            }
        }
    }

    WelcomeScreen(
        languages = uiState.languages,
        themes = uiState.themes,
        initialLanguageCode = uiState.settings.languageCode,
        initialThemeName = uiState.settings.themeName,
        lw = lw,
        onStart = viewModel::saveWelcomeSelection
    )
}
