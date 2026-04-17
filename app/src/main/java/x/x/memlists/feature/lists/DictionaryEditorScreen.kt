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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import x.x.memlists.core.data.DictionaryItem
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.ConfirmDeleteDialog
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.SnackbarTone
import x.x.memlists.core.ui.UiTokens
import x.x.memlists.core.ui.showThemedSnackbar

@Composable
fun DictionaryEditorScreen(
    lw: (String) -> String,
    onNavigateBack: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val viewModel: DictionaryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingDelete by remember { mutableStateOf<DictionaryItem?>(null) }
    var pendingEdit by remember { mutableStateOf<DictionaryItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    fun showDuplicate() {
        scope.launch {
            snackbarHostState.showThemedSnackbar(lw("Already exists"), SnackbarTone.Caution)
        }
    }

    fun showSaveError() {
        scope.launch {
            snackbarHostState.showThemedSnackbar(lw("Save failed"), SnackbarTone.Error)
        }
    }

    fun showDeleteError() {
        scope.launch {
            snackbarHostState.showThemedSnackbar(lw("Delete failed"), SnackbarTone.Error)
        }
    }

    ScreenScaffold(
        title = lw("Items dictionary"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = lw("Add"))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(lw("Search")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            if (state.items.isEmpty() && !state.isLoading) {
                HeroCard(
                    title = if (state.query.isBlank()) lw("Empty dictionary")
                            else lw("Nothing found")
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        DictionaryRow(
                            item = item,
                            lw = lw,
                            onEdit = { pendingEdit = item },
                            onDelete = { pendingDelete = item }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = lw("Delete item"),
            itemName = target.name,
            lw = lw,
            onConfirm = {
                viewModel.delete(target.id, onError = { showDeleteError() })
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showAdd) {
        DictionaryEditDialog(
            initialName = "",
            initialUnit = "",
            isEdit = false,
            lw = lw,
            onSave = { name, unit ->
                viewModel.add(
                    name = name,
                    unit = unit,
                    onDuplicate = { showDuplicate() },
                    onDone = { showAdd = false },
                    onAdded = {
                        scope.launch {
                            snackbarHostState.showThemedSnackbar(lw("Added"), SnackbarTone.Success)
                        }
                    },
                    onError = { showSaveError() }
                )
            },
            onDismiss = { showAdd = false }
        )
    }

    pendingEdit?.let { target ->
        DictionaryEditDialog(
            initialName = target.name,
            initialUnit = target.unit.orEmpty(),
            isEdit = true,
            lw = lw,
            onSave = { name, unit ->
                viewModel.update(
                    id = target.id,
                    name = name,
                    unit = unit,
                    onDuplicate = { showDuplicate() },
                    onDone = { pendingEdit = null },
                    onError = { showSaveError() }
                )
            },
            onDismiss = { pendingEdit = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryRow(
    item: DictionaryItem,
    lw: (String) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    var menuExpanded by remember { mutableStateOf(false) }
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                else -> true
            }
        }
    )
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
                onClick = { menuExpanded = true },
                onLongClick = { menuExpanded = true }
            )
        ) {
            Card(
                shape = UiTokens.shapeLarge,
                colors = CardDefaults.cardColors(containerColor = palette.clFill)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = palette.clText,
                        modifier = Modifier.size(8.dp)
                    )
                    Text(
                        text = item.name,
                        color = palette.clText,
                        fontSize = UiTokens.fsNormal,
                        fontWeight = FontWeight.Medium
                    )
                    item.unit?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "/$it",
                            color = palette.clText.copy(alpha = 0.7f),
                            fontSize = UiTokens.fsNormal
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(x = 120.dp, y = 0.dp),
                containerColor = palette.clMenu
            ) {
                DropdownMenuItem(
                    text = { Text(lw("Edit"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { menuExpanded = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text(lw("Delete"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun DictionaryEditDialog(
    initialName: String,
    initialUnit: String,
    isEdit: Boolean,
    lw: (String) -> String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(initialUnit) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = UiTokens.shapeLarge,
                colors = CardDefaults.cardColors(containerColor = palette.clFill),
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = lw(if (isEdit) "Edit item" else "Add item"),
                        color = palette.clText,
                        fontSize = UiTokens.fsMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(lw("Name")) }
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(lw("Unit")) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = UiTokens.shapeMedium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.clUpBar,
                                contentColor = palette.clText
                            )
                        ) { Text(lw("Cancel"), fontSize = UiTokens.fsNormal, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { onSave(name, unit) },
                            shape = UiTokens.shapeMedium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.clUpBar,
                                contentColor = palette.clText
                            )
                        ) { Text(lw("Save"), fontSize = UiTokens.fsNormal, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
