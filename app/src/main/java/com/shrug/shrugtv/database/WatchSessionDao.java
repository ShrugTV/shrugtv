package com.shrug.shrugtv.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface WatchSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WatchSession session);

    @Query("SELECT * FROM watch_session WHERE day >= :from AND day <= :to ORDER BY id")
    List<WatchSession> between(String from, String to);

    @Query("SELECT MIN(day) FROM watch_session")
    String oldestDay();

    @Query("DELETE FROM watch_session WHERE day < :before")
    int deleteBefore(String before);

    @Query("DELETE FROM watch_session")
    void deleteAll();
}
