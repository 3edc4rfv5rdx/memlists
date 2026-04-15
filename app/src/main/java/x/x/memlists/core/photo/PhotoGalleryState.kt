package x.x.memlists.core.photo

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File

data class PhotoEntry(
    val dbId: Long?,
    val path: String
)

class PhotoGalleryState(
    val context: Context,
    val repository: PhotoRepository,
    val ownerType: PhotoOwnerType,
    initialOwnerId: Long?
) {
    var ownerId: Long? by mutableStateOf(initialOwnerId)
    val tempId: Long = PhotoStorage.newTempId()

    val committed = mutableStateListOf<PhotoEntry>()
    val pending = mutableStateListOf<String>()

    val entries: List<PhotoEntry>
        get() = committed + pending.map { PhotoEntry(null, it) }

    val count: Int get() = committed.size + pending.size

    suspend fun reloadCommitted() {
        val id = ownerId ?: return
        val rows = repository.list(ownerType, id)
        committed.clear()
        committed.addAll(rows.map { PhotoEntry(dbId = it.id, path = it.path) })
    }

    fun newCaptureFile(): File {
        val id = ownerId
        val dir = if (id != null) {
            PhotoStorage.ownerDir(context, ownerType, id)
        } else {
            PhotoStorage.tempDir(context, tempId)
        }
        return PhotoStorage.newPhotoFile(dir)
    }

    suspend fun attachCaptured(file: File) {
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return
        }
        val id = ownerId
        if (id != null) {
            val dbId = repository.insert(ownerType, id, file.absolutePath)
            committed.add(PhotoEntry(dbId, file.absolutePath))
        } else {
            pending.add(file.absolutePath)
        }
    }

    suspend fun importFromUri(uri: Uri) {
        val id = ownerId
        val destDir = if (id != null) {
            PhotoStorage.ownerDir(context, ownerType, id)
        } else {
            PhotoStorage.tempDir(context, tempId)
        }
        val dest = PhotoStorage.newPhotoFile(destDir)
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!ok) {
            dest.delete()
            return
        }
        if (id != null) {
            val dbId = repository.insert(ownerType, id, dest.absolutePath)
            committed.add(PhotoEntry(dbId, dest.absolutePath))
        } else {
            pending.add(dest.absolutePath)
        }
    }

    suspend fun delete(entry: PhotoEntry) {
        if (entry.dbId != null) {
            repository.delete(entry.dbId)
            committed.removeAll { it.dbId == entry.dbId }
        } else {
            runCatching { File(entry.path).delete() }
            pending.remove(entry.path)
        }
    }

    suspend fun commitPending(newOwnerId: Long) {
        if (pending.isEmpty()) {
            ownerId = newOwnerId
            return
        }
        repository.commitTempToOwner(ownerType, newOwnerId, tempId, pending.toList())
        pending.clear()
        ownerId = newOwnerId
        reloadCommitted()
    }

    fun discardPending() {
        pending.forEach { runCatching { File(it).delete() } }
        pending.clear()
        PhotoStorage.tempDir(context, tempId).deleteRecursively()
    }
}

@Composable
fun rememberPhotoGalleryState(
    context: Context,
    repository: PhotoRepository,
    ownerType: PhotoOwnerType,
    initialOwnerId: Long?
): PhotoGalleryState = remember(ownerType, initialOwnerId) {
    PhotoGalleryState(context, repository, ownerType, initialOwnerId)
}
