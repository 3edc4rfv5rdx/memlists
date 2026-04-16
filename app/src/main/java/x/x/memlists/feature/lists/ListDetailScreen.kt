package x.x.memlists.feature.lists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.ListEntrySummary
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ConfirmDeleteDialog
import x.x.memlists.core.ui.UiTokens
import androidx.compose.material.icons.filled.Image

@Composable
fun ListDetailScreen(
    title: String,
    comment: String?,
    isLoading: Boolean,
    uncheckedEntries: List<ListEntrySummary>,
    checkedEntries: List<ListEntrySummary>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onAddEntry: () -> Unit,
    onToggleChecked: (Long, Boolean) -> Unit,
    onEditEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val palette = LocalAppThemePalette.current
    var pendingDeleteEntry by remember { mutableStateOf<ListEntrySummary?>(null) }

    pendingDeleteEntry?.let { target ->
        ConfirmDeleteDialog(
            title = lw("Delete item"),
            itemName = target.name,
            lw = lw,
            onConfirm = {
                val id = target.id
                pendingDeleteEntry = null
                onDeleteEntry(id)
            },
            onDismiss = { pendingDeleteEntry = null }
        )
    }

    ScreenScaffold(
        title = title,
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = lw("New item")
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            comment?.takeIf { it.isNotBlank() }?.let { text ->
                item {
                    Text(
                        text = text,
                        color = palette.clText.copy(alpha = 0.8f),
                        fontSize = UiTokens.fsNormal,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
                                text = lw("Loading items"),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
            } else {
                if (uncheckedEntries.isEmpty()) {
                    item {
                        HeroCard(
                            title = lw("No active items"),
                            body = lw("Add the first item to this list.")
                        )
                    }
                } else {
                    items(uncheckedEntries, key = { it.id }) { entry ->
                        SwipeableEntryRow(
                            entry = entry,
                            checked = false,
                            lw = lw,
                            onToggleChecked = onToggleChecked,
                            onEdit = { onEditEntry(entry.id) },
                            onRequestDelete = { pendingDeleteEntry = entry }
                        )
                    }
                }

                if (checkedEntries.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = palette.clText.copy(alpha = 0.4f), thickness = 2.dp)
                    }
                    items(checkedEntries, key = { it.id }) { entry ->
                        SwipeableEntryRow(
                            entry = entry,
                            checked = true,
                            lw = lw,
                            onToggleChecked = onToggleChecked,
                            onEdit = { onEditEntry(entry.id) },
                            onRequestDelete = { pendingDeleteEntry = entry }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableEntryRow(
    entry: ListEntrySummary,
    checked: Boolean,
    lw: (String) -> String,
    onToggleChecked: (Long, Boolean) -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                SwipeToDismissBoxValue.EndToStart -> { onRequestDelete(); false }
                else -> true
            }
        }
    )
    var contextMenuExpanded by remember { mutableStateOf(false) }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isEdit = direction == SwipeToDismissBoxValue.StartToEnd
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val bgColor = when {
                isEdit -> palette.clSel
                isDelete -> Color.Red
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, UiTokens.shapeLarge)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isEdit) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when {
                    isEdit -> Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = lw("Edit"),
                        tint = palette.clText
                    )
                    isDelete -> Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = lw("Delete"),
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEdit,
                onLongClick = { contextMenuExpanded = true }
            )
        ) {
            ListEntryCard(
                entry = entry,
                checked = checked,
                onToggleChecked = onToggleChecked
            )
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                offset = DpOffset(x = 120.dp, y = 0.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(lw("Edit"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { contextMenuExpanded = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text(lw("Delete"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { contextMenuExpanded = false; onRequestDelete() }
                )
            }
        }
    }
}

@Composable
private fun ListEntryCard(
    entry: ListEntrySummary,
    checked: Boolean,
    onToggleChecked: (Long, Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggleChecked(entry.id, it) }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.name,
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                )
                val quantityLine = buildList {
                    entry.quantity?.takeIf { it.isNotBlank() }?.let { add(it) }
                    entry.unit?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" ")
                if (quantityLine.isNotBlank()) {
                    Text(
                        text = quantityLine,
                        color = palette.clText.copy(alpha = 0.75f),
                        fontSize = UiTokens.fsNormal
                    )
                }
            }
            if (entry.photoCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = palette.clText
                    )
                    Text(
                        text = entry.photoCount.toString(),
                        color = palette.clText,
                        fontSize = UiTokens.fsNormal
                    )
                }
            }
        }
    }
}
