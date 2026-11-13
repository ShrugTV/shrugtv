package com.shrug.shrugtv.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface VodInfoDao {

    @Query("SELECT * FROM vod_info WHERE broadcast_id = :broadcastId")
    VodInfo findByBroadcastId(long broadcastId);

    @Query("UPDATE vod_info SET last_access = NULL WHERE last_access IS NOT NULL")
    void clearHistory();

    @Insert
    void insert(VodInfo vodInfo);

    @Update
    void update(VodInfo vodInfo);

    @Query("SELECT * FROM vod_info WHERE last_access IS NOT NULL AND channel_name_english IS NOT NULL ORDER BY last_access DESC")
    List<VodInfo> getHistory();

    @Delete
    void delete(VodInfo vodInfo);

    @Query("SELECT * FROM vod_info")
    List<VodInfo> getAll();
}
