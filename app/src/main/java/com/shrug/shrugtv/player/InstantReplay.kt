package com.shrug.shrugtv.player

/**
 * Instant replay in a live stream: the player jumps [lengthMs] back, and once that much has
 * actually played again (pauses, buffering and loading the recording do not count) it returns to the live edge.
 * Pure: the caller feeds [tick] regularly with the time passed and whether the picture runs.
 */
class InstantReplay(val lengthMs: Long) {
    private var played = 0L

    /** True once the replayed part has played. */
    fun tick(deltaMs: Long, playing: Boolean): Boolean {
        if (playing && deltaMs > 0) played += deltaMs
        return played >= lengthMs
    }

    companion object {
        const val PREF_ENABLED = "pref_player_replay"
        const val PREF_SECONDS = "pref_player_replay_seconds"

        /** Length from the setting in seconds; 30 s if it is missing or odd, 5 to 120 s. */
        fun lengthMs(seconds: String?): Long = ((seconds?.toLongOrNull() ?: 30L).coerceIn(5L, 120L)) * 1000L
    }
}
