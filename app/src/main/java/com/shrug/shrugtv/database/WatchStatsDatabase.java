package com.shrug.shrugtv.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.shrug.shrugtv.telemetry.CrashReporter;

/**
 * The watch statistics. A file of their own on purpose: the history of what
 * somebody watches is more personal than the watch progress of {@link AppDatabase}, and a single table
 * cannot be left out of a backup, a file can (see backup_rules.xml). This also keeps the identity
 * hash and the migrations of the main database untouched.
 */
@Database(entities = {WatchSession.class}, version = 1, exportSchema = false)
public abstract class WatchStatsDatabase extends RoomDatabase {

    /** File name in the databases folder; the backup rules name it. */
    public static final String FILE = "watch_stats";

    private static volatile WatchStatsDatabase instance;

    public static synchronized WatchStatsDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                instance = Room.databaseBuilder(context.getApplicationContext(), WatchStatsDatabase.class, FILE).build();
            } catch (Exception e) {
                CrashReporter.recordException(e);
            }
        }
        return instance;
    }

    public abstract WatchSessionDao watchSessionDao();
}
