package x.x.memlists.core.photo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.UiTokens
import java.io.File

@Composable
fun PhotoGallerySection(
    state: PhotoGalleryState,
    lw: (String) -> String,
    onOpenViewer: (Int) -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PhotoEntry?>(null) }
    var limitMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.ownerId) {
        if (state.ownerId != null) state.reloadCommitted()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCaptureFile
        pendingCaptureFile = null
        if (success && file != null) {
            scope.launch { state.attachCaptured(file) }
        } else {
            file?.delete()
        }
    }

    fun launchCameraWithFile() {
        val file = state.newCaptureFile()
        pendingCaptureFile = file
        cameraLauncher.launch(PhotoStorage.contentUriFor(context, file))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraWithFile()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { state.importFromUri(uri) }
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCameraWithFile()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun requestAdd() {
        if (state.count >= MAX_PHOTOS_PER_OWNER) {
            limitMessage = lw("Max photos reached")
            return
        }
        showSourceMenu = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${lw("Photos")} (${state.count}/$MAX_PHOTOS_PER_OWNER)",
                color = palette.clText,
                fontSize = UiTokens.fsNormal
            )
            Button(
                onClick = { requestAdd() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText
                ),
                shape = UiTokens.shapeMedium
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = lw("Add photo"))
            }
        }

        if (state.entries.isEmpty()) {
            Text(
                text = lw("No photos"),
                color = palette.clText.copy(alpha = 0.6f),
                fontSize = UiTokens.fsSmall
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                val snapshot = state.entries
                items(snapshot.size) { index ->
                    PhotoThumb(
                        entry = snapshot[index],
                        onClick = { onOpenViewer(index) },
                        onLongClick = { pendingDelete = snapshot[index] }
                    )
                }
            }
        }
    }

    if (showSourceMenu) {
        AlertDialog(
            onDismissRequest = { showSourceMenu = false },
            containerColor = palette.clMenu,
            title = { Text(lw("Add photo"), color = palette.clText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showSourceMenu = false
                            launchCamera()
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
                TextButton(onClick = { showSourceMenu = false }) {
                    Text(lw("Cancel"), color = palette.clText)
                }
            }
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = palette.clMenu,
            title = { Text(lw("Delete photo?"), color = palette.clText) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { state.delete(entry) }
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium
                ) { Text(lw("Delete")) }
            },
            dismissButton = {
                Button(
                    onClick = { pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium
                ) { Text(lw("Cancel")) }
            }
        )
    }

    limitMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { limitMessage = null },
            containerColor = palette.clMenu,
            title = { Text(lw("Photos"), color = palette.clText) },
            text = { Text(msg, color = palette.clText) },
            confirmButton = {
                TextButton(onClick = { limitMessage = null }) {
                    Text("OK", color = palette.clText)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumb(
    entry: PhotoEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    var bitmap by remember(entry.path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(entry.path) { mutableStateOf(false) }

    LaunchedEffect(entry.path) {
        bitmap = PhotoBitmapLoader.load(entry.path, targetPx = 240)
        failed = bitmap == null
    }

    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.clFill)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
            failed -> Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = palette.clText.copy(alpha = 0.6f)
            )
            else -> Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(palette.clFill)
            )
        }
    }
}
