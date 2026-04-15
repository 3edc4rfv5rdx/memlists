package x.x.memlists

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import x.x.memlists.app.AppViewModel
import x.x.memlists.core.reminder.ReminderScheduler
import x.x.memlists.core.theme.MemListsTheme
import x.x.memlists.core.ui.LoadingScreen
import x.x.memlists.feature.lists.ListsHomeScreen
import x.x.memlists.feature.lists.ListsViewModel
import x.x.memlists.feature.lists.ListDetailScreen
import x.x.memlists.feature.lists.ListDetailViewModel
import x.x.memlists.feature.lists.ListEditorScreen
import x.x.memlists.feature.lists.ListEntryEditorScreen
import x.x.memlists.feature.memos.MemosHomeScreen
import x.x.memlists.feature.memos.MemoEditorScreen
import x.x.memlists.feature.memos.MemosViewModel
import x.x.memlists.feature.memos.TagCloudScreen
import x.x.memlists.feature.memos.UserFiltersScreen
import x.x.memlists.feature.settings.SettingsScreen
import x.x.memlists.feature.welcome.WelcomeScreen

private object Routes {
    const val Welcome = "welcome"
    const val Memos = "memos"
    const val Lists = "lists"
    const val Settings = "settings"
    const val MemoNew = "memo_new"
    const val MemoEdit = "memo_edit/{id}"
    const val ListNew = "list_new/{parentId}/{isFolder}"
    const val ListDetail = "list_detail/{listId}"
    const val EntryNew = "entry_new/{listId}"
    const val TagCloud = "tag_cloud"
    const val UserFilters = "user_filters"
}

private fun listNewRoute(parentId: Long?, isFolder: Boolean): String {
    val encodedParent = parentId ?: -1L
    val encodedFolder = if (isFolder) 1 else 0
    return "list_new/$encodedParent/$encodedFolder"
}

private fun listDetailRoute(listId: Long): String = "list_detail/$listId"

private fun memoEditRoute(id: Long): String = "memo_edit/$id"

private const val NAV_TAG = "MemListsNav"

/**
 * Pop only if the expected route is still on top of the back stack. Prevents
 * double-pop white-screen race when Back and a save-completion callback both
 * try to leave the same screen (see TODO.md / project_memo_editor_back_race).
 * Sets the refresh flag on whichever entry is now current so the parent
 * screen still refreshes even if Back already popped us.
 */
private fun popIfOnTop(
    navController: NavHostController,
    expectedRoute: String,
    refreshKey: String
) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    if (currentRoute == expectedRoute) {
        navController.previousBackStackEntry?.savedStateHandle?.set(refreshKey, true)
        navController.popBackStack()
        Log.d(NAV_TAG, "popIfOnTop: popped $expectedRoute")
    } else {
        // Already popped (likely by Back press during save) — just notify
        // whichever entry is now on top so it still refreshes.
        navController.currentBackStackEntry?.savedStateHandle?.set(refreshKey, true)
        Log.d(NAV_TAG, "popIfOnTop: skip pop, current=$currentRoute expected=$expectedRoute")
    }
}

private fun entryNewRoute(listId: Long): String = "entry_new/$listId"

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
        val activity = LocalContext.current as? Activity
        val startDestination = if (uiState.settings.isFirstLaunch) Routes.Welcome else Routes.Memos

        androidx.compose.runtime.DisposableEffect(navController) {
            val listener = androidx.navigation.NavController.OnDestinationChangedListener { ctrl, destination, _ ->
                val stack = ctrl.currentBackStack.value.joinToString("→") { it.destination.route ?: "?" }
                Log.d(NAV_TAG, "destChanged → ${destination.route}; stack=[$stack]")
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose { navController.removeOnDestinationChangedListener(listener) }
        }

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
                val memosApplication = LocalContext.current.applicationContext as MemListsApplication
                val memosScope = rememberCoroutineScope()
                val memosViewModel: MemosViewModel = viewModel()
                val memosUiState by memosViewModel.uiState.collectAsState()
                val todayReminderItems = remember { mutableStateOf(emptyList<x.x.memlists.core.data.MemoItemSummary>()) }
                val showTodayRemindersDialog = remember { mutableStateOf(false) }
                val refreshHandle = navController.currentBackStackEntry?.savedStateHandle
                val shouldRefresh by refreshHandle
                    ?.getStateFlow("memos_refresh", false)
                    ?.collectAsState()
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(uiState.settings.newestFirst) {
                    memosViewModel.refreshIfNeeded(newestFirst = uiState.settings.newestFirst)
                }

                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        memosViewModel.refresh(newestFirst = uiState.settings.newestFirst)
                        refreshHandle?.set("memos_refresh", false)
                    }
                }

                MemosHomeScreen(
                    selectedFolder = memosUiState.selectedFolder,
                    isLoading = memosUiState.isLoading,
                    items = memosUiState.items,
                    folders = memosUiState.folders,
                    todayReminderItems = todayReminderItems.value,
                    showTodayRemindersDialog = showTodayRemindersDialog.value,
                    selectedTags = memosUiState.selectedTags,
                    lw = lw,
                    onOpenLists = { navController.navigate(Routes.Lists) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenTagCloud = {
                        navController.navigate(Routes.TagCloud)
                    },
                    onOpenUserFilters = {
                        navController.navigate(Routes.UserFilters)
                    },
                    userFilterActive = memosUiState.userFilter.isActive,
                    onClearAllFilters = { memosViewModel.clearAllFilters() },
                    onCheckReminders = {
                        memosScope.launch {
                            val items = withContext(Dispatchers.IO) {
                                memosApplication.repository.loadTodayReminderItems()
                            }
                            todayReminderItems.value = items
                            showTodayRemindersDialog.value = true
                        }
                    },
                    onDismissTodayReminders = {
                        showTodayRemindersDialog.value = false
                    },
                    onAddMemo = { navController.navigate(Routes.MemoNew) },
                    onOpenFolder = { folder ->
                        memosViewModel.openFolder(folder, newestFirst = uiState.settings.newestFirst)
                    },
                    onBackFromFolder = {
                        memosViewModel.leaveFolder(newestFirst = uiState.settings.newestFirst)
                    },
                    onCloseRoot = {
                        activity?.finish()
                    },
                    onToggleActive = { item ->
                        memosScope.launch {
                            withContext(Dispatchers.IO) {
                                memosApplication.repository.toggleMemoActive(item.id, item.active)
                                val newActive = !item.active
                                if (newActive) {
                                    ReminderScheduler.scheduleItem(
                                        memosApplication, memosApplication.repository, item.id
                                    )
                                } else {
                                    ReminderScheduler.cancelItem(memosApplication, item.id)
                                }
                            }
                            memosViewModel.refresh(newestFirst = uiState.settings.newestFirst)
                        }
                    },
                    onEditMemo = { item ->
                        navController.navigate(memoEditRoute(item.id)) {
                            launchSingleTop = true
                        }
                    },
                    onDeleteMemo = { item ->
                        memosScope.launch {
                            withContext(Dispatchers.IO) {
                                ReminderScheduler.cancelItem(memosApplication, item.id)
                                memosApplication.repository.deleteMemo(item.id)
                                memosApplication.photoRepository.deleteAllForOwner(
                                    x.x.memlists.core.photo.PhotoOwnerType.Memo,
                                    item.id
                                )
                            }
                            memosViewModel.refresh(newestFirst = uiState.settings.newestFirst)
                        }
                    },
                    photoRepository = memosApplication.photoRepository,
                    onPhotosChanged = {
                        memosViewModel.refresh(newestFirst = uiState.settings.newestFirst)
                    }
                )
            }
            composable(Routes.TagCloud) {
                val tagCloudApp = LocalContext.current.applicationContext as MemListsApplication
                val tagCounts = remember { mutableStateOf(emptyMap<String, Int>()) }
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry(Routes.Memos)
                }
                val parentViewModel: MemosViewModel = viewModel(parentEntry)
                val parentState by parentViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    tagCounts.value = withContext(Dispatchers.IO) {
                        tagCloudApp.repository.loadTagCloud()
                    }
                }

                TagCloudScreen(
                    tagCounts = tagCounts.value,
                    initialSelected = parentState.selectedTags,
                    lw = lw,
                    onApply = { tags ->
                        parentViewModel.setTagFilter(tags)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.UserFilters) {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry(Routes.Memos)
                }
                val parentViewModel: MemosViewModel = viewModel(parentEntry)
                val parentState by parentViewModel.uiState.collectAsState()

                UserFiltersScreen(
                    initial = parentState.userFilter,
                    lw = lw,
                    onApply = { filter ->
                        parentViewModel.setUserFilter(filter)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.MemoNew) {
                val application = LocalContext.current.applicationContext as MemListsApplication
                MemoEditorScreen(
                    application = application,
                    languageCode = uiState.settings.languageCode,
                    timeMorning = uiState.settings.timeMorning,
                    timeDay = uiState.settings.timeDay,
                    timeEvening = uiState.settings.timeEvening,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { itemId ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("memos_refresh", true)
                        navController.navigate(memoEditRoute(itemId)) {
                            popUpTo(Routes.MemoNew) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.MemoEdit) { backStackEntry ->
                val application = LocalContext.current.applicationContext as MemListsApplication
                val memoId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                if (memoId == null) {
                    navController.popBackStack()
                    return@composable
                }
                MemoEditorScreen(
                    application = application,
                    languageCode = uiState.settings.languageCode,
                    timeMorning = uiState.settings.timeMorning,
                    timeDay = uiState.settings.timeDay,
                    timeEvening = uiState.settings.timeEvening,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("memos_refresh", true)
                    },
                    memoId = memoId
                )
            }
            composable(Routes.Lists) {
                val listsViewModel: ListsViewModel = viewModel()
                val listsUiState by listsViewModel.uiState.collectAsState()
                val refreshHandle = navController.currentBackStackEntry?.savedStateHandle
                val shouldRefresh by refreshHandle
                    ?.getStateFlow("lists_refresh", false)
                    ?.collectAsState()
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    listsViewModel.refresh()
                }

                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        listsViewModel.refresh()
                        refreshHandle?.set("lists_refresh", false)
                    }
                }

                ListsHomeScreen(
                    currentFolderName = listsUiState.currentFolderName,
                    currentFolderId = listsUiState.currentFolderId,
                    isLoading = listsUiState.isLoading,
                    containers = listsUiState.containers,
                    lw = lw,
                    onNavigateBack = {
                        if (listsUiState.currentFolderId == null) {
                            navController.popBackStack()
                        } else {
                            listsViewModel.leaveFolder()
                        }
                    },
                    onOpenFolder = listsViewModel::openFolder,
                    onOpenList = { listId -> navController.navigate(listDetailRoute(listId)) },
                    onAddList = { navController.navigate(listNewRoute(listsUiState.currentFolderId, isFolder = false)) },
                    onAddFolder = { navController.navigate(listNewRoute(null, isFolder = true)) }
                )
            }
            composable(Routes.ListNew) { backStackEntry ->
                val application = LocalContext.current.applicationContext as MemListsApplication
                val parentId = backStackEntry.arguments?.getString("parentId")?.toLongOrNull()?.takeIf { it >= 0L }
                val isFolder = backStackEntry.arguments?.getString("isFolder") == "1"
                ListEditorScreen(
                    application = application,
                    isFolder = isFolder,
                    parentId = parentId,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("lists_refresh", true)
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.ListDetail) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId")?.toLongOrNull() ?: return@composable
                val detailViewModel: ListDetailViewModel = viewModel()
                val detailUiState by detailViewModel.uiState.collectAsState()
                val refreshHandle = navController.currentBackStackEntry?.savedStateHandle
                val shouldRefresh by refreshHandle
                    ?.getStateFlow("list_detail_refresh", false)
                    ?.collectAsState()
                    ?: remember { mutableStateOf(false) }

                LaunchedEffect(listId) {
                    detailViewModel.load(listId)
                }
                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        detailViewModel.load(listId)
                        refreshHandle?.set("list_detail_refresh", false)
                    }
                }

                ListDetailScreen(
                    title = detailUiState.title,
                    comment = detailUiState.comment,
                    isLoading = detailUiState.isLoading,
                    uncheckedEntries = detailUiState.uncheckedEntries,
                    checkedEntries = detailUiState.checkedEntries,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onAddEntry = { navController.navigate(entryNewRoute(listId)) },
                    onToggleChecked = detailViewModel::toggleChecked
                )
            }
            composable(Routes.EntryNew) { backStackEntry ->
                val application = LocalContext.current.applicationContext as MemListsApplication
                val listId = backStackEntry.arguments?.getString("listId")?.toLongOrNull() ?: return@composable
                ListEntryEditorScreen(
                    application = application,
                    listId = listId,
                    lw = lw,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("list_detail_refresh", true)
                        navController.popBackStack()
                    }
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
                    onLargeFontWakeLockChanged = viewModel::updateLargeFontWakeLock,
                    onTimeMorningChanged = viewModel::updateTimeMorning,
                    onTimeDayChanged = viewModel::updateTimeDay,
                    onTimeEveningChanged = viewModel::updateTimeEvening,
                    onDefaultSoundChanged = viewModel::updateDefaultSound,
                    onSoundRepeatsChanged = viewModel::updateSoundRepeats,
                    onRestoreComplete = {
                        viewModel.reloadAfterRestore {
                            runCatching {
                                navController.getBackStackEntry(Routes.Memos)
                                    .savedStateHandle["memos_refresh"] = true
                            }
                            runCatching {
                                navController.getBackStackEntry(Routes.Lists)
                                    .savedStateHandle["lists_refresh"] = true
                            }
                        }
                    }
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
        selectedLanguageCode = uiState.settings.languageCode,
        selectedThemeName = uiState.settings.themeName,
        lw = lw,
        onLanguageSelected = viewModel::previewWelcomeLanguage,
        onThemeSelected = viewModel::previewWelcomeTheme,
        onStart = viewModel::saveWelcomeSelection
    )
}
