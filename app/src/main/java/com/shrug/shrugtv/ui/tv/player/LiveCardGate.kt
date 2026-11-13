package com.shrug.shrugtv.ui.tv.player

/**
 * When the player shows the "X is live" card: not for the channel that is
 * playing, not while another card (raid, next channel, an earlier live card) is up, and at most one
 * card per [MIN_GAP_MS] so the picture stays calm on evenings when many channels start.
 */
object LiveCardGate {
    const val MIN_GAP_MS = 120_000L

    /** How long the card waits for OK. */
    const val SHOW_SECONDS = 10

    @JvmStatic
    fun shouldShow(nowMs: Long, lastShownMs: Long, channelId: Int, currentChannelId: Int, otherCardUp: Boolean): Boolean =
        !otherCardUp &&
            channelId > 0 &&
            channelId != currentChannelId &&
            (lastShownMs <= 0L || nowMs - lastShownMs >= MIN_GAP_MS)
}
