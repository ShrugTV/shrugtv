package com.shrug.shrugtv.player.hls;

import android.net.Uri;

/**
 * Public ("oss") edition: plays Twitch streams exactly as delivered, ads included
 *. Same interface as the homelab edition's ad blocker in
 * {@code src/homelab/java}, so player, parser and resolver stay identical in both editions.
 */
public final class TwitchAdBlocker {

    public static final String LABEL_BLOCKING = "blocking";
    public static final String LABEL_STRIPPING = "stripping";

    /** Receives the ad sign text; never called in this edition. */
    public interface AdStateListener {
        void onAdBlockState(String label);
    }

    private TwitchAdBlocker() {}

    public static void setListener(AdStateListener l) {
        // nothing to report: this edition does not touch ads
    }

    public static void registerMaster(String masterUrl, String channel, String masterText) {
        // nothing to remember
    }

    /** Returns the playlist unchanged. */
    public static String process(Uri uri, String text) {
        return text;
    }
}
