package x.x.memlists.core.backup

import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.util.Log
import x.x.memlists.MemListsApplication
import java.io.File
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val TAG = "MemListsCsv"
    private const val ROOT_DIR = "Memlists"

    private val TABLES = listOf(
        "items" to "memos-items.csv",
        "lists" to "lists.csv",
        "dictionary" to "dictionary.csv",
        "entries" to "entries.csv"
    )

    fun exportAll(context: Context): Result<File> = runCatching {
        val app = context.applicationContext as MemListsApplication
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val targetDir = File(documents, "$ROOT_DIR/csv-$stamp")
        if (!targetDir.mkdirs() && !targetDir.isDirectory) {
            error("Cannot create csv dir: ${targetDir.absolutePath}")
        }

        val db = app.databaseHelper.readableDatabase
        TABLES.forEach { (table, fileName) ->
            val file = File(targetDir, fileName)
            db.rawQuery("SELECT * FROM $table", null).use { cursor ->
                file.bufferedWriter().use { writer ->
                    writeTable(cursor, writer)
                }
            }
        }

        Log.d(TAG, "CSV export complete: ${targetDir.absolutePath}")
        targetDir
    }

    private fun writeTable(cursor: Cursor, writer: Writer) {
        val columns = cursor.columnNames
        writer.write(columns.joinToString(",") { escape(it) })
        writer.write("\n")
        while (cursor.moveToNext()) {
            val row = (0 until cursor.columnCount).joinToString(",") { idx ->
                escape(readValue(cursor, idx))
            }
            writer.write(row)
            writer.write("\n")
        }
    }

    private fun readValue(cursor: Cursor, idx: Int): String = when (cursor.getType(idx)) {
        Cursor.FIELD_TYPE_NULL -> ""
        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(idx).toString()
        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(idx).toString()
        else -> cursor.getString(idx) ?: ""
    }

    private fun escape(raw: String): String {
        val needsQuote = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuote) return raw
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }
}
