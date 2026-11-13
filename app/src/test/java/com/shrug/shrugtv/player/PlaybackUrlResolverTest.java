package com.shrug.shrugtv.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shrug.shrugtv.api.helix.HelixParser;
import com.shrug.shrugtv.model.Enums.RowType;
import com.shrug.shrugtv.model.Movie;
import com.shrug.shrugtv.model.StreamQuality;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Quality list from a real master playlist and clip playback/parsing tests
 *.
 */
public class PlaybackUrlResolverTest {

    static String readFile(String name) throws IOException {
        try (InputStream in = PlaybackUrlResolverTest.class.getResourceAsStream("/playlists/" + name)) {
            if (in == null) throw new IOException("fehlt: " + name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void allLevelsFromTheMaster() throws IOException {
        ArrayList<StreamQuality> q = PlaybackUrlResolver.qualitiesFromMaster(readFile("live_master.m3u8"), "https://master.example/x.m3u8");
        List<String> names = new ArrayList<>();
        for (StreamQuality s : q) names.add(s.getName());
        assertEquals(Arrays.asList("Auto", "1080p60 (source)", "720p60", "480p", "360p", "160p", "audio_only"), names);
        assertEquals("https://master.example/x.m3u8", q.get(0).getUrl());
        for (StreamQuality s : q.subList(1, q.size())) {
            assertTrue(s.getName() + ": " + s.getUrl(), s.getUrl().startsWith("https://") && s.getUrl().contains(".m3u8"));
        }
    }

    @Test
    public void emptyMasterOnlyAuto() {
        assertEquals(1, PlaybackUrlResolver.qualitiesFromMaster("", "https://m").size());
    }

    @Test
    public void clipSlugDetection() {
        assertTrue(PlaybackUrlResolver.isClipSlug("HelplessSlipperyPeanutVoteNay-iDIvZ7YW709zGf34"));
        assertFalse(PlaybackUrlResolver.isClipSlug("2345678901"));
        assertFalse(PlaybackUrlResolver.isClipSlug(""));
        assertFalse(PlaybackUrlResolver.isClipSlug(null));
    }

    @Test
    public void qualitiesFromClipGql() {
        String json = "{"
                + "\"data\":{\"clip\":{"
                + "\"videoQualities\":["
                + "{\"frameRate\":60.0,\"quality\":\"1080\",\"sourceURL\":\"https://cdn.example/1080/index.mp4\"},"
                + "{\"frameRate\":30.0,\"quality\":\"480\",\"sourceURL\":\"https://cdn.example/480/index.mp4\"}"
                + "],"
                + "\"playbackAccessToken\":{\"signature\":\"abc123\",\"value\":\"{\\\"clip_uri\\\":\\\"x\\\"}\"}"
                + "}}}";
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        ArrayList<StreamQuality> q = PlaybackUrlResolver.qualitiesFromClip(root);
        assertEquals(3, q.size());
        assertEquals("Auto", q.get(0).getName());
        assertEquals("1080p60", q.get(1).getName());
        assertEquals("480p", q.get(2).getName());
        assertEquals(q.get(1).getUrl(), q.get(0).getUrl());
        assertTrue(q.get(1).getUrl().startsWith("https://cdn.example/1080/index.mp4?sig=abc123&token="));
    }

    @Test
    public void parseHelixClips() {
        String json = "[{"
                + "\"id\":\"HelplessSlipperyPeanutVoteNay-iDIvZ7YW709zGf34\","
                + "\"broadcaster_id\":\"71092938\","
                + "\"broadcaster_name\":\"xQc\","
                + "\"title\":\"Big play\","
                + "\"view_count\":42100,"
                + "\"created_at\":\"2026-10-25T12:00:00Z\","
                + "\"thumbnail_url\":\"https://static-cdn.jtvnw.net/twitch-video-assets/clip/thumb.jpg\","
                + "\"duration\":29.6"
                + "}]";
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        List<Movie> clips = HelixParser.parseClips(arr, RowType.VOD_CHANNEL_HIGHLIGHTS, "https://avatar.example/x.png");
        assertEquals(1, clips.size());
        Movie m = clips.get(0);
        assertTrue(m.isClip());
        assertEquals("HelplessSlipperyPeanutVoteNay-iDIvZ7YW709zGf34", m.getClipSlug());
        assertEquals("HelplessSlipperyPeanutVoteNay-iDIvZ7YW709zGf34", m.getPlaybackId());
        assertEquals(71092938, m.getUserId());
        assertEquals("xQc", m.getDisplayName());
        assertEquals("Big play", m.getTitle());
        assertEquals(42100, m.getViewCount());
        assertEquals(30, m.getDurationSeconds());
        assertEquals("https://avatar.example/x.png", m.getAvatarUrl());
    }
}
