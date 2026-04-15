package x.x.memlists.core.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PhotoStorage {

    private const val ROOT = "photo"
    private const val TEMP = "temp"

    fun photosRoot(context: Context): File =
        File(context.filesDir, ROOT).also { it.mkdirs() }

    fun ownerDir(context: Context, ownerType: PhotoOwnerType, ownerId: Long): File =
        File(photosRoot(context), "${ownerType.dbValue}/$ownerId").also { it.mkdirs() }

    fun tempDir(context: Context, tempId: Long): File =
        File(photosRoot(context), "$TEMP/$tempId").also { it.mkdirs() }

    fun newTempId(): Long = System.currentTimeMillis()

    fun newPhotoFile(dir: File): File {
        dir.mkdirs()
        return File(dir, "photo-${System.currentTimeMillis()}.jpg")
    }

    fun contentUriFor(context: Context, file: File): Uri {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun deleteOwnerDir(context: Context, ownerType: PhotoOwnerType, ownerId: Long) {
        ownerDir(context, ownerType, ownerId).deleteRecursively()
    }

    fun cleanupTemp(context: Context) {
        File(photosRoot(context), TEMP).deleteRecursively()
    }

    fun moveTempToOwner(
        context: Context,
        tempId: Long,
        ownerType: PhotoOwnerType,
        ownerId: Long,
        sourcePath: String
    ): String {
        val destDir = ownerDir(context, ownerType, ownerId)
        val src = File(sourcePath)
        val dest = File(destDir, src.name)
        if (src.exists()) {
            src.renameTo(dest) || src.copyAndDelete(dest)
        }
        return dest.absolutePath
    }

    private fun File.copyAndDelete(dest: File): Boolean {
        return try {
            this.copyTo(dest, overwrite = true)
            this.delete()
            true
        } catch (_: Exception) {
            false
        }
    }
}
