package com.shrug.shrugtv.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteDao {

    @Query("SELECT channel_id FROM favorite_info")
    List<String> getAllChannelIds();

    @Query("DELETE FROM favorite_info WHERE channel_id = :channelId")
    void deleteByChannelId(String channelId);

    @Query("SELECT * FROM favorite_info WHERE channel_id = :channelId")
    List<FavoriteInfo> findByChannelId(String channelId);

    @Insert
    void insert(FavoriteInfo favoriteInfo);
}
