package x.x.memlists.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.photo.PhotoGallerySection
import x.x.memlists.core.photo.PhotoOwnerType
import x.x.memlists.core.photo.PhotoViewerOverlay
import x.x.memlists.core.photo.rememberPhotoGalleryState
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SnackbarTone
import x.x.memlists.core.ui.UiTokens
import x.x.memlists.core.ui.showThemedSnackbar

@Composable
fun ListEntryEditorScreen(
    application: MemListsApplication,
    listId: Long,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    entryId: Long? = null
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val isEdit = entryId != null
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    val photoGalleryState = rememberPhotoGalleryState(
        context = context,
        repository = application.photoRepository,
        ownerType = PhotoOwnerType.Entry,
        initialOwnerId = entryId
    )
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(entryId) {
        if (isEdit) {
            val entry = application.repository.loadListEntry(entryId)
            name = entry.name
            quantity = entry.quantity.orEmpty()
            unit = entry.unit.orEmpty()
        } else {
            focusRequester.requestFocus()
        }
    }

    fun save() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            scope.launch { snackbarHostState.showThemedSnackbar(lw("Name is required"), SnackbarTone.Error) }
            return
        }
        scope.launch {
            try {
                if (isEdit) {
                    application.repository.updateListEntry(
                        entryId = entryId,
                        name = trimmedName,
                        quantity = quantity.trim().ifBlank { null },
                        unit = unit.trim().ifBlank { null }
                    )
                } else {
                    val newId = application.repository.insertListEntry(
                        listId = listId,
                        name = trimmedName,
                        quantity = quantity.trim().ifBlank { null },
                        unit = unit.trim().ifBlank { null }
                    )
                    photoGalleryState.commitPending(newId)
                }
                onSaved()
                snackbarHostState.showThemedSnackbar(lw("Saved"), SnackbarTone.Success)
            } catch (e: Exception) {
                snackbarHostState.showThemedSnackbar(lw("Save failed"), SnackbarTone.Error)
            }
        }
    }

    ScreenScaffold(
        title = lw(if (isEdit) "Edit item" else "New item"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { save() }) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = lw("Save"),
                    tint = palette.clText
                )
            }
        }
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            Card(
                shape = UiTokens.shapeLarge,
                colors = CardDefaults.cardColors(containerColor = palette.clFill)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        label = { Text(lw("Name")) }
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Quantity")) }
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Unit")) }
                    )
                    PhotoGallerySection(
                        state = photoGalleryState,
                        lw = lw,
                        onOpenViewer = { viewerIndex = it }
                    )
                }
            }
        }
    }

    viewerIndex?.let { startIndex ->
        val entries = photoGalleryState.entries
        if (entries.isEmpty()) {
            viewerIndex = null
        } else {
            PhotoViewerOverlay(
                entries = entries,
                initialIndex = startIndex,
                lw = lw,
                onClose = { viewerIndex = null },
                onDelete = { entry ->
                    scope.launch {
                        photoGalleryState.delete(entry)
                        if (photoGalleryState.entries.isEmpty()) {
                            viewerIndex = null
                        }
                    }
                }
            )
        }
    }
}
