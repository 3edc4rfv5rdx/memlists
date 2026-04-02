package x.x.memlists.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemListsDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT,
                tags TEXT,
                priority INTEGER NOT NULL DEFAULT 0,
                created INTEGER NOT NULL,
                hidden INTEGER NOT NULL DEFAULT 0,
                reminder_type INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                date INTEGER,
                time INTEGER,
                times TEXT,
                date_to INTEGER,
                days_mask INTEGER,
                sound TEXT,
                fullscreen INTEGER NOT NULL DEFAULT 0,
                loop_sound INTEGER NOT NULL DEFAULT 1,
                yearly INTEGER NOT NULL DEFAULT 0,
                monthly INTEGER NOT NULL DEFAULT 0,
                remove INTEGER NOT NULL DEFAULT 0,
                period_done_until INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE settings (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_type TEXT NOT NULL,
                owner_id INTEGER NOT NULL,
                path TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE lists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                comment TEXT,
                parent_id INTEGER REFERENCES lists(id) ON DELETE SET NULL,
                is_folder INTEGER NOT NULL DEFAULT 0,
                pin TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE dictionary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                unit TEXT,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                list_id INTEGER NOT NULL REFERENCES lists(id) ON DELETE CASCADE,
                dict_id INTEGER REFERENCES dictionary(id) ON DELETE SET NULL,
                name TEXT,
                unit TEXT,
                quantity TEXT,
                is_checked INTEGER NOT NULL DEFAULT 0,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == newVersion) return
    }

    companion object {
        private const val DATABASE_NAME = "memlists.db"
        private const val DATABASE_VERSION = 1
    }
}

