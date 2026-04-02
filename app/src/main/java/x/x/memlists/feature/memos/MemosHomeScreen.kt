package x.x.memlists.feature.memos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.MemoFolderSummary
import x.x.memlists.core.data.MemoFolderType
import x.x.memlists.core.data.MemoItemSummary
import x.x.memlists.core.data.firstDailyTime
import x.x.memlists.core.data.formatDate
import x.x.memlists.core.data.formatTime
import x.x.memlists.core.data.formatTimes
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.SpacerBlock
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
    onBackFromFolder: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    ScreenScaffold(
        title = lw(selectedFolder?.titleKey ?: "MemLists"),
        canNavigateBack = selectedFolder != null,
        onNavigateBack = onBackFromFolder,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMemo) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = lw("New memo")
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
            item {
                HeroCard(
                    title = lw("Memos"),
                    body = if (selectedFolder == null) lw("Main screen") else lw("Folders and lists"),
                    icon = Icons.AutoMirrored.Filled.StickyNote2
                )
            }
            if (selectedFolder == null) {
                item {
                    HeroCard(
                        title = lw("Open Lists"),
                        body = lw("Folders and lists"),
                        icon = Icons.Default.Checklist,
                        onClick = onOpenLists
                    )
                }
                item {
                    HeroCard(
                        title = lw("Open settings"),
                        body = lw("Shared settings"),
                        icon = Icons.Default.Settings,
                        onClick = onOpenSettings
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
            } else if (items.isEmpty()) {
                item {
                    HeroCard(
                        title = lw("No items yet"),
                        body = lw("The memos list is empty.")
                    )
                }
            } else {
                items(items, key = { it.id }) { item ->
                    MemoItemCard(item = item, lw = lw)
                }
            }
            if (selectedFolder == null && folders.isNotEmpty()) {
                item {
                    SpacerBlock()
                }
                item {
                    SectionTitle(title = lw("Folders"))
                }
                items(folders, key = { it.type.name }) { folder ->
                    HeroCard(
                        title = lw(folder.type.titleKey),
                        body = folder.count.toString(),
                        icon = Icons.Default.Checklist,
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
    lw: (String) -> String
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = palette.clText,
                    fontSize = UiTokens.fsMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (item.priority > 0) {
                    Text(
                        text = "★".repeat(item.priority),
                        color = palette.clText,
                        fontSize = UiTokens.fsNormal
                    )
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
                    fontSize = UiTokens.fsSmall
                )
            }
            memoDetailsLine(item)?.let {
                Text(
                    text = it,
                    color = palette.clText.copy(alpha = 0.84f),
                    fontSize = UiTokens.fsSmall
                )
            }
            if (item.photoCount > 0) {
                Text(
                    text = item.photoCount.toString(),
                    color = palette.clText.copy(alpha = 0.72f),
                    fontSize = UiTokens.fsSmall
                )
            }
        }
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
