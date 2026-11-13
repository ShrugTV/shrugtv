package com.shrug.shrugtv.stats

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shrug.shrugtv.database.WatchSession
import com.shrug.shrugtv.database.WatchStatsDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The database of the watch statistics and the backup rules that keep it out. */
@RunWith(RobolectricTestRunner::class)
class WatchStatsStoreTest {
    private lateinit var db: WatchStatsDatabase

    @Before
    fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WatchStatsDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun close() = db.close()

    private fun row(id: Long, day: String, sec: Long) = WatchSession().apply {
        this.id = id; channelId = 1; channelLogin = "a"; channelName = "A"; category = "Chess"
        isVod = false; playedSec = sec; this.day = day; hour = 20
    }

    @Test
    fun `a checkpoint replaces the row of the same session`() {
        val dao = db.watchSessionDao()
        dao.upsert(row(1, "2026-11-01", 90))
        dao.upsert(row(1, "2026-11-01", 150))
        val all = dao.between("2026-11-01", "2026-11-01")
        assertEquals(1, all.size)
        assertEquals(150L, all.single().playedSec)
    }

    @Test
    fun `between takes both border days and oldest finds the first day`() {
        val dao = db.watchSessionDao()
        dao.upsert(row(1, "2026-10-31", 10))
        dao.upsert(row(2, "2026-11-01", 20))
        dao.upsert(row(3, "2026-11-04", 30))
        dao.upsert(row(4, "2026-11-05", 40))
        assertEquals(listOf(2L, 3L), dao.between("2026-11-01", "2026-11-04").map { it.id })
        assertEquals("2026-10-31", dao.oldestDay())
    }

    @Test
    fun `retention drops what is older than the day and the delete button empties everything`() {
        val dao = db.watchSessionDao()
        dao.upsert(row(1, "2024-09-30", 10))
        dao.upsert(row(2, "2024-10-01", 20))
        assertEquals(1, dao.deleteBefore("2024-10-01"))
        assertEquals("2024-10-01", dao.oldestDay())
        dao.deleteAll()
        assertNull(dao.oldestDay())
    }

    @Test
    fun `both backup rule files leave the watch statistics out`() {
        for (name in listOf("backup_rules.xml", "data_extraction_rules.xml")) {
            val text = File("src/main/res/xml/$name").readText()
            assertTrue("$name must exclude the database file", text.contains("domain=\"database\" path=\"${WatchStatsDatabase.FILE}\""))
        }
    }
}
