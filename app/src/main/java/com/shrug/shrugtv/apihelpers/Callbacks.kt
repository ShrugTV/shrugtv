package com.shrug.shrugtv.apihelpers

import com.shrug.shrugtv.model.Movie

fun interface FavoriteChangedCallback {
    fun onChanged()
}

fun interface IsFavoritedCallback {
    fun onResult(isFavorited: Boolean)
}

fun interface IsFollowedCallback {
    fun onResult(isFollowed: Boolean)
}

fun interface FollowedChannelsCallback {
    fun onList(list: List<Movie>)
}

fun interface FollowedGamesCallback {
    fun onList(arrayList: ArrayList<Movie>)
}

fun interface LiveStreamHistoryCallback {
    fun onList(arrayList: ArrayList<Movie>)
}

fun interface PastBroadcastCallback {
    fun onList(list: List<Movie>)
}

fun interface SearchCallback {
    fun onList(list: List<Movie>)
}

fun interface TopGamesCallback {
    fun onList(arrayList: ArrayList<Movie>, cursor: String)
}
