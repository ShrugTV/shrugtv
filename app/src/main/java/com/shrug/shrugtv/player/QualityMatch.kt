package com.shrug.shrugtv.player

/**
 * Maps a quality name from the master playlist ("1080p60 (source)", "720p60", "480p") to one of the
 * video tracks ExoPlayer exposes for that master playlist, so a quality can be chosen with a
 * TrackSelectionOverride instead of reloading the stream (docs/bug_review.md B6).
 * Without Android types, so it is testable on the JVM.
 */
object QualityMatch {

    private val NAME = Regex("(\\d{3,4})p(\\d{2,3})?")

    /**
     * Readable quality name for menus: "1080p60 (source)" -> "1080p60 ([source])", "audio_only" ->
     * [audioOnly]; everything else unchanged. Twitch's raw names stay in the data.
     */
    @JvmStatic
    fun displayName(name: String?, source: String, audioOnly: String): String {
        val n = name ?: return ""
        if (n.equals("audio_only", ignoreCase = true) || n.equals("audio only", ignoreCase = true)) return audioOnly
        return n.replace("(source)", "($source)", ignoreCase = true)
    }

    /** True for Twitch's picture-less variant ("audio_only" / "Audio Only"). */
    @JvmStatic
    fun isAudioOnly(name: String?): Boolean =
        name != null && (name.equals("audio_only", ignoreCase = true) || name.equals("audio only", ignoreCase = true))

    /** Height and frame rate of a quality name; frame rate 30 when the name has none. Null for "Auto", "audio_only" … */
    @JvmStatic
    fun parse(name: String?): Pair<Int, Int>? {
        val m = NAME.find(name ?: return null) ?: return null
        val height = m.groupValues[1].toIntOrNull() ?: return null
        val fps = m.groupValues[2].toIntOrNull() ?: 30
        return height to fps
    }

    /**
     * Index of the track in [tracks] (height, frame rate; frame rate <= 0 = unknown) that matches
     * [wanted]: same height, closest frame rate. -1 if no track has that height.
     */
    @JvmStatic
    fun bestTrack(tracks: List<Pair<Int, Float>>, wanted: Pair<Int, Int>): Int {
        var best = -1
        var bestDiff = Float.MAX_VALUE
        tracks.forEachIndexed { i, (height, frameRate) ->
            if (height != wanted.first) return@forEachIndexed
            val fps = if (frameRate > 0f) frameRate else 30f
            val diff = kotlin.math.abs(fps - wanted.second)
            if (diff < bestDiff) {
                bestDiff = diff
                best = i
            }
        }
        return best
    }
}
