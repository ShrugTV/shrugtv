package com.shrug.shrugtv.ui.tv.components

import android.content.Context
import com.shrug.shrugtv.apihelpers.FavoriteChangedCallback
import com.shrug.shrugtv.apihelpers.FavoriteChannels
import com.shrug.shrugtv.apihelpers.FollowedGames
import com.shrug.shrugtv.apihelpers.IsFavoritedCallback
import com.shrug.shrugtv.apihelpers.IsFollowedCallback
import com.shrug.shrugtv.apihelpers.LiveStreamHistory
import com.shrug.shrugtv.apihelpers.Search
import com.shrug.shrugtv.apihelpers.SearchCallback

/** Data-layer entry points for the Compose screens. */
object TvData {

    fun searchLive(c: Context, text: String, cb: SearchCallback) = Search.getLiveStreams(c, text, cb)

    fun searchChannels(c: Context, text: String, cb: SearchCallback) = Search.getChannels(c, text, cb)

    fun searchGames(c: Context, text: String, cb: SearchCallback) = Search.getGames(c, text, cb)

    fun isFavorite(c: Context, channelId: String, cb: IsFavoritedCallback) =
        FavoriteChannels.isFavorited(c, channelId, cb)

    fun setFavorite(c: Context, channelId: String, on: Boolean, cb: FavoriteChangedCallback) {
        if (on) FavoriteChannels.setFavorite(c, channelId, cb)
        else FavoriteChannels.removeFavorite(c, channelId, cb)
    }

    fun gameFollowed(c: Context, gameId: Long, cb: IsFollowedCallback) =
        FollowedGames.isFollowed(c, gameId, cb)

    /** Add the channel to the live history or remove it. */
    fun history(c: Context, channelId: Int, on: Boolean) {
        if (on) LiveStreamHistory.addToHistory(c, channelId)
        else LiveStreamHistory.removeFromHistory(c, channelId)
    }
}
