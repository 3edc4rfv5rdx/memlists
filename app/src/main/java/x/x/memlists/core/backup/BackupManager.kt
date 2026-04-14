package x.x.memlists.core.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import x.x.memlists.MemListsApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInfo(
    val dir: File,
    val dayFolderName: String,
    val timeFolderName: String,
    val timestamp: Long,
    val label: String
)

object BackupManager {

    private const val TAG = "MemListsBackup"
    private const val DB_NAME = "memlists.db"
    private const val PHOTO_DIR = "photo"
    private const val SOUNDS_DIR = "Sounds"
    private const val ROOT_DIR = "Memlists"

    private fun backupRoot(): File {
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documents, ROOT_DIR)
    }

    fun createBackup(context: Context): Result<File> = runCatching {
        val app = context.applicationContext as MemListsApplication
        val now = Date()
        val dayName = "bak-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val timeName = "x" + SimpleDateFormat("HHmmss", Locale.US).format(now)
        val targetDir = File(backupRoot(), "$dayName/$timeName")
        if (!targetDir.mkdirs() && !targetDir.isDirectory) {
            error("Cannot create backup dir: ${targetDir.absolutePath}")
        }

        walCheckpoint(app)

        val dbSource = context.getDatabasePath(DB_NAME)
        if (dbSource.exists()) {
            dbSource.copyTo(File(targetDir, DB_NAME), overwrite = true)
        }

        val photoSource = File(context.filesDir, PHOTO_DIR)
        if (photoSource.isDirectory) {
            photoSource.copyRecursively(File(targetDir, PHOTO_DIR), overwrite = true)
        }

        val soundsSource = File(context.filesDir, SOUNDS_DIR)
        if (soundsSource.isDirectory) {
            soundsSource.copyRecursively(File(targetDir, SOUNDS_DIR), overwrite = true)
        }

        Log.d(TAG, "Backup created: ${targetDir.absolutePath}")
        targetDir
    }

    fun listBackups(): List<BackupInfo> {
        val root = backupRoot()
        if (!root.isDirectory) return emptyList()
        val out = mutableListOf<BackupInfo>()
        root.listFiles { f -> f.isDirectory && f.name.startsWith("bak-") }?.forEach { dayDir ->
            dayDir.listFiles { f -> f.isDirectory && f.name.startsWith("x") }?.forEach { timeDir ->
                val ts = parseTimestamp(dayDir.name, timeDir.name) ?: timeDir.lastModified()
                out += BackupInfo(
                    dir = timeDir,
                    dayFolderName = dayDir.name,
                    timeFolderName = timeDir.name,
                    timestamp = ts,
                    label = formatLabel(dayDir.name, timeDir.name)
                )
            }
        }
        return out.sortedByDescending { it.timestamp }
    }

    fun restoreBackup(context: Context, backupDir: File): Result<Unit> = runCatching {
        val app = context.applicationContext as MemListsApplication
        val dbSource = File(backupDir, DB_NAME)
        require(dbSource.exists()) { "Backup db missing: ${dbSource.absolutePath}" }

        app.databaseHelper.close()

        val dbDest = context.getDatabasePath(DB_NAME)
        dbDest.parentFile?.mkdirs()
        listOf(dbDest, File(dbDest.absolutePath + "-wal"), File(dbDest.absolutePath + "-shm")).forEach {
            if (it.exists()) it.delete()
        }
        dbSource.copyTo(dbDest, overwrite = true)

        val photoSource = File(backupDir, PHOTO_DIR)
        val photoDest = File(context.filesDir, PHOTO_DIR)
        if (photoDest.exists()) photoDest.deleteRecursively()
        if (photoSource.isDirectory) photoSource.copyRecursively(photoDest, overwrite = true)

        val soundsSource = File(backupDir, SOUNDS_DIR)
        val soundsDest = File(context.filesDir, SOUNDS_DIR)
        if (soundsDest.exists()) soundsDest.deleteRecursively()
        if (soundsSource.isDirectory) soundsSource.copyRecursively(soundsDest, overwrite = true)

        Log.d(TAG, "Restore complete from: ${backupDir.absolutePath}")
    }

    private fun walCheckpoint(app: MemListsApplication) {
        try {
            app.databaseHelper.writableDatabase.rawQuery(
                "PRAGMA wal_checkpoint(TRUNCATE)", null
            ).use { it.moveToFirst() }
        } catch (e: Exception) {
            Log.w(TAG, "WAL checkpoint failed: ${e.message}")
        }
    }

    private fun parseTimestamp(dayName: String, timeName: String): Long? = try {
        val day = dayName.removePrefix("bak-")
        val time = timeName.removePrefix("x")
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US).parse(day + time)?.time
    } catch (_: Exception) { null }

    private fun formatLabel(dayName: String, timeName: String): String {
        val day = dayName.removePrefix("bak-")
        val time = timeName.removePrefix("x")
        val date = if (day.length == 8) "${day.substring(0, 4)}-${day.substring(4, 6)}-${day.substring(6, 8)}" else day
        val clock = if (time.length == 6) "${time.substring(0, 2)}:${time.substring(2, 4)}:${time.substring(4, 6)}" else time
        return "$date $clock"
    }
}
