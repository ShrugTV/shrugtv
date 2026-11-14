package com.shrug.shrugtv.stats

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shrug.shrugtv.database.WatchStatsDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Version 1 of the statistics database gets the column "is_clip" and keeps its rows. */
@RunWith(RobolectricTestRunner::class)
class WatchStatsMigrationTest {

    @Test
    fun `old rows survive and are no clips`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration_test")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                "CREATE TABLE watch_session (id INTEGER NOT NULL, channel_id INTEGER NOT NULL, channel_login TEXT, channel_name TEXT, " +
                    "category TEXT, is_vod INTEGER NOT NULL, played_sec INTEGER NOT NULL, day TEXT, hour INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            old.execSQL("CREATE INDEX index_watch_session_day ON watch_session (day)")
            old.execSQL("INSERT INTO watch_session VALUES (1, 7, 'lirik', 'LIRIK', 'Chess', 1, 3600, '2026-11-02', 20)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, WatchStatsDatabase::class.java, "migration_test")
            .addMigrations(WatchStatsDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val rows = db.watchSessionDao().between("2026-11-01", "2026-11-30")
            assertEquals(1, rows.size)
            assertEquals(3600L, rows.single().playedSec)
            assertFalse(rows.single().isClip)
        } finally {
            db.close()
        }
    }
}
