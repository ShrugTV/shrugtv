package com.shrug.shrugtv.player

import android.app.Activity
import android.util.Log
import android.view.Display
import androidx.preference.PreferenceManager

/**
 * Match the display refresh rate to the stream, switchable in Stream settings
 * ("pref_match_frame_rate", off by default: the TV goes black for 1-2 s when it switches).
 *
 * A 50 fps stream on a 60 Hz output shows every fifth frame twice, pans judder. Measured 2026-10-29:
 * 2 of the 60 biggest German streams send 50 fps (the other 52 send 60, 5 send 30), none of the
 * 60 biggest overall. Fire OS 8 is Android 11, where Surface.setFrameRate cannot force a mode
 * change; the reliable way is the window's preferredDisplayModeId, also used by Kodi and recommended
 * for Fire TV. Only modes with the current resolution are considered, so only the refresh rate changes.
 */
object FrameRateMatch {
    const val PREF_KEY = "pref_match_frame_rate"
    private const val TAG = "Playback"

    data class Mode(val id: Int, val width: Int, val height: Int, val refreshRate: Float)

    @JvmStatic
    fun enabled(activity: Activity): Boolean =
        PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PREF_KEY, false)

    /**
     * Best mode for [fps]: same resolution as [current], refresh rate an integer multiple of the
     * frame rate (50 fps -> 50/100 Hz, 60 -> 60/120, 30 -> 60 also fine). Prefers the lowest such
     * rate. Returns null if [current] already fits or nothing fits.
     */
    @JvmStatic
    fun choose(modes: List<Mode>, current: Mode, fps: Float): Mode? {
        if (fps <= 0f) return null
        fun fits(hz: Float): Boolean {
            val ratio = hz / fps
            return ratio >= 0.99f && Math.abs(ratio - Math.round(ratio)) < 0.01f
        }
        if (fits(current.refreshRate)) return null
        return modes
            .filter { it.width == current.width && it.height == current.height && fits(it.refreshRate) }
            .minByOrNull { it.refreshRate }
    }

    /** Applies the best mode for [fps] to the activity's window; no-op when off or nothing fits. */
    @JvmStatic
    fun apply(activity: Activity, fps: Float) {
        if (!enabled(activity)) return
        val display: Display = activity.window.decorView.display ?: return
        val current = display.mode.let { Mode(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate) }
        val modes = display.supportedModes.map { Mode(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate) }
        val target = choose(modes, current, fps)
        Log.i(TAG, "frame rate %.3f fps, display %.2f Hz -> %s".format(fps, current.refreshRate, target?.let { "%.2f Hz".format(it.refreshRate) } ?: "unchanged"))
        if (target == null) return
        val attrs = activity.window.attributes
        attrs.preferredDisplayModeId = target.id
        activity.window.attributes = attrs
    }

    /** Back to the system's choice when leaving the player. */
    @JvmStatic
    fun reset(activity: Activity) {
        val attrs = activity.window.attributes
        if (attrs.preferredDisplayModeId != 0) {
            attrs.preferredDisplayModeId = 0
            activity.window.attributes = attrs
        }
    }
}
