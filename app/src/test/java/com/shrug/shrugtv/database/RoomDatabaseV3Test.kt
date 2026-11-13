package com.shrug.shrugtv.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class RoomDatabaseV3Test {

    companion object {
        private const val EXPECTED_IDENTITY_HASH = "dd460f1e22be79f1ead2e1fc14bef293"
    }

    @Test
    fun testRealV3DatabaseSchemaAndIdentityHash() {
        val resource = javaClass.getResource("/database/database_v3.db")
        assertNotNull("Real v3 database file must exist in test resources", resource)
        val dbFile = File(resource!!.toURI())
        assertTrue("Database file must exist on disk", dbFile.exists())

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                // 1. Verify Room identity hash matches the compiled Room database hash
                val hashRs = stmt.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42")
                assertTrue("room_master_table must contain row 42", hashRs.next())
                val actualHash = hashRs.getString("identity_hash")
                assertEquals("Identity hash must match room_master_table exactly", EXPECTED_IDENTITY_HASH, actualHash)

                // 2. Verify vod_info columns and types
                val vodCols = mutableMapOf<String, String>()
                val colRs = stmt.executeQuery("PRAGMA table_info(vod_info)")
                while (colRs.next()) {
                    vodCols[colRs.getString("name")] = colRs.getString("type")
                }
                assertTrue("Must contain vod_legth column (preserving original typo)", vodCols.containsKey("vod_legth"))
                assertEquals("INTEGER", vodCols["vod_legth"])
                assertTrue("Must contain channel_name_english column (from migration 2->3)", vodCols.containsKey("channel_name_english"))
                assertTrue("Must contain broadcast_id", vodCols.containsKey("broadcast_id"))
                assertTrue("Must contain position", vodCols.containsKey("position"))
                assertTrue("Must contain last_access", vodCols.containsKey("last_access"))

                // 3. Verify favorite_info columns
                val favCols = mutableMapOf<String, String>()
                val favRs = stmt.executeQuery("PRAGMA table_info(favorite_info)")
                while (favRs.next()) {
                    favCols[favRs.getString("name")] = favRs.getString("type")
                }
                assertTrue("Must contain channel_id", favCols.containsKey("channel_id"))

                // 4. Verify existing data can be queried cleanly
                val rowRs = stmt.executeQuery("SELECT broadcast_id, channel_name, vod_legth, position FROM vod_info")
                assertTrue("Real v3 DB contains sample watch record", rowRs.next())
                assertEquals(2904525110L, rowRs.getLong("broadcast_id"))
                assertEquals("ahmpy", rowRs.getString("channel_name"))
                assertEquals(12281, rowRs.getInt("vod_legth"))
                assertEquals(176, rowRs.getInt("position"))
            }
        }
    }

    @Test
    fun testMigration1To2SqlCompatibility() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { stmt ->
                // Create v1 schema
                stmt.execute(
                    "CREATE TABLE `vod_info` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`broadcast_id` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL" +
                        ")"
                )

                // Apply statements matching AppDatabase.MIGRATION_1_2
                stmt.execute("ALTER TABLE vod_info ADD COLUMN last_access TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN published_date TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN video_thumbnail TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN logo_image TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN vod_legth INTEGER NOT NULL DEFAULT 0;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN channel_name TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN category_name TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN title TEXT;")
                stmt.execute("ALTER TABLE vod_info ADD COLUMN channel_id INTEGER NOT NULL DEFAULT 0;")

                // Insert and verify default values
                stmt.execute("INSERT INTO vod_info (broadcast_id, position) VALUES (12345, 60)")
                val rs = stmt.executeQuery("SELECT vod_legth, channel_id, position FROM vod_info WHERE broadcast_id = 12345")
                assertTrue(rs.next())
                assertEquals(0, rs.getInt("vod_legth"))
                assertEquals(0, rs.getInt("channel_id"))
                assertEquals(60, rs.getInt("position"))
            }
        }
    }

    @Test
    fun testMigration2To3SqlCompatibility() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { stmt ->
                // Create v2 schema
                stmt.execute(
                    "CREATE TABLE `vod_info` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`broadcast_id` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`last_access` TEXT, " +
                        "`published_date` TEXT, " +
                        "`video_thumbnail` TEXT, " +
                        "`logo_image` TEXT, " +
                        "`vod_legth` INTEGER NOT NULL DEFAULT 0, " +
                        "`channel_name` TEXT, " +
                        "`category_name` TEXT, " +
                        "`title` TEXT, " +
                        "`channel_id` INTEGER NOT NULL DEFAULT 0" +
                        ")"
                )

                // Apply AppDatabase.MIGRATION_2_3
                stmt.execute("ALTER TABLE vod_info ADD COLUMN channel_name_english TEXT;")

                stmt.execute("INSERT INTO vod_info (broadcast_id, position, channel_name_english) VALUES (999, 120, 'streamer_en')")
                val rs = stmt.executeQuery("SELECT channel_name_english FROM vod_info WHERE broadcast_id = 999")
                assertTrue(rs.next())
                assertEquals("streamer_en", rs.getString("channel_name_english"))
            }
        }
    }
}
