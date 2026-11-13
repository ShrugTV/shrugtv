package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.database.FavoriteInfo
import com.shrug.shrugtv.session.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Room-backed favorite channel helpers (replaces decompiled FavoriteChannels.java,
 * ).
 */
class FavoriteChannels {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        fun isFavorited(context: Context, channelId: String, callback: IsFavoritedCallback) {
            scope.launch {
                val ids = AppDatabase.getInstance(context).favoriteChannelDao().allChannelIds
                AppState.favoriteChannelIds = ids
                val favorited = ids.contains(channelId)
                main.post { callback.onResult(favorited) }
            }
        }

        fun removeFavorite(context: Context, channelId: String, callback: FavoriteChangedCallback) {
            scope.launch {
                val dao = AppDatabase.getInstance(context).favoriteChannelDao()
                dao.deleteByChannelId(channelId)
                AppState.favoriteChannelIds = dao.allChannelIds
                main.post { callback.onChanged() }
            }
        }

        fun setFavorite(context: Context, channelId: String, callback: FavoriteChangedCallback) {
            scope.launch {
                val dao = AppDatabase.getInstance(context).favoriteChannelDao()
                val info = FavoriteInfo().apply { this.channelId = channelId }
                dao.insert(info)
                AppState.favoriteChannelIds = dao.allChannelIds
                main.post { callback.onChanged() }
            }
        }
    }
}
