package x.x.memlists.core.photo

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import x.x.memlists.core.data.MemListsDatabaseHelper
import java.io.File

class PhotoRepository(
    private val context: Context,
    private val databaseHelper: MemListsDatabaseHelper
) {

    suspend fun list(
        ownerType: PhotoOwnerType,
        ownerId: Long
    ): List<PhotoRef> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PhotoRef>()
        databaseHelper.readableDatabase.query(
            "photos",
            arrayOf("id", "owner_type", "owner_id", "path", "sort_order"),
            "owner_type = ? AND owner_id = ?",
            arrayOf(ownerType.dbValue, ownerId.toString()),
            null, null,
            "sort_order ASC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += PhotoRef(
                    id = cursor.getLong(0),
                    ownerType = PhotoOwnerType.fromDb(cursor.getString(1)),
                    ownerId = cursor.getLong(2),
                    path = cursor.getString(3),
                    sortOrder = cursor.getInt(4)
                )
            }
        }
        result
    }

    suspend fun count(ownerType: PhotoOwnerType, ownerId: Long): Int = withContext(Dispatchers.IO) {
        databaseHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM photos WHERE owner_type = ? AND owner_id = ?",
            arrayOf(ownerType.dbValue, ownerId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    suspend fun insert(
        ownerType: PhotoOwnerType,
        ownerId: Long,
        path: String
    ): Long = withContext(Dispatchers.IO) {
        val nextSort = count(ownerType, ownerId)
        val values = ContentValues().apply {
            put("owner_type", ownerType.dbValue)
            put("owner_id", ownerId)
            put("path", path)
            put("sort_order", nextSort)
        }
        databaseHelper.writableDatabase.insertOrThrow("photos", null, values)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val path = databaseHelper.readableDatabase.query(
            "photos", arrayOf("path"), "id = ?", arrayOf(id.toString()),
            null, null, null
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

        databaseHelper.writableDatabase.delete("photos", "id = ?", arrayOf(id.toString()))
        path?.let { runCatching { File(it).delete() } }
    }

    suspend fun deleteAllForOwner(
        ownerType: PhotoOwnerType,
        ownerId: Long
    ) = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.delete(
            "photos",
            "owner_type = ? AND owner_id = ?",
            arrayOf(ownerType.dbValue, ownerId.toString())
        )
        PhotoStorage.deleteOwnerDir(context, ownerType, ownerId)
    }

    suspend fun commitTempToOwner(
        ownerType: PhotoOwnerType,
        ownerId: Long,
        tempId: Long,
        tempPaths: List<String>
    ) = withContext(Dispatchers.IO) {
        tempPaths.forEach { tempPath ->
            val finalPath = PhotoStorage.moveTempToOwner(context, tempId, ownerType, ownerId, tempPath)
            insert(ownerType, ownerId, finalPath)
        }
        PhotoStorage.tempDir(context, tempId).deleteRecursively()
    }
}
