package com.shrug.shrugtv.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One watched stretch; lives in its own database, see {@link WatchStatsDatabase}. */
@Entity(tableName = "watch_session", indices = {@Index("day")})
public class WatchSession {

    /** Epoch ms of the start; a checkpoint of the same session replaces the row. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "channel_id")
    public int channelId;

    @ColumnInfo(name = "channel_login")
    public String channelLogin;

    @ColumnInfo(name = "channel_name")
    public String channelName;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "is_vod")
    public boolean isVod;

    /** A clip; clips are recordings too ({@link #isVod}), but counted on their own. */
    @ColumnInfo(name = "is_clip", defaultValue = "0")
    public boolean isClip;

    @ColumnInfo(name = "played_sec")
    public long playedSec;

    /** Local date of the start, yyyy-MM-dd (sorts as text). */
    @ColumnInfo(name = "day")
    public String day;

    @ColumnInfo(name = "hour")
    public int hour;
}
