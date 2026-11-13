package com.shrug.shrugtv.ui.tv.player

/**
 * Which list the channel switcher shows next when up / down step through the lists.
 * Pure index math so it runs in JVM tests.
 */
object SwitcherNav {
    /** Result of [step]: close the switcher instead of showing another list. */
    const val CLOSE = -1

    /**
     * The list after [index] for one key press: [delta] +1 = up (next list, after the last one the first again),
     * -1 = down (previous list; down on the first list closes the switcher).
     */
    @JvmStatic
    fun step(index: Int, delta: Int, size: Int): Int {
        if (size <= 0) return CLOSE
        if (delta < 0 && index <= 0) return CLOSE
        return Math.floorMod(index + delta, size)
    }

    /** Next candidate while skipping empty lists: keeps the direction and wraps in both ways, never closes. */
    @JvmStatic
    fun skip(index: Int, delta: Int, size: Int): Int = if (size <= 0) 0 else Math.floorMod(index + delta, size)

    /** The lists to show: those that are [isOn]; if none is, "Followed" stays so the switcher is never empty. */
    @JvmStatic
    fun enabled(isOn: (ChannelSwitcherTab) -> Boolean): List<ChannelSwitcherTab> =
        ChannelSwitcherTab.values().filter(isOn).ifEmpty { listOf(ChannelSwitcherTab.FOLLOWED) }
}
