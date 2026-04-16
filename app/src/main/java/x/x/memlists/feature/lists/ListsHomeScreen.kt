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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import x.x.memlists.core.data.ListContainerSummary
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ConfirmDeleteDialog
import x.x.memlists.core.ui.UiTokens

@Composable
fun ListsHomeScreen(
    currentFolderName: String?,
    currentFolderId: Long?,
    isLoading: Boolean,
    containers: List<ListContainerSummary>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onOpenFolder: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    onAddList: () -> Unit,
    onAddFolder: () -> Unit,
    onEditList: (Long) -> Unit,
    onDeleteList: (Long) -> Unit
) {
    val palette = LocalAppThemePalette.current
    var pendingDelete by remember { mutableStateOf<ListContainerSummary?>(null) }

    pendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = lw(if (target.isFolder) "Delete folder" else "Delete list"),
            itemName = target.name,
            lw = lw,
            onConfirm = {
                val id = target.id
                pendingDelete = null
                onDeleteList(id)
            },
            onDismiss = { pendingDelete = null }
        )
    }

    ScreenScaffold(
        title = lw(currentFolderName ?: "Lists"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentFolderId == null) {
                    SmallFloatingActionButton(
                        onClick = onAddFolder,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = lw("New folder")
                        )
                    }
                }
                FloatingActionButton(
                    onClick = onAddList,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = lw("New list")
                    )
                }
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
                                text = lw("Loading lists"),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
            } else if (containers.isEmpty()) {
                item {
                    HeroCard(
                        title = lw("No lists yet"),
                        body = lw("The lists home is empty.")
                    )
                }
            } else {
                items(containers, key = { it.id }) { container ->
                    SwipeableContainerRow(
                        container = container,
                        lw = lw,
                        onOpen = {
                            if (container.isFolder) onOpenFolder(container.id)
                            else onOpenList(container.id)
                        },
                        onEdit = { onEditList(container.id) },
                        onRequestDelete = { pendingDelete = container }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableContainerRow(
    container: ListContainerSummary,
    lw: (String) -> String,
    onOpen: () -> Unit,
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
                onClick = onOpen,
                onLongClick = { contextMenuExpanded = true }
            )
        ) {
            ListContainerCard(container = container)
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
private fun ListContainerCard(container: ListContainerSummary) {
    val palette = LocalAppThemePalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (container.isFolder) Icons.Default.FolderCopy else Icons.Default.Checklist,
                contentDescription = container.name,
                tint = palette.clText,
                modifier = Modifier.padding(top = 4.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = container.name,
                        color = palette.clText,
                        fontSize = if (!container.isFolder && container.uncheckedCount > 0) UiTokens.fsLarge else UiTokens.fsMedium,
                        fontWeight = if (!container.isFolder && container.uncheckedCount > 0) FontWeight.Bold else FontWeight.Medium,
                        textDecoration = if (!container.isFolder && container.totalCount > 0 && container.uncheckedCount == 0) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (container.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = container.name,
                            tint = palette.clText
                        )
                    }
                }
                container.comment?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = palette.clText.copy(alpha = 0.8f),
                        fontSize = UiTokens.fsNormal
                    )
                }
                if (!container.isFolder) {
                    Text(
                        text = "${container.uncheckedCount}/${container.totalCount}",
                        color = palette.clText.copy(alpha = 0.75f),
                        fontSize = UiTokens.fsNormal
                    )
                }
            }
        }
    }
}
