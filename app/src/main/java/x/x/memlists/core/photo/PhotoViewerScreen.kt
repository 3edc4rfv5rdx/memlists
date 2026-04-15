package x.x.memlists.core.photo

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PhotoViewerOverlay(
    entries: List<PhotoEntry>,
    initialIndex: Int,
    lw: (String) -> String,
    onClose: () -> Unit,
    onDelete: (PhotoEntry) -> Unit
) {
    if (entries.isEmpty()) {
        onClose()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startIndex = initialIndex.coerceIn(0, entries.size - 1)
    val pagerState = rememberPagerState(initialPage = startIndex) { entries.size }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    LaunchedEffect(entries.size) {
        if (pagerState.currentPage >= entries.size && entries.isNotEmpty()) {
            pagerState.scrollToPage(entries.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            FullscreenPhoto(entries[page])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = lw("Close"), tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${pagerState.currentPage + 1} / ${entries.size}",
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val entry = entries.getOrNull(pagerState.currentPage) ?: return@IconButton
                sharePhoto(context, entry)
            }) {
                Icon(Icons.Default.Share, contentDescription = lw("Share"), tint = Color.White)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = lw("Delete"), tint = Color.White)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(lw("Delete photo?")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val entry = entries.getOrNull(pagerState.currentPage) ?: return@TextButton
                    scope.launch { onDelete(entry) }
                }) { Text(lw("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(lw("Cancel")) }
            }
        )
    }
}

@Composable
private fun FullscreenPhoto(entry: PhotoEntry) {
    var bitmap by remember(entry.path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(entry.path) {
        bitmap = PhotoBitmapLoader.load(entry.path, targetPx = 1600)
    }
    var scale by remember(entry.path) { mutableStateOf(1f) }
    var offsetX by remember(entry.path) { mutableStateOf(0f) }
    var offsetY by remember(entry.path) { mutableStateOf(0f) }

    var lastTapTime by remember(entry.path) { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(entry.path) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var movedBeyondSlop = false
                    var wasMultiTouch = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointers = event.changes.count { it.pressed }
                        val multiTouch = pointers >= 2
                        if (multiTouch) wasMultiTouch = true
                        if (!movedBeyondSlop) {
                            val current = event.changes.firstOrNull { it.pressed }?.position
                            if (current != null) {
                                val dx = current.x - downPos.x
                                val dy = current.y - downPos.y
                                if (kotlin.math.hypot(dx, dy) > slop) movedBeyondSlop = true
                            }
                        }
                        if (multiTouch || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f; offsetY = 0f
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!movedBeyondSlop && !wasMultiTouch) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300L) {
                            if (scale > 1f) {
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            } else {
                                scale = 2f
                            }
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

private fun sharePhoto(context: android.content.Context, entry: PhotoEntry) {
    val file = File(entry.path).takeIf { it.exists() } ?: return
    val uri = PhotoStorage.contentUriFor(context, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
