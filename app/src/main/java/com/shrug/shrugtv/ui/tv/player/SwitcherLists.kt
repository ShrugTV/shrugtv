package com.shrug.shrugtv.ui.tv.player

import android.content.Context
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.home.HomeData

/**
 * The channels of one list of the channel switcher, for the switcher on the TV and for the phone page
 *. [gameId] is asked only for the two "same category" lists; 0 means none is known, those lists are empty then.
 */
object SwitcherLists {
    suspend fun load(context: Context, tab: ChannelSwitcherTab, gameId: suspend () -> Long): List<Movie> = when (tab) {
        ChannelSwitcherTab.FOLLOWED -> HomeData.followedLive(context)
        ChannelSwitcherTab.SAME_GAME, ChannelSwitcherTab.SAME_GAME_LANGUAGE -> {
            val id = gameId()
            if (id > 0L) HomeData.gameStreams(context, id, tab == ChannelSwitcherTab.SAME_GAME_LANGUAGE) else emptyList()
        }
        ChannelSwitcherTab.HISTORY -> HomeData.liveHistory(context)
        ChannelSwitcherTab.TOP_STREAMS -> HomeData.topStreams(context)
        ChannelSwitcherTab.TOP_LANGUAGE -> HomeData.languageStreams(context)
    }
}
