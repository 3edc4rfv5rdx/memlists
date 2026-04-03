package x.x.memlists.feature.memos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.MemoFolderSummary
import x.x.memlists.core.data.MemoFolderType
import x.x.memlists.core.data.MemoItemSummary
import x.x.memlists.core.data.todayAsInt
import x.x.memlists.core.data.firstDailyTime
import x.x.memlists.core.data.formatDate
import x.x.memlists.core.data.formatTime
import x.x.memlists.core.data.formatTimes
import x.x.memlists.core.theme.AppThemePalette
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.UiTokens

@Composable
fun MemosHomeScreen(
    selectedFolder: MemoFolderType?,
    isLoading: Boolean,
    items: List<MemoItemSummary>,
    folders: List<MemoFolderSummary>,
    lw: (String) -> String,
    onOpenLists: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddMemo: () -> Unit,
    onOpenFolder: (MemoFolderType) -> Unit,
    onBackFromFolder: () -> Unit,
    onCloseRoot: () -> Unit,
    onToggleActive: (MemoItemSummary) -> Unit = {}
) {
    val palette = LocalAppThemePalette.current
    val visibleItems = if (selectedFolder == null) {
        items.filter { it.reminderType == 1 && !it.monthly && !it.yearly }
    } else {
        items
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) {
        x.x.memlists.core.ui.AboutDialog(
            lw = lw,
            versionName = x.x.memlists.BuildConfig.VERSION_NAME,
            versionCode = x.x.memlists.BuildConfig.VERSION_CODE,
            onDismiss = { showAbout = false }
        )
    }
    ScreenScaffold(
        title = lw(selectedFolder?.titleKey ?: "MemLists"),
        navigationButtonMode = if (selectedFolder == null) NavigationButtonMode.Close else NavigationButtonMode.Back,
        onNavigateBack = {
            if (selectedFolder == null) {
                onCloseRoot()
            } else {
                onBackFromFolder()
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMemo,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = lw("New memo")
                )
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = lw("Check Reminders")
                )
            }
            TextButton(onClick = { }) {
                Text(
                    text = lw("(All)"),
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = lw("Menu")
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(lw("Clear All Filters")) },
                    onClick = { menuExpanded = false },
                    enabled = false
                )
                DropdownMenuItem(
                    text = { Text(lw("Filters")) },
                    onClick = { menuExpanded = false },
                    enabled = false
                )
                DropdownMenuItem(
                    text = { Text(lw("Tag Filter")) },
                    onClick = { menuExpanded = false },
                    enabled = false
                )
                DropdownMenuItem(
                    text = { Text(lw("Settings")) },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text(lw("About")) },
                    onClick = {
                        menuExpanded = false
                        showAbout = true
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedFolder == null) {
                item {
                    MemoNavigationRow(
                        title = lw("Lists"),
                        subtitle = null,
                        palette = palette,
                        onClick = onOpenLists
                    )
                }
            }
            if (isLoading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.clFill),
                        shape = UiTokens.shapeLarge
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = lw("Loading memos"),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
            } else if (visibleItems.isEmpty() && (selectedFolder != null || folders.isEmpty())) {
                item {
                    MemoEmptyRow(
                        text = lw("No items yet"),
                        palette = palette
                    )
                }
            } else {
                items(visibleItems, key = { it.id }) { item ->
                    MemoItemCard(
                        item = item,
                        lw = lw,
                        palette = palette,
                        onToggleActive = { onToggleActive(item) }
                    )
                }
            }
            if (selectedFolder == null && folders.isNotEmpty()) {
                items(folders, key = { it.type.name }) { folder ->
                    MemoNavigationRow(
                        title = lw(folder.type.titleKey),
                        subtitle = folder.count.toString(),
                        palette = palette,
                        onClick = { onOpenFolder(folder.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoItemCard(
    item: MemoItemSummary,
    lw: (String) -> String,
    palette: AppThemePalette,
    onToggleActive: () -> Unit = {}
) {
    val isToday = item.date == todayAsInt()
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) palette.clSel.copy(alpha = 0.45f) else palette.clFill
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (item.reminderType != 0) {
                Checkbox(
                    checked = item.active,
                    onCheckedChange = { onToggleActive() }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (item.reminderType == 0) 18.dp else 0.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = if (isToday) androidx.compose.ui.graphics.Color.Red else palette.clText,
                    fontSize = UiTokens.fsMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (item.priority > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(item.priority) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = item.title,
                                tint = palette.clText
                            )
                        }
                    }
                }
            }
            item.content?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = palette.clText.copy(alpha = 0.84f),
                    fontSize = UiTokens.fsNormal
                )
            }
            item.tags?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "${lw("Tags")}: $it",
                    color = palette.clText.copy(alpha = 0.8f),
                    fontSize = UiTokens.fsNormal
                )
            }
            memoDetailsLine(item)?.let {
                Text(
                    text = it,
                    color = if (isToday || item.reminderType != 0) androidx.compose.ui.graphics.Color.Red else palette.clText.copy(alpha = 0.84f),
                    fontSize = UiTokens.fsNormal
                )
            }
            if (item.photoCount > 0) {
                Text(
                    text = item.photoCount.toString(),
                    color = palette.clText.copy(alpha = 0.72f),
                    fontSize = UiTokens.fsNormal
                )
            }
            }
        }
    }
}

@Composable
private fun MemoNavigationRow(
    title: String,
    subtitle: String?,
    palette: AppThemePalette,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderCopy,
                contentDescription = title,
                tint = palette.clText
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = palette.clText,
                    fontSize = UiTokens.fsMedium,
                    fontWeight = FontWeight.Bold
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = palette.clText.copy(alpha = 0.82f),
                        fontSize = UiTokens.fsNormal
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoEmptyRow(
    text: String,
    palette: AppThemePalette
) {
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            color = palette.clText,
            fontSize = UiTokens.fsNormal
        )
    }
}

private fun memoDetailsLine(item: MemoItemSummary): String? {
    return when (item.reminderType) {
        2 -> formatTimes(item.timesJson)
        3 -> listOfNotNull(
            formatDate(item.date),
            formatDate(item.dateTo)
        ).joinToString(" - ").ifBlank {
            formatTime(item.time)
        }
        else -> listOfNotNull(formatDate(item.date), formatTime(item.time)).joinToString("  ").ifBlank {
            firstDailyTime(item.timesJson)
        }
    }?.ifBlank { null }
}
