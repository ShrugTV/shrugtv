package com.shrug.shrugtv.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.shrug.shrugtv.telemetry.CrashReporter;

@Database(
    entities = {VodInfo.class, FavoriteInfo.class},
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE vod_info ADD COLUMN last_access TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN published_date TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN video_thumbnail TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN logo_image TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN vod_legth INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN channel_name TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN category_name TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN title TEXT;");
            db.execSQL("ALTER TABLE vod_info ADD COLUMN channel_id INTEGER NOT NULL DEFAULT 0;");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE vod_info ADD COLUMN channel_name_english TEXT;");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
            } catch (Exception e) {
                CrashReporter.recordException(e);
            }
        }
        return instance;
    }

    public abstract FavoriteDao favoriteChannelDao();

    public abstract VodInfoDao vodInfoDao();
}
