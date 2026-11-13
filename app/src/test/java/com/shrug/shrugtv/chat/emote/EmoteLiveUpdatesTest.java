package com.shrug.shrugtv.chat.emote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.ref.WeakReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Live updates of the channel emotes (tests #36): 7TV dispatches and BTTV messages
 * im Format laut github.com/SevenTV/EventAPI bzw. Chatterino (BttvLiveUpdateMessages.cpp,
 * eventapi/Dispatch.cpp) change the channel emotes of the EmoteManager.
 */
public class EmoteLiveUpdatesTest {
    private static final int CHANNEL = 4711;
    private EmoteManager emotes;
    private EmoteLiveUpdates live;

    static JsonObject json(String s) {
        return new JsonParser().parse(s).getAsJsonObject();
    }

    static String sevenTvEmote(String id, String name, boolean animated) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"flags\":0,\"data\":{\"id\":\"" + id + "\",\"name\":\"" + name
                + "\",\"animated\":" + animated + ",\"owner\":{\"id\":\"o\"},\"host\":{\"url\":\"//cdn.7tv.app/emote/" + id
                + "\",\"files\":[{\"name\":\"1x.avif\",\"format\":\"AVIF\"},{\"name\":\"1x.webp\",\"format\":\"WEBP\"}]}}}";
    }

    @Before
    public void up() {
        emotes = new EmoteManager(CHANNEL, "test", new WeakReference<>(null), 28, 28, true);
        live = new EmoteLiveUpdates(emotes, CHANNEL);
    }

    @After
    public void from() {
        live.stop();
    }

    @Test
    public void sevenTvNewRenamedRemoved() {
        live.sevenTvDispatch(json("{\"type\":\"emote_set.update\",\"body\":{\"id\":\"set1\",\"kind\":3,"
                + "\"pushed\":[{\"key\":\"emotes\",\"index\":12,\"value\":" + sevenTvEmote("e1", "PogNeu", true) + "}]}}"));
        Emote e = emotes.channelEmote("PogNeu");
        assertNotNull(e);
        assertEquals("https://cdn.7tv.app/emote/e1/1x.webp", e.getUrl());
        assertTrue(e.isAnimated());

        live.sevenTvDispatch(json("{\"type\":\"emote_set.update\",\"body\":{\"id\":\"set1\",\"kind\":3,"
                + "\"updated\":[{\"key\":\"emotes\",\"index\":12,\"old_value\":" + sevenTvEmote("e1", "PogNeu", true)
                + ",\"value\":" + sevenTvEmote("e1", "PogUmbenannt", true) + "}]}}"));
        assertNull(emotes.channelEmote("PogNeu"));
        assertNotNull(emotes.channelEmote("PogUmbenannt"));

        live.sevenTvDispatch(json("{\"type\":\"emote_set.update\",\"body\":{\"id\":\"set1\",\"kind\":3,"
                + "\"pulled\":[{\"key\":\"emotes\",\"index\":12,\"old_value\":" + sevenTvEmote("e1", "PogUmbenannt", true) + "}]}}"));
        assertNull(emotes.channelEmote("PogUmbenannt"));
        assertEquals(0, emotes.getChannelEmoteCount());
    }

    /** Real dispatch, recorded from the 7TV EventAPI on 2026-10-27 (channel rewinside, emote "Howdy"). */
    @Test
    public void sevenTvRealRecording() throws java.io.IOException {
        String text;
        try (java.io.InputStream in = getClass().getResourceAsStream("/emotes/7tv_hinzu_echt.json")) {
            text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        live.sevenTvDispatch(json(text));
        Emote e = emotes.channelEmote("Howdy");
        assertNotNull(e);
        assertTrue(e.getUrl(), e.getUrl().startsWith("https://cdn.7tv.app/emote/01M5ZEF72ZZ13QVTJ5Z5V358XQ/") && e.getUrl().endsWith(".webp"));
    }

    @Test
    public void sevenTvOtherKeysDoNotCount() {
        live.sevenTvDispatch(json("{\"type\":\"emote_set.update\",\"body\":{\"id\":\"set1\",\"kind\":3,"
                + "\"updated\":[{\"key\":\"name\",\"old_value\":\"Old\",\"value\":\"New\"}]}}"));
        assertEquals(0, emotes.getChannelEmoteCount());
    }

    @Test
    public void bttvCreateUpdateDelete() {
        live.bttvMessage(json("{\"name\":\"emote_create\",\"data\":{\"channel\":\"twitch:" + CHANNEL
                + "\",\"emote\":{\"id\":\"b1\",\"code\":\"catJAM2\",\"imageType\":\"gif\",\"animated\":true}}}"));
        Emote e = emotes.channelEmote("catJAM2");
        assertNotNull(e);
        assertTrue(e.isGif());

        // emote_update with a new code, without imageType: old name gone, image type stays
        live.bttvMessage(json("{\"name\":\"emote_update\",\"data\":{\"channel\":\"twitch:" + CHANNEL
                + "\",\"emote\":{\"id\":\"b1\",\"code\":\"catJAM3\"}}}"));
        assertNull(emotes.channelEmote("catJAM2"));
        assertTrue(emotes.channelEmote("catJAM3").isGif());

        live.bttvMessage(json("{\"name\":\"emote_delete\",\"data\":{\"channel\":\"twitch:" + CHANNEL + "\",\"emoteId\":\"b1\"}}"));
        assertNull(emotes.channelEmote("catJAM3"));
    }

    @Test
    public void bttvForeignChannelIsIgnored() {
        live.bttvMessage(json("{\"name\":\"emote_create\",\"data\":{\"channel\":\"twitch:1\","
                + "\"emote\":{\"id\":\"b9\",\"code\":\"Fremd\",\"imageType\":\"png\"}}}"));
        assertNull(emotes.channelEmote("Fremd"));
    }
}
