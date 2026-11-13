package com.shrug.shrugtv.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.shrug.shrugtv.util.TimeUtil;
import java.time.ZonedDateTime;

@Entity(tableName = "vod_info")
public class VodInfo {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "broadcast_id")
    private long broadcastId;

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "last_access")
    private String lastAccess;

    @ColumnInfo(name = "published_date")
    private String publishedDate;

    @ColumnInfo(name = "video_thumbnail")
    private String videoThumbnail;

    @ColumnInfo(name = "logo_image")
    private String logoImage;

    // Deliberate historical spelling ("vod_legth") preserved to match Room schema v3.
    @ColumnInfo(name = "vod_legth")
    private int vodLength;

    @ColumnInfo(name = "channel_name")
    private String channelName;

    @ColumnInfo(name = "category_name")
    private String categoryName;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "channel_id")
    private int channelId;

    @ColumnInfo(name = "channel_name_english")
    private String channelNameEnglish;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(long broadcastId) {
        this.broadcastId = broadcastId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPositionText() {
        return position == 0 ? "N/A" : TimeUtil.hms(position);
    }

    public String getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(String lastAccess) {
        this.lastAccess = lastAccess;
    }

    public ZonedDateTime getLastAccessDateTime() {
        return TimeUtil.parseIso(lastAccess);
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getVideoThumbnail() {
        return videoThumbnail;
    }

    public void setVideoThumbnail(String videoThumbnail) {
        this.videoThumbnail = videoThumbnail;
    }

    public String getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }

    public int getVodLength() {
        return vodLength;
    }

    public void setVodLength(int vodLength) {
        this.vodLength = vodLength;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getChannelNameEnglish() {
        return channelNameEnglish;
    }

    public void setChannelNameEnglish(String channelNameEnglish) {
        this.channelNameEnglish = channelNameEnglish;
    }

    public boolean isWatchedToEnd() {
        return vodLength == position;
    }
}
