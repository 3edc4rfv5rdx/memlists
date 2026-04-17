package x.x.memlists.feature.lists

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import x.x.memlists.core.data.ListEntrySummary
import x.x.memlists.core.photo.MAX_PHOTOS_PER_OWNER
import x.x.memlists.core.photo.PhotoEntry
import x.x.memlists.core.photo.PhotoOwnerType
import x.x.memlists.core.photo.PhotoRepository
import x.x.memlists.core.photo.PhotoStorage
import x.x.memlists.core.photo.PhotoViewerOverlay
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.ConfirmDeleteDialog
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.UiTokens
import java.io.File

@Composable
fun ListDetailScreen(
    title: String,
    comment: String?,
    isLoading: Boolean,
    uncheckedEntries: List<ListEntrySummary>,
    checkedEntries: List<ListEntrySummary>,
    photoRepository: PhotoRepository,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onAddEntry: () -> Unit,
    onToggleChecked: (Long, Boolean) -> Unit,
    onEditEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onPhotosChanged: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingDeleteEntry by remember { mutableStateOf<ListEntrySummary?>(null) }

    // Photo state
    var addPhotoEntryId by remember { mutableStateOf<Long?>(null) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var limitMessage by remember { mutableStateOf<String?>(null) }

    var viewerEntryId by remember { mutableStateOf<Long?>(null) }
    var viewerEntries by remember { mutableStateOf<List<PhotoEntry>>(emptyList()) }
    var viewerDirty by remember { mutableStateOf(false) }

    var removeAllEntryId by remember { mutableStateOf<Long?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCaptureFile
        val entryId = addPhotoEntryId
        pendingCaptureFile = null
        addPhotoEntryId = null
        if (success && file != null && entryId != null) {
            scope.launch {
                photoRepository.insert(PhotoOwnerType.Entry, entryId, file.absolutePath)
                onPhotosChanged()
            }
        } else {
            file?.delete()
        }
    }

    fun launchCameraWithFile() {
        val entryId = addPhotoEntryId ?: return
        val dir = PhotoStorage.ownerDir(context, PhotoOwnerType.Entry, entryId)
        val file = PhotoStorage.newPhotoFile(dir)
        pendingCaptureFile = file
        cameraLauncher.launch(PhotoStorage.contentUriFor(context, file))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraWithFile()
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val entryId = addPhotoEntryId
        addPhotoEntryId = null
        if (uri != null && entryId != null) {
            scope.launch {
                val dir = PhotoStorage.ownerDir(context, PhotoOwnerType.Entry, entryId)
                val dest = PhotoStorage.newPhotoFile(dir)
                val ok = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    } != null
                }.getOrDefault(false)
                if (ok) {
                    photoRepository.insert(PhotoOwnerType.Entry, entryId, dest.absolutePath)
                } else {
                    dest.delete()
                }
                onPhotosChanged()
            }
        }
    }

    fun requestAddPhoto(entryId: Long, currentCount: Int) {
        if (currentCount >= MAX_PHOTOS_PER_OWNER) {
            limitMessage = lw("Max photos reached")
            return
        }
        addPhotoEntryId = entryId
        showSourceMenu = true
    }

    fun openViewer(entryId: Long) {
        viewerEntryId = entryId
        viewerDirty = false
        scope.launch {
            val refs = photoRepository.list(PhotoOwnerType.Entry, entryId)
            viewerEntries = refs.map { PhotoEntry(dbId = it.id, path = it.path) }
        }
    }

    // Delete entry dialog
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
                            onRequestDelete = { pendingDeleteEntry = entry },
                            onAddPhoto = { requestAddPhoto(entry.id, entry.photoCount) },
                            onViewPhotos = if (entry.photoCount > 0) {
                                { openViewer(entry.id) }
                            } else null,
                            onRemovePhotos = if (entry.photoCount > 0) {
                                { removeAllEntryId = entry.id }
                            } else null
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
                            onRequestDelete = { pendingDeleteEntry = entry },
                            onAddPhoto = { requestAddPhoto(entry.id, entry.photoCount) },
                            onViewPhotos = if (entry.photoCount > 0) {
                                { openViewer(entry.id) }
                            } else null,
                            onRemovePhotos = if (entry.photoCount > 0) {
                                { removeAllEntryId = entry.id }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Source menu dialog (Camera / Gallery)
    if (showSourceMenu) {
        AlertDialog(
            onDismissRequest = { showSourceMenu = false; addPhotoEntryId = null },
            containerColor = palette.clMenu,
            title = { Text(lw("Add photo"), color = palette.clText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showSourceMenu = false
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCameraWithFile()
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.clUpBar,
                            contentColor = palette.clText
                        ),
                        shape = UiTokens.shapeMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Text("  ${lw("Camera")}")
                    }
                    Button(
                        onClick = {
                            showSourceMenu = false
                            galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.clUpBar,
                            contentColor = palette.clText
                        ),
                        shape = UiTokens.shapeMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text("  ${lw("Gallery")}")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSourceMenu = false; addPhotoEntryId = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium
                ) { Text(lw("Cancel")) }
            }
        )
    }

    // Photo limit dialog
    limitMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { limitMessage = null },
            containerColor = palette.clMenu,
            title = { Text(lw("Photos"), color = palette.clText) },
            text = { Text(msg, color = palette.clText) },
            confirmButton = {
                Button(
                    onClick = { limitMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium
                ) { Text("OK") }
            }
        )
    }

    // Remove all photos confirmation
    removeAllEntryId?.let { entryId ->
        RemoveAllPhotosDialog(
            lw = lw,
            photoRepository = photoRepository,
            entryId = entryId,
            context = context,
            onDone = {
                removeAllEntryId = null
                onPhotosChanged()
            },
            onDismiss = { removeAllEntryId = null }
        )
    }

    // Photo viewer overlay
    if (viewerEntryId != null && viewerEntries.isNotEmpty()) {
        PhotoViewerOverlay(
            entries = viewerEntries,
            initialIndex = 0,
            lw = lw,
            onClose = {
                viewerEntryId = null
                if (viewerDirty) {
                    viewerDirty = false
                    onPhotosChanged()
                }
            },
            onDelete = { entry ->
                val dbId = entry.dbId ?: return@PhotoViewerOverlay
                scope.launch {
                    photoRepository.delete(dbId)
                    viewerEntries = viewerEntries.filter { it.dbId != dbId }
                    viewerDirty = true
                    if (viewerEntries.isEmpty()) {
                        viewerEntryId = null
                        onPhotosChanged()
                    }
                }
            }
        )
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
    onRequestDelete: () -> Unit,
    onAddPhoto: () -> Unit,
    onViewPhotos: (() -> Unit)?,
    onRemovePhotos: (() -> Unit)?
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
                onClick = {},
                onLongClick = { contextMenuExpanded = true }
            )
        ) {
            ListEntryCard(
                entry = entry,
                checked = checked,
                onToggleChecked = onToggleChecked,
                onPhotoTap = onViewPhotos
            )
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                offset = DpOffset(x = 120.dp, y = 0.dp),
                containerColor = palette.clMenu
            ) {
                DropdownMenuItem(
                    text = { Text(lw("Edit"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { contextMenuExpanded = false; onEdit() }
                )
                val photoLabel = if (entry.photoCount > 0) {
                    "${lw("Add photo")} (${entry.photoCount})"
                } else {
                    lw("Add photo")
                }
                DropdownMenuItem(
                    text = { Text(photoLabel, fontSize = UiTokens.fsNormal, color = palette.clText) },
                    onClick = { contextMenuExpanded = false; onAddPhoto() }
                )
                if (onViewPhotos != null) {
                    DropdownMenuItem(
                        text = { Text(lw("View photos"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                        onClick = { contextMenuExpanded = false; onViewPhotos() }
                    )
                }
                if (onRemovePhotos != null) {
                    DropdownMenuItem(
                        text = { Text(lw("Remove photo"), fontSize = UiTokens.fsNormal, color = palette.clText) },
                        onClick = { contextMenuExpanded = false; onRemovePhotos() }
                    )
                }
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
    onToggleChecked: (Long, Boolean) -> Unit,
    onPhotoTap: (() -> Unit)? = null
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
                    modifier = if (onPhotoTap != null) {
                        Modifier.clickable { onPhotoTap() }
                    } else Modifier,
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

@Composable
private fun RemoveAllPhotosDialog(
    lw: (String) -> String,
    photoRepository: PhotoRepository,
    entryId: Long,
    context: android.content.Context,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.clMenu,
        title = { Text(lw("Remove all photos?"), color = palette.clText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val photos = photoRepository.list(PhotoOwnerType.Entry, entryId)
                            photos.forEach { ref ->
                                PhotoStorage.saveToDeviceGallery(context, java.io.File(ref.path))
                            }
                            photoRepository.deleteAllForOwner(PhotoOwnerType.Entry, entryId)
                            onDone()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(lw("Move to device gallery")) }
                Button(
                    onClick = {
                        scope.launch {
                            photoRepository.deleteAllForOwner(PhotoOwnerType.Entry, entryId)
                            onDone()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(lw("Delete permanently")) }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText
                ),
                shape = UiTokens.shapeMedium
            ) { Text(lw("Cancel")) }
        }
    )
}
