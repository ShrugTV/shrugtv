package com.shrug.shrugtv.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.shrug.shrugtv.telemetry.CrashReporter;

/**
 * The watch statistics. A file of their own on purpose: the history of what
 * somebody watches is more personal than the watch progress of {@link AppDatabase}, and a single table
 * cannot be left out of a backup, a file can (see backup_rules.xml). This also keeps the identity
 * hash and the migrations of the main database untouched.
 */
@Database(entities = {WatchSession.class}, version = 2, exportSchema = false)
public abstract class WatchStatsDatabase extends RoomDatabase {

    /** File name in the databases folder; the backup rules name it. */
    public static final String FILE = "watch_stats";

    /** Version 2: clips are marked, older rows are no clips. */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE watch_session ADD COLUMN is_clip INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static volatile WatchStatsDatabase instance;

    public static synchronized WatchStatsDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                instance = Room.databaseBuilder(context.getApplicationContext(), WatchStatsDatabase.class, FILE)
                        .addMigrations(MIGRATION_1_2)
                        .build();
            } catch (Exception e) {
                CrashReporter.recordException(e);
            }
        }
        return instance;
    }

    public abstract WatchSessionDao watchSessionDao();
}
